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

package com.alibaba.cloud.ai.example.deepresearch.util.Multiagent;

import com.alibaba.cloud.ai.example.deepresearch.config.SmartAgentProperties;
import com.alibaba.cloud.ai.example.deepresearch.dispatcher.SmartAgentDispatcher;
import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.QuestionClassifierService;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.SearchPlatformSelectionService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * Agent集成工具类 统一处理智能Agent的集成逻辑，提供智能Agent选择、错误处理等功能
 *
 * @author Makoto
 * @since 2025/01/28
 */
public class AgentIntegrationUtil {

	private static final Logger logger = LoggerFactory.getLogger(AgentIntegrationUtil.class);

	/**
	 * 智能Agent选择结果
	 */
	public static class AgentSelectionResult {

		private final ChatClient selectedAgent;

		private final AgentType agentType;

		private final boolean isSmartAgent;

		private final String reason;

		public AgentSelectionResult(ChatClient selectedAgent, AgentType agentType, boolean isSmartAgent,
				String reason) {
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

	}

	/**
	 * 智能Agent选择工具类
	 */
	public static class SmartAgentSelectionHelper {

		private final SmartAgentProperties smartAgentProperties;

		private final SmartAgentDispatcher smartAgentDispatcher;

		private final QuestionClassifierService questionClassifierService;

		private final SearchPlatformSelectionService searchPlatformSelectionService;

		public SmartAgentSelectionHelper(SmartAgentProperties smartAgentProperties,
				SmartAgentDispatcher smartAgentDispatcher, QuestionClassifierService questionClassifierService,
				SearchPlatformSelectionService searchPlatformSelectionService) {
			this.smartAgentProperties = smartAgentProperties;
			this.smartAgentDispatcher = smartAgentDispatcher;
			this.questionClassifierService = questionClassifierService;
			this.searchPlatformSelectionService = searchPlatformSelectionService;
		}

		/**
		 * 选择合适的智能Agent
		 * @param questionContent 问题内容
		 * @param state 全局状态
		 * @param fallbackAgent 回退Agent
		 * @return Agent选择结果
		 */
		public AgentSelectionResult selectSmartAgent(String questionContent, OverAllState state,
				ChatClient fallbackAgent) {
			if (!AgentIntegrationUtil.isSmartAgentAvailable(smartAgentProperties, smartAgentDispatcher)) {
				logger.debug("智能Agent功能未开启或服务不可用，使用默认Agent");
				return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false, "智能Agent功能未开启或服务不可用");
			}

			try {
				SmartAgentDispatcher.AgentDispatchResult dispatchResult = smartAgentDispatcher
					.dispatchToAgent(questionContent, state);

				if (dispatchResult.isSuccess() && dispatchResult.getAgent() != null) {
					logger.info("选择智能Agent: {} -> {}", questionContent, dispatchResult.getAgentType());
					return new AgentSelectionResult(dispatchResult.getAgent(), dispatchResult.getAgentType(), true,
							"智能Agent选择成功");
				}
				else {
					logger.warn("智能Agent分派失败: {}", dispatchResult.getErrorMessage());
					return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false,
							"智能Agent分派失败: " + dispatchResult.getErrorMessage());
				}
			}
			catch (Exception e) {
				logger.warn("智能Agent选择失败，回退到默认Agent: {}", e.getMessage());
				return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false,
						"智能Agent选择异常: " + e.getMessage());
			}
		}

		/**
		 * 智能选择搜索引擎
		 * @param state 全局状态
		 * @param query 查询内容
		 * @return 搜索引擎枚举
		 */
		public SearchEnum intelligentSearchEngineSelection(OverAllState state, String query) {
			if (!AgentIntegrationUtil.isSmartAgentAvailable(smartAgentProperties, questionClassifierService,
					searchPlatformSelectionService)) {
				return state.value("search_engine", SearchEnum.class).orElse(SearchEnum.TAVILY);
			}

			AgentType agentType = questionClassifierService.classifyQuestion(query);
			logger.info("问题分类结果: {} -> {}", query, agentType);

			List<SearchEnum> platforms = searchPlatformSelectionService.selectSearchPlatforms(agentType, query);
			if (platforms != null && !platforms.isEmpty()) {
				SearchEnum primaryPlatform = platforms.get(0);
				logger.info("智能选择搜索平台: {} (Agent类型: {})", primaryPlatform, agentType);
				return primaryPlatform;
			}

			// 如果没有选择成功，则回退到原有搜索引擎
			return state.value("search_engine", SearchEnum.class).orElse(SearchEnum.TAVILY);
		}

	}

	/**
	 * 错误处理和日志记录工具类
	 */
	public static class ErrorHandlingHelper {

		private final Logger logger;

		public ErrorHandlingHelper(Logger logger) {
			this.logger = logger;
		}


		public void logSmartAgentSuccess(String operation, String question, AgentType agentType) {
			logger.info("{} 成功 - 问题: '{}', Agent类型: {}", operation, question, agentType);
		}


		public void logSmartAgentFailureAndFallback(String operation, String question, String error,
				String fallbackAction) {
			logger.warn("{} 失败 - 问题: '{}', 错误: {}, 回退: {}", operation, question, error, fallbackAction);
		}


		public void logSmartAgentConfigurationStatus(boolean enabled, int availableServices) {
			if (enabled) {
				logger.info("智能Agent功能已启用，可用服务数: {}", availableServices);
			}
			else {
				logger.debug("智能Agent功能未启用，使用默认配置");
			}
		}

	}

	/**
	 * 创建智能Agent选择辅助器
	 */
	public static SmartAgentSelectionHelper createSelectionHelper(SmartAgentProperties smartAgentProperties,
			SmartAgentDispatcher smartAgentDispatcher, QuestionClassifierService questionClassifierService,
			SearchPlatformSelectionService searchPlatformSelectionService) {
		return new SmartAgentSelectionHelper(smartAgentProperties, smartAgentDispatcher, questionClassifierService,
				searchPlatformSelectionService);
	}

	/**
	 * 创建错误处理辅助器
	 */
	public static ErrorHandlingHelper createErrorHelper(Logger logger) {
		return new ErrorHandlingHelper(logger);
	}

	public static boolean isSmartAgentAvailable(SmartAgentProperties smartAgentProperties, Object... services) {
		if (smartAgentProperties == null || !smartAgentProperties.isEnabled()) {
			logger.debug("智能Agent功能未开启");
			return false;
		}

		for (Object service : services) {
			if (service == null) {
				logger.warn("智能Agent必要服务不可用，回退到原有逻辑");
				return false;
			}
		}

		return true;
	}

}