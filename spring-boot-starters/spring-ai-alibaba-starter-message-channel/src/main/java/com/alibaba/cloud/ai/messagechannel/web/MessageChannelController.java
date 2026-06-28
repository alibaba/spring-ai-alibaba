/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.messagechannel.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.messagechannel.adapter.ChannelAdapterRegistry;
import com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.dispatcher.MessageChannelDispatcher;
import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single inbound endpoint shared by all IM platforms: each adapter registers itself
 * under a name (e.g. {@code dingtalk}, {@code feishu}) and the path variable selects
 * which one handles the request.
 *
 * <p>Order of operations per request: extract headers → ask the adapter to handle a
 * platform handshake (one-shot URL verification) → otherwise verify signature and
 * parse the body into a {@link ChannelMessage} → run the bound Agent via the
 * dispatcher → ask the adapter to serialize the reply for sync response, or fall
 * back to async push.</p>
 */
@RestController
@RequestMapping
public class MessageChannelController {

	private static final Logger log = LoggerFactory.getLogger(MessageChannelController.class);

	private final ChannelAdapterRegistry adapters;

	private final MessageChannelDispatcher dispatcher;

	public MessageChannelController(ChannelAdapterRegistry adapters, MessageChannelDispatcher dispatcher) {
		this.adapters = adapters;
		this.dispatcher = dispatcher;
	}

	@PostMapping("${spring.ai.alibaba.message-channel.base-path:/channel}/{name}/callback")
	public ResponseEntity<?> callback(@PathVariable("name") String name,
			@RequestBody(required = false) String body,
			HttpServletRequest request) {
		MessageChannelAdapter adapter;
		try {
			adapter = adapters.require(name);
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}

		Map<String, String> headers = collectHeaders(request);
		String safeBody = body == null ? "" : body;

		String challenge = adapter.handleVerification(headers, safeBody);
		if (challenge != null) {
			return ResponseEntity.ok(challenge);
		}

		ChannelMessage message;
		try {
			message = adapter.parseInbound(headers, safeBody);
		}
		catch (SecurityException e) {
			log.warn("Signature verification failed for channel '{}': {}", name, e.getMessage());
			return ResponseEntity.status(401).body(Map.of("error", "signature_invalid"));
		}
		catch (Exception e) {
			log.error("Failed to parse inbound payload for channel '{}'", name, e);
			return ResponseEntity.badRequest().body(Map.of("error", "bad_payload"));
		}
		if (message == null) {
			return ResponseEntity.ok().build();
		}

		ChannelReply reply = dispatcher.dispatch(message);
		Object syncBody = adapter.serializeSyncReply(message, reply);
		if (syncBody != null) {
			return ResponseEntity.ok(syncBody);
		}

		try {
			adapter.push(message.userId(), message.conversationId(), reply);
		}
		catch (Exception e) {
			log.error("Async push failed for channel '{}'", name, e);
		}
		return ResponseEntity.ok(Collections.emptyMap());
	}

	private Map<String, String> collectHeaders(HttpServletRequest request) {
		Map<String, String> headers = new HashMap<>();
		var names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String h = names.nextElement();
			headers.put(h.toLowerCase(), request.getHeader(h));
		}
		return headers;
	}

}
