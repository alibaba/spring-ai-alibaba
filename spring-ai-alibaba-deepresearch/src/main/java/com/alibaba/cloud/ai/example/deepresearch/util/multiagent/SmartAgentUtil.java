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

package com.alibaba.cloud.ai.example.deepresearch.util.multiagent;

import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.SearchPlatform;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intelligent Agent System Common Utility Class
 * Integrates common logic such as switch checks, type conversion, and state management
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class SmartAgentUtil {

	private static final Logger logger = LoggerFactory.getLogger(SmartAgentUtil.class);

	/**
	 * Parses AI classification results
	 * @param aiResponse AI-returned classification result
	 * @return Parsed agent type
	 */
	public static AgentType parseAiClassification(String aiResponse) {
		if (aiResponse == null) {
			return AgentType.GENERAL_RESEARCH;
		}

		String response = aiResponse.toLowerCase().trim();

		if (response.contains("academic_research") || response.contains("学术研究")) {
			return AgentType.ACADEMIC_RESEARCH;
		}
		else if (response.contains("lifestyle_travel") || response.contains("生活") || response.contains("旅游")) {
			return AgentType.LIFESTYLE_TRAVEL;
		}
		else if (response.contains("encyclopedia") || response.contains("百科")) {
			return AgentType.ENCYCLOPEDIA;
		}
		else if (response.contains("data_analysis") || response.contains("数据分析")) {
			return AgentType.DATA_ANALYSIS;
		}

		return AgentType.GENERAL_RESEARCH;
	}

	/**
	 * Parses AI search platform selection results
	 * @param aiResponse AI-returned search platform selection result
	 * @return Parsed search platform type, returns null if parsing fails
	 */
	public static SearchPlatform parseAiSearchPlatformSelection(String aiResponse) {
		if (aiResponse == null || aiResponse.trim().isEmpty()) {
			return null;
		}

		String response = aiResponse.toUpperCase().trim();

		// Remove possible code block markers
		response = response.replace("```", "").trim();

		try {
			// Attempt direct platform name matching
			for (SearchPlatform platform : SearchPlatform.values()) {
				if (response.contains(platform.name())) {
					return platform;
				}
			}

			if (response.contains("OPENALEX")) {
				return SearchPlatform.OPENALEX;
			}
			else if (response.contains("GOOGLE_SCHOLAR")) {
				return SearchPlatform.GOOGLE_SCHOLAR;
			}
			else if (response.contains("WIKIPEDIA")) {
				return SearchPlatform.WIKIPEDIA;
			}
			else if (response.contains("OPENTRIPMAP")) {
				return SearchPlatform.OPENTRIPMAP;
			}
			else if (response.contains("WORLD_BANK")) {
				return SearchPlatform.WORLDBANK_DATA;
			}
			else if (response.contains("TAVILY")) {
				return SearchPlatform.TAVILY;
			}
		}
		catch (Exception e) {
			logger.warn("Failed to parse AI search platform selection: {}", aiResponse, e);
		}
		return null; // Return null when parsing fails, allowing the caller to use default values
	}

	/**
	 * Converts SearchPlatform to SearchEnum
	 * @param platform Search platform enumeration
	 * @return Corresponding SearchEnum
	 */
	public static SearchEnum convertToSearchEnum(SearchPlatform platform) {
		return switch (platform) {
			case TAVILY -> SearchEnum.TAVILY;
			case ALIYUN_AI_SEARCH -> SearchEnum.ALIYUN;
			case BAIDU_SEARCH -> SearchEnum.BAIDU;
			case SERPAPI -> SearchEnum.SERPAPI;

			// Special tool invocation mapping - returns null to indicate tool invocation search is required
			case OPENALEX, OPENTRIPMAP, TRIPADVISOR, WIKIPEDIA, WORLDBANK_DATA, GOOGLE_SCHOLAR -> null;
		};
	}

	/**
	 * Checks if it is a tool invocation platform
	 * @param platform Search platform
	 * @return true indicates it is a tool invocation platform
	 */
	public static boolean isToolCallingPlatform(SearchPlatform platform) {
		return platform != null && switch (platform) {
			case OPENALEX, OPENTRIPMAP, TRIPADVISOR, WIKIPEDIA, WORLDBANK_DATA, GOOGLE_SCHOLAR -> true;
			default -> false;
		};
	}

	/**
	 * Checks if the search engine is in the enabled list
	 * @param searchEnum Search engine enumeration
	 * @param enabledSearchEngines List of enabled search engines
	 * @return true indicates it is enabled
	 */
	public static boolean isSearchEngineEnabled(SearchEnum searchEnum, List<String> enabledSearchEngines) {
		if (enabledSearchEngines == null || enabledSearchEngines.isEmpty()) {
			return true;
		}

		String searchName = searchEnum.name().toLowerCase();
		return enabledSearchEngines.contains(searchName);
	}

	/**
	 * Creates a state update Map with intelligent agent related configurations
	 * @param searchPlatforms Selected search platform list
	 * @param agentType Agent type
	 * @return State update Map containing intelligent agent configurations
	 */
	public static Map<String, Object> createSmartAgentStateUpdate(List<SearchEnum> searchPlatforms,
			AgentType agentType) {
		Map<String, Object> stateUpdate = new HashMap<>();

		stateUpdate.put("selectedSearchPlatforms", searchPlatforms);
		stateUpdate.put("agentType", agentType);
		stateUpdate.put("searchPlatformCount", searchPlatforms.size());

		if (!searchPlatforms.isEmpty()) {
			stateUpdate.put("primarySearchEngine", searchPlatforms.get(0).name());
		}

		stateUpdate.put("agentTypeName", agentType.getName());
		stateUpdate.put("agentTypeCode", agentType.getCode());

		logger.debug("Created smart agent state update: agentType={}, searchPlatforms={}", agentType, searchPlatforms);

		return stateUpdate;
	}

	/**
	 * Retrieves the search strategy description for the agent type
	 * @param agentType Agent type
	 * @return Search strategy description
	 */
	public static String getSearchStrategyDescription(AgentType agentType) {
		return switch (agentType) {
			case ACADEMIC_RESEARCH -> "优先使用学术搜索引擎，重点关注论文、期刊和学术资源";
			case LIFESTYLE_TRAVEL -> "优先使用生活和旅游平台，重点关注实用信息和用户体验";
			case ENCYCLOPEDIA -> "优先使用百科和知识库，重点关注权威和准确的基础信息";
			case DATA_ANALYSIS -> "优先使用数据和统计平台，重点关注官方数据和市场分析";
			case GENERAL_RESEARCH -> "使用通用搜索引擎进行综合性研究";
		};
	}

	/**
	 * Unified search selection result encapsulation
	 */
	public static class SearchSelectionResult {

		private final SearchEnum searchEnum;

		private final SearchPlatform searchPlatform;

		private final AgentType agentType;

		private final boolean isToolCalling;

		public SearchSelectionResult(SearchEnum searchEnum, SearchPlatform searchPlatform, AgentType agentType,
				boolean isToolCalling) {
			this.searchEnum = searchEnum;
			this.searchPlatform = searchPlatform;
			this.agentType = agentType;
			this.isToolCalling = isToolCalling;
		}

		public SearchEnum getSearchEnum() {
			return searchEnum;
		}

		public SearchPlatform getSearchPlatform() {
			return searchPlatform;
		}

		public AgentType getAgentType() {
			return agentType;
		}

		public boolean isToolCalling() {
			return isToolCalling;
		}

	}

	/**
	 * Validates if the search platform is valid and available
	 * @param platform Search platform
	 * @param enabledSearchEngines List of enabled search engines
	 * @return true indicates the platform is valid and available
	 */
	public static boolean isValidAndEnabledPlatform(SearchPlatform platform, List<String> enabledSearchEngines) {
		if (platform == null) {
			return false;
		}

		// Tool calling platforms are always valid
		if (isToolCallingPlatform(platform)) {
			return true;
		}

		// Traditional search engines need to check if enabled
		SearchEnum searchEnum = convertToSearchEnum(platform);
		return searchEnum != null && isSearchEngineEnabled(searchEnum, enabledSearchEngines);
	}

}
