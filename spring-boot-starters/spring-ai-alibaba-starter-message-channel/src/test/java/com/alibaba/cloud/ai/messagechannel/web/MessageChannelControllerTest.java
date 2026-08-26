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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.messagechannel.adapter.ChannelAdapterRegistry;
import com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.dispatcher.AgentBindingRegistry;
import com.alibaba.cloud.ai.messagechannel.dispatcher.MessageChannelDispatcher;
import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageChannelControllerTest {

	@Test
	void unknownChannel_returns404() throws Exception {
		MockMvc mvc = mvcWith(List.of(new StubAdapter("real", null, null)),
				message -> ChannelReply.text("ignored"));

		mvc.perform(post("/channel/missing/callback").contentType("application/json").content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void verificationHandshake_returnsChallenge() throws Exception {
		StubAdapter adapter = new StubAdapter("verify",
				(headers, body) -> headers.get("x-challenge"),
				null);
		MockMvc mvc = mvcWith(List.of(adapter), message -> {
			throw new AssertionError("dispatcher should not be called during handshake");
		});

		mvc.perform(post("/channel/verify/callback")
				.contentType("application/json").header("x-challenge", "ping").content("{}"))
				.andExpect(status().isOk())
				.andExpect(content -> assertThat(content.getResponse().getContentAsString()).contains("ping"));
	}

	@Test
	void signatureFailure_returns401() throws Exception {
		StubAdapter adapter = new StubAdapter("ding", null,
				(headers, body) -> {
					throw new SecurityException("Signature mismatch");
				});
		MockMvc mvc = mvcWith(List.of(adapter), message -> ChannelReply.text("never"));

		mvc.perform(post("/channel/ding/callback").contentType("application/json").content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("signature_invalid"));
	}

	@Test
	void validRequest_returnsSyncEnvelope() throws Exception {
		AtomicReference<ChannelMessage> dispatched = new AtomicReference<>();
		StubAdapter adapter = new StubAdapter("ding", null,
				(headers, body) -> new ChannelMessage("ding", "u1", "c1", "hello", null, Map.of()));
		adapter.syncEnvelope = (msg, reply) -> Map.of("msgtype", "text",
				"text", Map.of("content", reply.content()));

		MockMvc mvc = mvcWith(List.of(adapter), message -> {
			dispatched.set(message);
			return ChannelReply.text("hi back");
		});

		mvc.perform(post("/channel/ding/callback").contentType("application/json").content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.msgtype").value("text"))
				.andExpect(jsonPath("$.text.content").value("hi back"));

		assertThat(dispatched.get()).isNotNull();
		assertThat(dispatched.get().userId()).isEqualTo("u1");
	}

	@Test
	void noSyncEnvelope_fallsBackToAsyncPush() throws Exception {
		AtomicReference<ChannelReply> pushed = new AtomicReference<>();
		StubAdapter adapter = new StubAdapter("async", null,
				(headers, body) -> new ChannelMessage("async", "u9", "c9", "go", null, Map.of()));
		adapter.onPush = (userId, conv, reply) -> pushed.set(reply);

		MockMvc mvc = mvcWith(List.of(adapter), message -> ChannelReply.text("pushed-content"));

		mvc.perform(post("/channel/async/callback").contentType("application/json").content("{}"))
				.andExpect(status().isOk());

		assertThat(pushed.get()).isNotNull();
		assertThat(pushed.get().content()).isEqualTo("pushed-content");
	}

	private static MockMvc mvcWith(List<MessageChannelAdapter> adapters,
			java.util.function.Function<ChannelMessage, ChannelReply> dispatchFn) {
		ChannelAdapterRegistry registry = new ChannelAdapterRegistry(adapters);
		MessageChannelDispatcher dispatcher = new MessageChannelDispatcher(
				new AgentBindingRegistry(new DefaultListableBeanFactory(), Map.of())) {
			@Override
			public ChannelReply dispatch(ChannelMessage message) {
				return dispatchFn.apply(message);
			}
		};
		return MockMvcBuilders.standaloneSetup(new MessageChannelController(registry, dispatcher)).build();
	}

	@FunctionalInterface
	private interface ParseFn {

		ChannelMessage parse(Map<String, String> headers, String body);

	}

	@FunctionalInterface
	private interface VerifyFn {

		String handle(Map<String, String> headers, String body);

	}

	@FunctionalInterface
	private interface PushFn {

		void push(String userId, String conversationId, ChannelReply reply);

	}

	@FunctionalInterface
	private interface SyncEnvelopeFn {

		Object build(ChannelMessage message, ChannelReply reply);

	}

	private static final class StubAdapter implements MessageChannelAdapter {

		private final String name;

		private final VerifyFn verifyFn;

		private final ParseFn parseFn;

		SyncEnvelopeFn syncEnvelope;

		PushFn onPush;

		StubAdapter(String name, VerifyFn verifyFn, ParseFn parseFn) {
			this.name = name;
			this.verifyFn = verifyFn;
			this.parseFn = parseFn;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String handleVerification(Map<String, String> headers, String rawBody) {
			return verifyFn == null ? null : verifyFn.handle(headers, rawBody);
		}

		@Override
		public ChannelMessage parseInbound(Map<String, String> headers, String rawBody) {
			return parseFn == null ? null : parseFn.parse(headers, rawBody);
		}

		@Override
		public Object serializeSyncReply(ChannelMessage message, ChannelReply reply) {
			return syncEnvelope == null ? null : syncEnvelope.build(message, reply);
		}

		@Override
		public void push(String userId, String conversationId, ChannelReply reply) {
			if (onPush != null) {
				onPush.push(userId, conversationId, reply);
			}
		}

	}

}
