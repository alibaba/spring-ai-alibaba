/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.studio.runtime.domain.chat;

import com.alibaba.cloud.ai.studio.runtime.domain.audio.AudioOutput;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a message in a chat conversation.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

	public ChatMessage(MessageRole role, Object content) {
		this.role = role;
		this.content = content;
	}

	/** The role of the message sender */
	@JsonProperty("role")
	private MessageRole role;

	/** The type of content in the message */
	@JsonProperty("content_type")
	@Builder.Default
	private ContentType contentType = ContentType.TEXT;

	/** The actual content of the message */
	@JsonProperty("content")
	@JsonDeserialize(using = ChatMessageContentDeserializer.class)
	private Object content;

	/** The name of the message sender */
	@JsonProperty("name")
	private String name;

	/** List of tool calls associated with the message */
	@JsonProperty("tool_calls")
	private List<ToolCall> toolCalls;

	/** Audio output associated with the message */
	@JsonProperty("audio")
	private AudioOutput audioOutput;

	/** Reasoning content for the message */
	@JsonProperty("reasoning_content")
	private String reasoningContent;

}
