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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.tool.SearchBeanUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @since 2025/5/17 18:37
 */

public class BackgroundInvestigationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(BackgroundInvestigationNode.class);

	private final Integer MAX_RETRY_COUNT = 3;

	private final Long RETRY_DELAY_MS = 500L;

	private final JinaCrawlerService jinaCrawlerService;

	private final SearchBeanUtil searchBeanUtil;

	public BackgroundInvestigationNode(SearchBeanUtil searchBeanUtil, JinaCrawlerService jinaCrawlerService) {
		this.jinaCrawlerService = jinaCrawlerService;
		this.searchBeanUtil = searchBeanUtil;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");
		List<String> queries = StateUtil.getOptimizeQueries(state);
		assert queries != null && !queries.isEmpty();
		List<List<Map<String, String>>> resultsList = new ArrayList<>();
		for (String query : queries) {
			SearchService searchService = searchBeanUtil
				.getSearchService(state.value("search_engine", SearchEnum.class).orElseThrow())
				.orElseThrow();

			List<Map<String, String>> results = new ArrayList<>();

			// Retry logic
			for (int i = 0; i < MAX_RETRY_COUNT; i++) {
				try {
					SearchService.Response response = searchService.query(query);
					if (response != null && response.getSearchResult() != null
							&& !response.getSearchResult().results().isEmpty()) {
						results = response.getSearchResult().results().stream().map(info -> {
							Map<String, String> result = new HashMap<>();
							result.put("title", info.title());
							if (jinaCrawlerService == null || !CommonToolCallUtils.isValidUrl(info.url())) {
								result.put("content", info.content());
							}
							else {
								try {
									logger.info("Get detail info of a url using Jina Crawler...");
									result.put("content",
											jinaCrawlerService.apply(new JinaCrawlerService.Request(info.url()))
												.content());
								}
								catch (Exception e) {
									logger.error("Jina Crawler Service Error", e);
									result.put("content", info.content());
								}
							}
							return result;
						}).collect(Collectors.toList());
						break;
					}
				}
				catch (Exception e) {
					logger.warn("搜索尝试 {} 失败: {}", i + 1, e.getMessage());
					Thread.sleep(RETRY_DELAY_MS);
				}
			}
			resultsList.add(results);
		}

		Map<String, Object> resultMap = new HashMap<>();
		if (!resultsList.isEmpty()) {
			List<String> backgroundResults = new ArrayList<>();
			assert resultsList.size() != queries.size();
			for (int i = 0; i < resultsList.size(); i++) {
				List<Map<String, String>> results = resultsList.get(i);
				String query = queries.get(i);
				String prompt = "background investigation query:\n" + query + "\n"
						+ "background investigation results:\n" + results + "\n";
				backgroundResults.add(prompt);
				logger.info("✅ 搜索结果: {} 条", results.size());
			}
			resultMap.put("background_investigation_results", backgroundResults);
		}
		else {
			logger.warn("⚠️ 搜索失败");
		}

		return resultMap;
	}

}
