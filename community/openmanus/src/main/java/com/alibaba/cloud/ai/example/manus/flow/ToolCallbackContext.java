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
package com.alibaba.cloud.ai.example.manus.flow;

import org.springframework.ai.chat.messages.Message;
import java.util.List;

/**
 * Tool回调上下文，用于存储工具调用相关的上下文信息
 */
public class ToolCallbackContext {

	private String planId;

	private String toolName;

	private List<Message> messages;

	private String lastResult;

	public ToolCallbackContext(String planId, String toolName) {
		this.planId = planId;
		this.toolName = toolName;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public String getLastResult() {
		return lastResult;
	}

	public void setLastResult(String lastResult) {
		this.lastResult = lastResult;
	}

}
