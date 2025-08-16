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

import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Request model for agent operations.
 *
 * @since 1.0.0.3
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentRequest implements Serializable {

	/** Application identifier */
	@JsonProperty("app_id")
	private String appId;

	/** Unique identifier for the conversation */
	@JsonProperty("conversation_id")
	private String conversationId;

	/** List of chat messages in the conversation */
	@JsonProperty("messages")
	private List<ChatMessage> messages;

	/** Flag indicating if streaming is enabled */
	@JsonProperty("stream")
	private Boolean stream = false;

	/** Map of variables used in prompt templates */
	@JsonProperty("prompt_variables")
	private Map<String, String> promptVariables;

	/** Additional parameters for the request */
	@JsonProperty("extra_params")
	private Map<String, Object> extraPrams;

	/** Flag indicating if this is a draft request */
	@JsonProperty("is_draft")
	private boolean draft = false;

}
