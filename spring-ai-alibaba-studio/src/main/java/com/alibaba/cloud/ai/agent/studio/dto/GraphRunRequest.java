/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.agent.studio.dto;

import com.alibaba.cloud.ai.agent.studio.dto.messages.UserMessageDTO;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for POST /graph_run_sse requests. Contains information needed to execute
 * a graph run.
 */
public class GraphRunRequest {

	@JsonProperty("graphName")
	public String graphName;

	@JsonProperty("userId")
	public String userId;

	@JsonProperty("threadId")
	public String threadId;

	@JsonProperty("newMessage")
	public UserMessageDTO newMessage;

	@JsonProperty("streaming")
	public boolean streaming = false;

	/**
	 * Optional custom inputs for the graph. If provided, overrides the default messages-based
	 * input built from newMessage.
	 */
	@JsonProperty("inputs")
	public Map<String, Object> inputs;

	public GraphRunRequest() {
	}

	public String getGraphName() {
		return graphName;
	}

	public String getUserId() {
		return userId;
	}

	public String getThreadId() {
		return threadId;
	}

	public UserMessageDTO getNewMessage() {
		return newMessage;
	}

	public boolean getStreaming() {
		return streaming;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}
}
