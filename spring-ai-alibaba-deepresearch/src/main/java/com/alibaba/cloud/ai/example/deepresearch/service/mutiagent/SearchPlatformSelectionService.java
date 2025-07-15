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

import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.SearchPlatform;
import com.alibaba.cloud.ai.example.deepresearch.util.Multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 搜索平台选择服务，根据Agent类型和问题内容智能选择最合适的搜索平台
 *
 * @author Makoto
 * @since 2025/01/28
 */
@Service
@ConditionalOnProperty(name = "spring.ai.alibaba.deepresearch.smart-agents.enabled", havingValue = "true",
		matchIfMissing = false)
public class SearchPlatformSelectionService {

	private static final Logger logger = LoggerFactory.getLogger(SearchPlatformSelectionService.class);

	@Value("${spring.ai.alibaba.deepresearch.search-list:tavily,aliyun,baidu,serpapi}")
	private List<String> enabledSearchEngines;

	/**
	 * 根据Agent类型选择主要搜索平台
	 */
	public List<SearchEnum> selectSearchPlatforms(AgentType agentType, String question) {
		List<SearchEnum> searchPlatforms = new ArrayList<>();

		try {
			// 根据具体问题内容选择主要搜索平台
			SearchPlatform primaryPlatform = selectPrimaryPlatform(agentType, question);

			SearchEnum primarySearchEnum = SmartAgentUtil.convertToSearchEnum(primaryPlatform);
			if (primarySearchEnum != null
					&& SmartAgentUtil.isSearchEngineEnabled(primarySearchEnum, enabledSearchEngines)) {
				searchPlatforms.add(primarySearchEnum);
			}

			// 如果主要搜索平台不可用，使用默认的通用搜索
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
	 * 根据Agent类型和问题内容选择主要搜索平台
	 */
	private SearchPlatform selectPrimaryPlatform(AgentType agentType, String question) {
		String lowerQuestion = question.toLowerCase();

		switch (agentType) {
			case ACADEMIC_RESEARCH:
				if (lowerQuestion.contains("google scholar") || lowerQuestion.contains("谷歌学术")
						|| lowerQuestion.contains("论文搜索")) {
					return SearchPlatform.GOOGLE_SCHOLAR;
				}
				return SearchPlatform.GOOGLE_SCHOLAR;

			case LIFESTYLE_TRAVEL:
				if (lowerQuestion.contains("小红书") || lowerQuestion.contains("xiaohongshu")
						|| lowerQuestion.contains("红书")) {
					return SearchPlatform.XIAOHONGSHU;
				}
				return SearchPlatform.XIAOHONGSHU;

			case ENCYCLOPEDIA:
				if (lowerQuestion.contains("wikipedia") || lowerQuestion.contains("维基百科")
						|| lowerQuestion.contains("wiki")) {
					return SearchPlatform.WIKIPEDIA;
				}
				return SearchPlatform.WIKIPEDIA;

			case DATA_ANALYSIS:
				if (lowerQuestion.contains("趋势") || lowerQuestion.contains("trends") || lowerQuestion.contains("热度")
						|| lowerQuestion.contains("流行")) {
					return SearchPlatform.GOOGLE_TRENDS;
				}
				else if (lowerQuestion.contains("百度指数") || lowerQuestion.contains("baidu index")) {
					return SearchPlatform.BAIDU_INDEX;
				}
				else if (lowerQuestion.contains("统计局") || lowerQuestion.contains("官方数据")
						|| lowerQuestion.contains("统计数据")) {
					return SearchPlatform.NATIONAL_STATISTICS;
				}
				return SearchPlatform.NATIONAL_STATISTICS;

			default:
				return SearchPlatform.TAVILY;
		}
	}

	/**
	 * 添加默认搜索引擎
	 */
	private void addDefaultSearchEngines(List<SearchEnum> searchPlatforms) {
		List<SearchEnum> defaultEngines = Arrays.asList(SearchEnum.TAVILY, SearchEnum.ALIYUN, SearchEnum.BAIDU,
				SearchEnum.SERPAPI);

		for (SearchEnum engine : defaultEngines) {
			if (SmartAgentUtil.isSearchEngineEnabled(engine, enabledSearchEngines)
					&& !searchPlatforms.contains(engine)) {
				searchPlatforms.add(engine);
				break;
			}
		}
	}

	/**
	 * 获取Agent类型的搜索策略描述
	 */
	public String getSearchStrategyDescription(AgentType agentType) {
		return SmartAgentUtil.getSearchStrategyDescription(agentType);
	}

}
