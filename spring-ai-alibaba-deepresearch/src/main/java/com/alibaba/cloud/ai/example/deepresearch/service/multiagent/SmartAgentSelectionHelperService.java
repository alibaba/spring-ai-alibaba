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

package com.alibaba.cloud.ai.example.deepresearch.service.multiagent;

import com.alibaba.cloud.ai.example.deepresearch.config.SmartAgentProperties;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentDispatchResult;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentSelectionResult;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.SearchPlatform;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.AgentIntegrationUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * Intelligent Agent Selection Assistant
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class SmartAgentSelectionHelperService {

	private static final Logger logger = LoggerFactory.getLogger(SmartAgentSelectionHelperService.class);

	private final SmartAgentProperties smartAgentProperties;

	private final SmartAgentDispatcherService smartAgentDispatcher;

	private final QuestionClassifierService questionClassifierService;

	private final SearchPlatformSelectionService searchPlatformSelectionService;

	public SmartAgentSelectionHelperService(SmartAgentProperties smartAgentProperties,
			SmartAgentDispatcherService smartAgentDispatcher, QuestionClassifierService questionClassifierService,
			SearchPlatformSelectionService searchPlatformSelectionService) {
		this.smartAgentProperties = smartAgentProperties;
		this.smartAgentDispatcher = smartAgentDispatcher;
		this.questionClassifierService = questionClassifierService;
		this.searchPlatformSelectionService = searchPlatformSelectionService;
	}

	/**
	 * Selects the appropriate intelligent agent
	 * @param questionContent The content of the question
	 * @param state The global state
	 * @param fallbackAgent The fallback agent
	 * @return Agent selection result
	 */
	public AgentSelectionResult selectSmartAgent(String questionContent, OverAllState state, ChatClient fallbackAgent) {
		if (!AgentIntegrationUtil.isSmartAgentAvailable(smartAgentProperties, smartAgentDispatcher)) {
			logger.debug("智能Agent功能未开启或服务不可用，使用默认Agent");
			return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false, "智能Agent功能未开启或服务不可用");
		}

		try {
			AgentDispatchResult dispatchResult = smartAgentDispatcher.dispatchToAgent(questionContent, state);

			if (dispatchResult.isSuccess() && dispatchResult.getAgent() != null) {
				return new AgentSelectionResult(dispatchResult.getAgent(), dispatchResult.getAgentType(), true,
						"智能Agent选择成功", dispatchResult.getStateUpdate());
			}
			else {
				return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false,
						"智能Agent分派失败: " + dispatchResult.getErrorMessage());
			}
		}
		catch (Exception e) {
			return new AgentSelectionResult(fallbackAgent, AgentType.GENERAL_RESEARCH, false,
					"智能Agent选择异常: " + e.getMessage());
		}
	}

	/**
	 * Core logic for intelligent search selection (unified question classification and platform selection)
	 */
	private AgentType classifyQueryAndLog(String query) {
		AgentType agentType = questionClassifierService.classifyQuestion(query);
		logger.info("问题分类结果: {} -> {}", query, agentType);
		return agentType;
	}

	/**
	 * Unified intelligent search selection method
	 * @param state Global state
	 * @param query Query content
	 * @return Search selection result
	 */
	public SmartAgentUtil.SearchSelectionResult intelligentSearchSelection(OverAllState state, String query) {
		if (!AgentIntegrationUtil.isSmartAgentAvailable(smartAgentProperties, questionClassifierService,
				searchPlatformSelectionService)) {
			SearchEnum fallbackEnum = state.value("search_engine", SearchEnum.class).orElse(SearchEnum.TAVILY);
			return new SmartAgentUtil.SearchSelectionResult(fallbackEnum, null, AgentType.GENERAL_RESEARCH, false);
		}

		try {
			AgentType agentType = classifyQueryAndLog(query);
			SearchPlatform selectedPlatform = searchPlatformSelectionService.getSelectedSearchPlatform(agentType,
					query);

			if (SmartAgentUtil.isToolCallingPlatform(selectedPlatform)) {
				logger.info("选择工具调用搜索: {} (Agent类型: {})", selectedPlatform.getName(), agentType);
				return new SmartAgentUtil.SearchSelectionResult(SearchEnum.TAVILY, selectedPlatform, agentType, true);
			}
			else {
				List<SearchEnum> platforms = searchPlatformSelectionService.selectSearchPlatforms(agentType, query);
				SearchEnum searchEnum = platforms != null && !platforms.isEmpty() ? platforms.get(0)
						: state.value("search_engine", SearchEnum.class).orElse(SearchEnum.TAVILY);
				logger.info("选择传统搜索: {} (Agent类型: {})", searchEnum, agentType);
				return new SmartAgentUtil.SearchSelectionResult(searchEnum, selectedPlatform, agentType, false);
			}
		}
		catch (Exception e) {
			logger.warn("选择失败: {}", e.getMessage());
			SearchEnum fallbackEnum = state.value("search_engine", SearchEnum.class).orElse(SearchEnum.TAVILY);
			return new SmartAgentUtil.SearchSelectionResult(fallbackEnum, null, AgentType.GENERAL_RESEARCH, false);
		}
	}

}
