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

package com.alibaba.cloud.ai.messagechannel.model;

import java.util.Collections;
import java.util.Map;

/**
 * Unified inbound message produced by a {@code MessageChannelAdapter} after parsing
 * the platform-specific webhook payload.
 *
 * <p>Each IM platform (DingTalk, WeChat Work, Feishu, Telegram, Slack, ...) has a
 * different request body shape; the adapter is responsible for translating it into
 * this neutral structure before the dispatcher routes it to the bound Agent.</p>
 *
 * @param channelName logical name of the channel that produced this message,
 * e.g. {@code "dingtalk"}; matches the {@code channels.<name>} key in configuration
 * @param userId stable identifier of the sender within the channel (used to address
 * push-back replies)
 * @param conversationId stable identifier of the conversation/session, used as
 * {@code thread_id} for checkpointed Agents so multi-turn state survives across
 * webhook calls
 * @param text plain-text content of the message; never {@code null}, may be empty
 * @param replyToken short-lived token some platforms hand out for synchronous reply
 * (e.g. Feishu); may be {@code null}
 * @param attributes channel-specific raw fields the Adapter wants to surface to the
 * Agent (sender display name, group id, mention list, ...); never {@code null}
 */
public record ChannelMessage(String channelName,
							 String userId,
							 String conversationId,
							 String text,
							 String replyToken,
							 Map<String, Object> attributes) {

	public ChannelMessage {
		if (channelName == null || channelName.isBlank()) {
			throw new IllegalArgumentException("channelName must not be blank");
		}
		if (text == null) {
			text = "";
		}
		if (attributes == null) {
			attributes = Collections.emptyMap();
		}
	}

}
