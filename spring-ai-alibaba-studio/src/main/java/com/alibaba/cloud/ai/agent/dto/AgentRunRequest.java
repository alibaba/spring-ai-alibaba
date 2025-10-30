/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.agent.dto;

import org.springframework.ai.chat.messages.UserMessage;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object (DTO) for POST /run and POST /run-sse requests. Contains information needed
 * to execute an agent run.
 */
public class AgentRunRequest {
	@JsonProperty("appName")
	public String appName;

	@JsonProperty("userId")
	public String userId;

	@JsonProperty("sessionId")
	public String sessionId;

	@JsonProperty("newMessage")
	public UserMessage newMessage;

	@JsonProperty("streaming")
	public boolean streaming = false;

	/**
	 * Optional state delta to merge into the session state before running the agent. This allows
	 * updating session state dynamically per request, useful for injecting configuration (e.g.,
	 * replay mode settings) without modifying the stored session.
	 */
	@JsonProperty("stateDelta")
	public Map<String, Object> stateDelta;

	public AgentRunRequest() {
	}

	public String getAppName() {
		return appName;
	}

	public String getUserId() {
		return userId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public UserMessage getNewMessage() {
		return newMessage;
	}

	public boolean getStreaming() {
		return streaming;
	}

	public Map<String, Object> getStateDelta() {
		return stateDelta;
	}
}
