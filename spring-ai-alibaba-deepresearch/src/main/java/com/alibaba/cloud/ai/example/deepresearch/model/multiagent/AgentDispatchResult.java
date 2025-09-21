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

package com.alibaba.cloud.ai.example.deepresearch.model.multiagent;

import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.SmartAgentUtil;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Agent分派结果
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class AgentDispatchResult {

	private final ChatClient agent;

	private final AgentType agentType;

	private final List<SearchEnum> searchPlatforms;

	private final String searchStrategy;

	private final boolean success;

	private final String errorMessage;

	public AgentDispatchResult(ChatClient agent, AgentType agentType, List<SearchEnum> searchPlatforms,
			String searchStrategy, boolean success, String errorMessage) {
		this.agent = agent;
		this.agentType = agentType;
		this.searchPlatforms = searchPlatforms;
		this.searchStrategy = searchStrategy;
		this.success = success;
		this.errorMessage = errorMessage;
	}

	public ChatClient getAgent() {
		return agent;
	}

	public AgentType getAgentType() {
		return agentType;
	}

	public List<SearchEnum> getSearchPlatforms() {
		return searchPlatforms;
	}

	public String getSearchStrategy() {
		return searchStrategy;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * 获取智能Agent配置的状态更新Map
	 * @return 包含智能Agent配置的状态更新Map
	 */
	public Map<String, Object> getStateUpdate() {
		if (success && agentType != null && searchPlatforms != null) {
			return SmartAgentUtil.createSmartAgentStateUpdate(searchPlatforms, agentType);
		}
		return Map.of(); // 返回空Map而不是null
	}

	@Override
	public String toString() {
		return String.format("AgentDispatchResult{agentType=%s, searchPlatforms=%s, success=%s}", agentType,
				searchPlatforms, success);
	}

}
