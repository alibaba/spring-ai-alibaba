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
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.SearchPlatform;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.AgentPromptTemplateUtil;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Search Platform Selection Service
 * Intelligently selects the most appropriate search platform based on agent type and question content
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Service
@ConditionalOnProperty(prefix = SmartAgentProperties.PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class SearchPlatformSelectionService {

	private static final Logger logger = LoggerFactory.getLogger(SearchPlatformSelectionService.class);

	@Autowired
	private SmartAgentProperties smartAgentProperties;

	@Value("${spring.ai.alibaba.deepresearch.search-list:tavily,aliyun,baidu,serpapi}")
	private List<String> enabledSearchEngines;

	private final ChatClient searchPlatformSelectorClient;

	public SearchPlatformSelectionService(DashScopeChatModel chatModel) {
		this.searchPlatformSelectorClient = ChatClient.builder(chatModel)
			.defaultSystem(AgentPromptTemplateUtil.getSearchPlatformSelectionPrompt())
			.build();
	}

	/**
	 * Selecting the primary search platform based on agent type (unified platform selection logic)
	 */
	private SearchPlatform selectPlatformInternal(AgentType agentType, String question) {
		SearchPlatform primaryPlatform = getPrimaryPlatformFromConfig(agentType);

		// If not configured, use AI intelligent selection
		if (primaryPlatform == null) {
			primaryPlatform = selectPrimaryPlatformByAI(agentType, question);
		}

		return primaryPlatform;
	}

	/**
	 * Select the primary search platform based on the agent type
	 */
	public List<SearchEnum> selectSearchPlatforms(AgentType agentType, String question) {
		List<SearchEnum> searchPlatforms = new ArrayList<>();

		try {
			SearchPlatform primaryPlatform = selectPlatformInternal(agentType, question);

			if (SmartAgentUtil.isToolCallingPlatform(primaryPlatform)) {
				// For the tool invocation platform, return a special identifier to inform the caller that tool invocation is required.
				// Use TAVILY as a placeholder, which will be overridden by the actual tool invocation.
				searchPlatforms.add(SearchEnum.TAVILY);
				logger.info("Selected tool calling platform for agent type {}: {}", agentType,
						primaryPlatform.getName());
			}
			else {
				if (SmartAgentUtil.isValidAndEnabledPlatform(primaryPlatform, enabledSearchEngines)) {
					SearchEnum primarySearchEnum = SmartAgentUtil.convertToSearchEnum(primaryPlatform);
					searchPlatforms.add(primarySearchEnum);
				}
			}

			// If the primary search platform is unavailable, use the default general search
			if (searchPlatforms.isEmpty()) {
				addDefaultSearchEngines(searchPlatforms);
			}

			logger.info("Selected search platform for agent type {}: {}", agentType, searchPlatforms);
			return searchPlatforms;

		}
		catch (Exception e) {
			logger.warn("Error selecting search platform for agent type {}, using default", agentType, e);
			List<SearchEnum> defaultPlatforms = new ArrayList<>();
			addDefaultSearchEngines(defaultPlatforms);
			return defaultPlatforms;
		}
	}

	/**
	 * Retrieve the selected search platform (for tool invocation)
	 */
	public SearchPlatform getSelectedSearchPlatform(AgentType agentType, String question) {
		if (agentType == null) {
			return null;
		}
		return selectPlatformInternal(agentType, question);
	}

	/**
	 * Obtain the primary search platform from the configuration
	 */
	private SearchPlatform getPrimaryPlatformFromConfig(AgentType agentType) {
		if (smartAgentProperties.getSearchPlatformMapping() == null) {
			return null;
		}

		SmartAgentProperties.SearchPlatformConfig config = smartAgentProperties.getSearchPlatformMapping()
			.get(agentType.name().toLowerCase());

		if (config == null || config.getPrimary() == null) {
			return null;
		}

		try {
			return SearchPlatform.valueOf(config.getPrimary().toUpperCase());
		}
		catch (IllegalArgumentException e) {
			logger.warn("Invalid search platform configuration: {}", config.getPrimary());
			return null;
		}
	}

	/**
	 * Uses AI to intelligently select a search platform
	 * @param agentType The type of agent
	 * @param question The content of the question
	 * @return The selected search platform; returns the default platform if AI selection fails
	 */
	private SearchPlatform selectPrimaryPlatformByAI(AgentType agentType, String question) {
		try {
			String aiSelection = searchPlatformSelectorClient.prompt()
				.user(String.format("Agent类型: %s\n问题内容: %s\n\n请根据以上信息选择最合适的搜索平台：", agentType.name(), question))
				.call()
				.content();

			SearchPlatform selectedPlatform = SmartAgentUtil.parseAiSearchPlatformSelection(aiSelection);

			if (selectedPlatform != null) {
				logger.info("AI选平台: {}", selectedPlatform.getName());
				return selectedPlatform;
			}
			logger.warn("AI解析失败，默认平台");
			return getDefaultPlatformForAgentType(agentType);
		}
		catch (Exception e) {
			logger.warn("AI异常，默认平台");
			return getDefaultPlatformForAgentType(agentType);
		}
	}

	/**
	 * Retrieves the default search platform based on the agent type
	 * @param agentType The type of agent
	 * @return The default search platform
	 */
	private SearchPlatform getDefaultPlatformForAgentType(AgentType agentType) {
		return switch (agentType) {
			case ACADEMIC_RESEARCH -> SearchPlatform.GOOGLE_SCHOLAR;
			case LIFESTYLE_TRAVEL -> SearchPlatform.OPENTRIPMAP;
			case ENCYCLOPEDIA -> SearchPlatform.WIKIPEDIA;
			case DATA_ANALYSIS -> SearchPlatform.WORLDBANK_DATA;
			default -> SearchPlatform.TAVILY;
		};
	}

	/**
	 * Adds the default search engine
	 */
	private void addDefaultSearchEngines(List<SearchEnum> searchPlatforms) {
		List<SearchEnum> defaultEngines = Arrays.asList(SearchEnum.TAVILY, SearchEnum.ALIYUN, SearchEnum.BAIDU,
				SearchEnum.SERPAPI);

		for (SearchEnum engine : defaultEngines) {
			if (SmartAgentUtil.isSearchEngineEnabled(engine, enabledSearchEngines)) {
				searchPlatforms.add(engine);
				break;
			}
		}

		if (searchPlatforms.isEmpty()) {
			searchPlatforms.add(SearchEnum.TAVILY);
		}
	}

	/**
	 * Retrieves the search strategy description
	 */
	public String getSearchStrategyDescription(AgentType agentType) {
		return SmartAgentUtil.getSearchStrategyDescription(agentType);
	}

}
