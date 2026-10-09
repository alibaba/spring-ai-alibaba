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

package com.alibaba.cloud.ai.messagechannel.adapter;

import java.util.Map;

import com.alibaba.cloud.ai.messagechannel.model.ChannelMessage;
import com.alibaba.cloud.ai.messagechannel.model.ChannelReply;

/**
 * Per-platform adapter that bridges one IM (DingTalk, WeChat Work, Feishu, ...) to
 * the unified {@link ChannelMessage}/{@link ChannelReply} model.
 *
 * <p>Implementations must be stateless w.r.t. a single request: signature
 * verification, payload parsing, response shaping, and active push are the four
 * responsibilities. Long-lived state (access-token cache, http client) belongs on
 * the implementation instance.</p>
 */
public interface MessageChannelAdapter {

	/**
	 * Logical name used in the inbound URL ({@code /channel/{name}/callback}) and in
	 * configuration ({@code spring.ai.alibaba.message-channel.channels.<name>}).
	 */
	String name();

	/**
	 * Verifies the inbound request authenticity and translates the platform-specific
	 * payload into a {@link ChannelMessage}.
	 *
	 * @param headers HTTP headers received on the webhook (case-insensitive lookups
	 * are the caller's responsibility)
	 * @param rawBody the raw request body, exactly as received — needed because some
	 * platforms (DingTalk, Feishu) sign over the raw bytes
	 * @return the parsed message, or {@code null} if the platform sent a verification
	 * handshake that has already been answered via {@link #handleVerification}
	 * @throws SecurityException if signature verification fails — the controller
	 * surfaces this as HTTP 401
	 */
	ChannelMessage parseInbound(Map<String, String> headers, String rawBody);

	/**
	 * Optional handshake handler for platforms that send a one-shot URL-verification
	 * challenge when the webhook is first registered (Feishu, Slack, WeChat). Return
	 * {@code null} to fall through to {@link #parseInbound}.
	 */
	default String handleVerification(Map<String, String> headers, String rawBody) {
		return null;
	}

	/**
	 * Serializes a reply into the body of the synchronous webhook response, for
	 * platforms that support sync reply (DingTalk single-chat bot, Slack
	 * slash-command). Return {@code null} when the platform requires async push via
	 * {@link #push} instead.
	 */
	default Object serializeSyncReply(ChannelMessage message, ChannelReply reply) {
		return null;
	}

	/**
	 * Pushes a message to a user/conversation out-of-band — the entry point for
	 * Agent-initiated messages (alerts, scheduled digests, follow-ups).
	 *
	 * @param userId target user id, as returned by {@link ChannelMessage#userId} on
	 * a previous inbound message
	 * @param conversationId target conversation id; may be {@code null} for
	 * single-user channels
	 * @param reply the content to deliver
	 * @throws RuntimeException on transport or auth failure
	 */
	void push(String userId, String conversationId, ChannelReply reply);

}
