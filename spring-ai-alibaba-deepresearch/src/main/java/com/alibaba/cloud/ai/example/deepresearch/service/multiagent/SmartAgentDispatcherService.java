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
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
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
 * Intelligent Agent Dispatcher: Assigns tasks to corresponding agents based on question classification results.
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Component
@ConditionalOnProperty(prefix = SmartAgentProperties.PREFIX, name = "enabled", havingValue = "true",
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
	 * Intelligently dispatches tasks to appropriate agents, implementing functions such as question classification,
	 * search platform selection, and agent selection. Retrieves search strategy descriptions and other information,
	 * and updates search configurations in the state.
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

			return new AgentDispatchResult(selectedAgent, agentType, searchPlatforms, searchStrategy, true, null);

		}
		catch (Exception e) {
			logger.error("Error dispatching question to agent: {}", question, e);
			return new AgentDispatchResult(researchAgent, AgentType.GENERAL_RESEARCH, List.of(SearchEnum.TAVILY),
					"使用通用搜索引擎进行综合性研究", false, e.getMessage());
		}
	}

	/**
	 * Selects the corresponding ChatClient based on the agent type
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
