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

import com.alibaba.cloud.ai.agent.studio.dto.messages.ToolRequestConfirmMessageDTO;
import com.alibaba.cloud.ai.agent.studio.dto.messages.UserMessageDTO;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object (DTO) for POST /run and POST /run-sse requests. Contains information needed
 * to execute an agent run.
 */
public class AgentResumeRequest {
	@JsonProperty("appName")
	public String appName;

	@JsonProperty("userId")
	public String userId;

	@JsonProperty("threadId")
	public String threadId;

	@JsonProperty("newMessage")
	public UserMessageDTO newMessage;

	@JsonProperty("streaming")
	public boolean streaming = false;

	@JsonProperty("toolFeedbacks")
	public List<ToolRequestConfirmMessageDTO.ToolFeedback> toolFeedbacks;

	/**
	 * Optional state delta to merge into the session state before running the agent. This allows
	 * updating session state dynamically per request, useful for injecting configuration (e.g.,
	 * replay mode settings) without modifying the stored session.
	 */
	@JsonProperty("stateDelta")
	public Map<String, Object> stateDelta;

	public AgentResumeRequest() {
	}

	public String getAppName() {
		return appName;
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

	public List<ToolRequestConfirmMessageDTO.ToolFeedback> getToolFeedbacks() {
		return toolFeedbacks;
	}

	public Map<String, Object> getStateDelta() {
		return stateDelta;
	}
}
