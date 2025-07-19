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

import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.SearchPlatform;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

			// TODO: 根据实际情况添加更多映射,并且暂时使用默认引擎
			case GOOGLE_SCHOLAR -> SearchEnum.SERPAPI;
			case XIAOHONGSHU -> SearchEnum.BAIDU;
			case WIKIPEDIA -> SearchEnum.TAVILY;
			case NATIONAL_STATISTICS -> SearchEnum.ALIYUN;
			case GOOGLE_TRENDS -> SearchEnum.SERPAPI;
			case BAIDU_INDEX -> SearchEnum.BAIDU;
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
	 * 更新状态中的智能Agent相关配置
	 * @param state 全局状态
	 * @param searchPlatforms 选择的搜索平台列表
	 * @param agentType Agent类型
	 */
	public static void updateStateWithSmartAgentConfig(OverAllState state, List<SearchEnum> searchPlatforms,
			AgentType agentType) {
		state.data().put("selectedSearchPlatforms", searchPlatforms);
		state.data().put("agentType", agentType);
		state.data().put("searchPlatformCount", searchPlatforms.size());

		if (!searchPlatforms.isEmpty()) {
			state.data().put("primarySearchEngine", searchPlatforms.get(0).name());
		}

		state.data().put("agentTypeName", agentType.getName());
		state.data().put("agentTypeCode", agentType.getCode());

		logger.debug("Updated state with smart agent config: agentType={}, searchPlatforms={}", agentType,
				searchPlatforms);
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

}
