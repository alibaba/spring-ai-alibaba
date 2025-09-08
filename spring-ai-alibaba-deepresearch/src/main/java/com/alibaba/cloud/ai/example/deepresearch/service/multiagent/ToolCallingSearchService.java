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
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.SearchPlatform;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Tool Invocation Search Service
 * Invokes corresponding specialized tool invocation services based on different agent types
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Service
@ConditionalOnProperty(prefix = SmartAgentProperties.PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class ToolCallingSearchService {

	private static final Logger logger = LoggerFactory.getLogger(ToolCallingSearchService.class);

	// Academic research tool
	private final SearchService openAlexService;

	// Travel & lifestyle tools
	private final SearchService openTripMapService;
	private final SearchService tripAdvisorService;

	// Encyclopedia knowledge tools
	private final SearchService wikipediaService;

	// Data analysis tools
	private final SearchService worldBankDataService;
	private final SearchService googleScholarService;

	/**
	 * Constructor injection for all search service dependencies
	 */
	public ToolCallingSearchService(@Nullable @Qualifier("openAlex") SearchService openAlexService,
			@Nullable @Qualifier("openTripMapService") SearchService openTripMapService,
			@Nullable @Qualifier("tripAdvisor") SearchService tripAdvisorService,
			@Nullable @Qualifier("searchWikipedia") SearchService wikipediaService,
			@Nullable @Qualifier("worldBankData") SearchService worldBankDataService,
			@Nullable @Qualifier("googleScholar") SearchService googleScholarService) {
		this.openAlexService = openAlexService;
		this.openTripMapService = openTripMapService;
		this.tripAdvisorService = tripAdvisorService;
		this.wikipediaService = wikipediaService;
		this.worldBankDataService = worldBankDataService;
		this.googleScholarService = googleScholarService;
	}

	/**
	 * Executes tool invocation search based on the search platform
	 * @param platform Search platform
	 * @param query Search query
	 * @return Search results
	 */
	public List<Map<String, String>> performToolCallingSearch(SearchPlatform platform, String query) {
		try {
			SearchService targetService = getSearchService(platform);
			if (targetService == null) {
				logger.warn("未找到对应的工具调用服务: {}", platform);
				return Collections.emptyList();
			}

			SearchService.Response response = targetService.query(query);

			if (response != null && response.getSearchResult() != null) {
				return convertToSearchResults(response.getSearchResult().results(), platform);
			}

		}
		catch (Exception e) {
			logger.error("工具调用搜索失败: platform={}, query={}", platform, query, e);
		}

		return Collections.emptyList();
	}

	/**
	 * Retrieves the corresponding search service based on the search platform
	 */
	private SearchService getSearchService(SearchPlatform platform) {
		return switch (platform) {
			case OPENALEX -> openAlexService;
			case OPENTRIPMAP -> openTripMapService;
			case TRIPADVISOR -> tripAdvisorService;
			case WIKIPEDIA -> wikipediaService;
			case WORLDBANK_DATA -> worldBankDataService;
			case GOOGLE_SCHOLAR -> googleScholarService;
			default -> null;
		};
	}

	/**
	 * Converts tool invocation results to standard search result format
	 */
	private List<Map<String, String>> convertToSearchResults(List<SearchService.SearchContent> contents,
			SearchPlatform platform) {

		List<Map<String, String>> results = new ArrayList<>();

		for (SearchService.SearchContent content : contents) {
			Map<String, String> result = new HashMap<>();
			result.put("title", content.title() != null ? content.title() : "未知标题");
			result.put("content", content.content() != null ? content.content() : "无内容描述");
			result.put("url", content.url() != null ? content.url() : "");
			result.put("weight", "1.0"); // 工具调用结果默认权重
			result.put("source", platform.getName()); // 标识数据来源
			results.add(result);
		}

		logger.info("工具调用搜索完成: platform={}, results_count={}", platform.getName(), results.size());
		return results;
	}

}
