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

package com.alibaba.cloud.ai.example.deepresearch.service.mutiagent;

import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentDispatchResult;
import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.util.Multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能Agent调度器，根据问题分类结果将任务分配给对应的Agent
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Component
@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
		matchIfMissing = false)
public class SmartAgentDispatcherService {

	private static final Logger logger = LoggerFactory.getLogger(SmartAgentDispatcherService.class);

	@Autowired
	private QuestionClassifierService questionClassifierService;

	@Autowired
	private SearchPlatformSelectionService searchPlatformSelectionService;

	@Autowired
	@Qualifier("researchAgent")
	private ChatClient researchAgent;

	@Autowired
	@Qualifier("academicResearchAgent")
	private ChatClient academicResearchAgent;

	@Autowired
	@Qualifier("lifestyleTravelAgent")
	private ChatClient lifestyleTravelAgent;

	@Autowired
	@Qualifier("encyclopediaAgent")
	private ChatClient encyclopediaAgent;

	@Autowired
	@Qualifier("dataAnalysisAgent")
	private ChatClient dataAnalysisAgent;

	/**
	 * 智能分派任务到合适的Agent,实现问题分类，搜索平台选择和Agent选择等功能，获取搜索策略描述等信息，更新状态中的搜索配置的功能
	 */
	public AgentDispatchResult dispatchToAgent(String question, OverAllState state) {
		try {
			AgentType agentType = questionClassifierService.classifyQuestion(question);

			logger.info("Question classified as: {} for question: {}", agentType, question);

			List<SearchEnum> searchPlatforms = searchPlatformSelectionService.selectSearchPlatforms(agentType,
					question);
			logger.info("Selected search platforms: {} for agent type: {}", searchPlatforms, agentType);

			ChatClient selectedAgent = selectAgent(agentType);

			String searchStrategy = searchPlatformSelectionService.getSearchStrategyDescription(agentType);

			SmartAgentUtil.updateStateWithSmartAgentConfig(state, searchPlatforms, agentType);

			return new AgentDispatchResult(selectedAgent, agentType, searchPlatforms, searchStrategy, true, null);

		}
		catch (Exception e) {
			logger.error("Error dispatching question to agent: {}", question, e);
			return new AgentDispatchResult(researchAgent, AgentType.GENERAL_RESEARCH, List.of(SearchEnum.TAVILY),
					"使用通用搜索引擎进行综合性研究", false, e.getMessage());
		}
	}

	/**
	 * 根据Agent类型选择对应的ChatClient
	 */
	private ChatClient selectAgent(AgentType agentType) {
		return switch (agentType) {
			case ACADEMIC_RESEARCH -> academicResearchAgent;
			case LIFESTYLE_TRAVEL -> lifestyleTravelAgent;
			case ENCYCLOPEDIA -> encyclopediaAgent;
			case DATA_ANALYSIS -> dataAnalysisAgent;
			case GENERAL_RESEARCH -> researchAgent;
		};
	}

}
