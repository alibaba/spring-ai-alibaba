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

package com.alibaba.cloud.ai.example.deepresearch.model.mutiagent;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 智能Agent选择结果
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class AgentSelectionResult {

	private final ChatClient selectedAgent;

	private final AgentType agentType;

	private final boolean isSmartAgent;

	private final String reason;

	public AgentSelectionResult(ChatClient selectedAgent, AgentType agentType, boolean isSmartAgent, String reason) {
		this.selectedAgent = selectedAgent;
		this.agentType = agentType;
		this.isSmartAgent = isSmartAgent;
		this.reason = reason;
	}

	public ChatClient getSelectedAgent() {
		return selectedAgent;
	}

	public AgentType getAgentType() {
		return agentType;
	}

	public boolean isSmartAgent() {
		return isSmartAgent;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return String.format("AgentSelectionResult{agentType=%s, isSmartAgent=%s, reason='%s'}", agentType,
				isSmartAgent, reason);
	}

}
