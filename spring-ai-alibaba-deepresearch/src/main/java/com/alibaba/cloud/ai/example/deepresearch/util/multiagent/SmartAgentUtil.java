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
 * 智能Agent系统通用工具类 整合开关检查、类型转换、状态管理等通用逻辑
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class SmartAgentUtil {

	private static final Logger logger = LoggerFactory.getLogger(SmartAgentUtil.class);

	/**
	 * 解析AI分类结果
	 * @param aiResponse AI返回的分类结果
	 * @return 解析后的Agent类型
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
	 * 解析AI搜索平台选择结果
	 * @param aiResponse AI返回的搜索平台选择结果
	 * @return 解析后的搜索平台类型，如果解析失败返回null
	 */
	public static SearchPlatform parseAiSearchPlatformSelection(String aiResponse) {
		if (aiResponse == null || aiResponse.trim().isEmpty()) {
			return null;
		}

		String response = aiResponse.toUpperCase().trim();

		// 移除可能的代码块标记
		response = response.replace("```", "").trim();

		try {
			// 尝试直接匹配平台名称
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
		return null; // 解析失败时返回null，让调用方使用默认值
	}

	/**
	 * 将SearchPlatform转换为SearchEnum
	 * @param platform 搜索平台枚举
	 * @return 对应的SearchEnum
	 */
	public static SearchEnum convertToSearchEnum(SearchPlatform platform) {
		return switch (platform) {
			case TAVILY -> SearchEnum.TAVILY;
			case ALIYUN_AI_SEARCH -> SearchEnum.ALIYUN;
			case BAIDU_SEARCH -> SearchEnum.BAIDU;
			case SERPAPI -> SearchEnum.SERPAPI;

			// 专用工具调用映射 - 返回null表示需要使用工具调用搜索
			case OPENALEX, OPENTRIPMAP, TRIPADVISOR, WIKIPEDIA, WORLDBANK_DATA, GOOGLE_SCHOLAR -> null;
		};
	}

	/**
	 * 检查是否为工具调用平台
	 * @param platform 搜索平台
	 * @return true表示是工具调用平台
	 */
	public static boolean isToolCallingPlatform(SearchPlatform platform) {
		return platform != null && switch (platform) {
			case OPENALEX, OPENTRIPMAP, TRIPADVISOR, WIKIPEDIA, WORLDBANK_DATA, GOOGLE_SCHOLAR -> true;
			default -> false;
		};
	}

	/**
	 * 检查搜索引擎是否在启用列表中
	 * @param searchEnum 搜索引擎枚举
	 * @param enabledSearchEngines 启用的搜索引擎列表
	 * @return true表示已启用
	 */
	public static boolean isSearchEngineEnabled(SearchEnum searchEnum, List<String> enabledSearchEngines) {
		if (enabledSearchEngines == null || enabledSearchEngines.isEmpty()) {
			return true;
		}

		String searchName = searchEnum.name().toLowerCase();
		return enabledSearchEngines.contains(searchName);
	}

	/**
	 * 创建智能Agent相关配置的状态更新Map
	 * @param searchPlatforms 选择的搜索平台列表
	 * @param agentType Agent类型
	 * @return 包含智能Agent配置的状态更新Map
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
	 * 获取Agent类型的搜索策略描述
	 * @param agentType Agent类型
	 * @return 搜索策略描述
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
	 * 统一的搜索选择结果封装
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
	 * 验证搜索平台是否有效且可用
	 * @param platform 搜索平台
	 * @param enabledSearchEngines 启用的搜索引擎列表
	 * @return true表示平台有效且可用
	 */
	public static boolean isValidAndEnabledPlatform(SearchPlatform platform, List<String> enabledSearchEngines) {
		if (platform == null) {
			return false;
		}

		// 工具调用平台总是有效的
		if (isToolCallingPlatform(platform)) {
			return true;
		}

		// 传统搜索引擎需要检查是否启用
		SearchEnum searchEnum = convertToSearchEnum(platform);
		return searchEnum != null && isSearchEngineEnabled(searchEnum, enabledSearchEngines);
	}

}
