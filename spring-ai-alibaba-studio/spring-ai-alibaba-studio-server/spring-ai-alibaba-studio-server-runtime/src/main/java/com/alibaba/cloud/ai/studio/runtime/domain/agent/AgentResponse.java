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

package com.alibaba.cloud.ai.studio.runtime.domain.agent;

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response model for agent completion requests.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse implements Serializable {

	/** Unique identifier for the request */
	@JsonProperty("request_id")
	private String requestId;

	/** Unique identifier for the conversation */
	@JsonProperty("conversation_id")
	private String conversationId;

	/** Current status of the agent */
	private AgentStatus status;

	/** Index of the response in the conversation */
	private String index;

	/** The chat message content */
	private ChatMessage message;

	/** Timestamp when the response was created */
	private Long created;

	/** Model identifier used for generating the response */
	private String model;

	/** Usage statistics for the request */
	private Usage usage;

	/** Error information if the request failed */
	private Error error;

	/** Checks if the response was successful */
	@JsonIgnore
	public boolean isSuccess() {
		return error == null;
	}

}
