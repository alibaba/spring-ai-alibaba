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
		TavilySearchService.Response response = tavilySearchService
			.apply(TavilySearchService.Request.simpleQuery(query));
		
		List<Map<String, String>> results = new ArrayList<>();
		if (response.results() != null) {
			results = response.results().stream().map(info -> {
				Map<String, String> result = new HashMap<>();
				result.put("title", info.title());
				result.put("content", info.content());
				logger.info("处理搜索结果: {}", result);
				return result;
			}).collect(Collectors.toList());
		} else {
			logger.warn("⚠️ Tavily搜索返回空结果，query: {}, response: {}", query, response);
			// 如果搜索失败，添加默认结果以确保后续流程正常进行
			Map<String, String> defaultResult = new HashMap<>();
			defaultResult.put("title", "搜索服务暂时不可用");
			defaultResult.put("content", "由于搜索服务暂时不可用，无法获取相关背景信息。建议稍后重试或使用其他信息源。");
			results.add(defaultResult);
		}
		
		logger.info("✅ 搜索结果: {}", results);

		Map<String, Object> resultMap = new HashMap<>();

		String prompt = "background investigation results of user query:\n" + results + "\n";
		resultMap.put("background_investigation_results", prompt);
		return resultMap;
	}

}
