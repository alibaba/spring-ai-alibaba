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

import com.alibaba.cloud.ai.example.deepresearch.service.InfoCheckService;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchFilterService;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
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

	private final InfoCheckService infoCheckService;

	private final SearchFilterService searchFilterService;

	public BackgroundInvestigationNode(JinaCrawlerService jinaCrawlerService, InfoCheckService infoCheckService,
			SearchFilterService searchFilterService) {
		this.jinaCrawlerService = jinaCrawlerService;
		this.infoCheckService = infoCheckService;
		this.searchFilterService = searchFilterService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");
		List<String> queries = StateUtil.getOptimizeQueries(state);
		assert queries != null && !queries.isEmpty();
		List<List<Map<String, String>>> resultsList = new ArrayList<>();
		for (String query : queries) {
			SearchEnum searchEnum = state.value("search_engine", SearchEnum.class).orElseThrow();
			List<Map<String, String>> results = new ArrayList<>();

			// Retry logic
			for (int i = 0; i < MAX_RETRY_COUNT; i++) {
				try {
					results = searchFilterService
						.queryAndFilter(state.value("enable_search_filter", true), searchEnum, query)
						.stream()
						.map(info -> {
							Map<String, String> result = new HashMap<>();
							result.put("title", info.content().title());
							result.put("weight", String.valueOf(info.weight()));
							if (jinaCrawlerService == null || !CommonToolCallUtils.isValidUrl(info.content().url())) {
								result.put("content", info.content().content());
							}
							else {
								try {
									logger.info("Get detail info of a url using Jina Crawler...");
									result.put("content",
											jinaCrawlerService
												.apply(new JinaCrawlerService.Request(info.content().url()))
												.content());
								}
								catch (Exception e) {
									logger.error("Jina Crawler Service Error", e);
									result.put("content", info.content().content());
								}
							}
							return result;
						})
						.collect(Collectors.toList());
					break;
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
				// filter result
				String checkResults = infoCheckService.backgroundInfoCheck(results, query);

				String prompt = "background investigation query:\n" + query + "\n"
						+ "background investigation results:\n" + checkResults + "\n";

				backgroundResults.add(prompt);
			}
			logger.info("✅ 搜索结果: {} 条", backgroundResults.size());
			resultMap.put("background_investigation_results", backgroundResults);
		}
		else {
			logger.warn("⚠️ 搜索失败");
		}

		return resultMap;
	}

}
