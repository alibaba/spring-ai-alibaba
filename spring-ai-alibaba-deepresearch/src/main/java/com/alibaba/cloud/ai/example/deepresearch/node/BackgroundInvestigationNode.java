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

import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;

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

	private final TavilySearchService tavilySearchService;

	public BackgroundInvestigationNode(TavilySearchService tavilySearchService) {
		this.tavilySearchService = tavilySearchService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");
		String query = StateUtil.getQuery(state);

		List<Map<String, String>> results = new ArrayList<>();

		// Retry logic
		for (int i = 0; i < 2; i++) {
			try {
				TavilySearchService.Response response = tavilySearchService
					.apply(TavilySearchService.Request.simpleQuery(query));

				if (response != null && response.results() != null && !response.results().isEmpty()) {
					results = response.results().stream().map(info -> {
						Map<String, String> result = new HashMap<>();
						result.put("title", info.title());
						result.put("content", info.content());
						return result;
					}).collect(Collectors.toList());
					break;
				}

			}
			catch (Exception e) {
				logger.warn("搜索尝试 {} 失败: {}", i + 1, e.getMessage());
			}

			// Wait 0.5 seconds before retrying if the first attempt fails
			if (i == 0) {
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		Map<String, Object> resultMap = new HashMap<>();
		if (!results.isEmpty()) {
			String prompt = "background investigation results of user query:\n" + results + "\n";
			resultMap.put("background_investigation_results", prompt);
			logger.info("✅ 搜索结果: {} 条", results.size());
		}
		else {
			logger.warn("⚠️ 搜索失败");
			resultMap.put("background_investigation_results", "");
		}

		return resultMap;
	}

}
