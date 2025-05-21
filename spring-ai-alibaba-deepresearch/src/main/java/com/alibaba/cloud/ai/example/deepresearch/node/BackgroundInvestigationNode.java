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

import com.alibaba.cloud.ai.example.deepresearch.model.SearchedContent;
import com.alibaba.cloud.ai.example.deepresearch.model.TavilySearchResponse;
import com.alibaba.cloud.ai.example.deepresearch.tool.tavily.TavilySearchApi;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/5/17 18:37
 */

public class BackgroundInvestigationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(BackgroundInvestigationNode.class);

	private final TavilySearchApi tavilySearchApi;

	public BackgroundInvestigationNode(TavilySearchApi tavilySearchApi) {
		this.tavilySearchApi = tavilySearchApi;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");
		List<Message> messages = state.value("messages", List.class)
			.map(obj -> new ArrayList<>((List<Message>) obj))
			.orElseGet(ArrayList::new);
		Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
		String query = lastMessage.getText();
		TavilySearchResponse response = tavilySearchApi.search(query);
		ArrayList<SearchedContent> results = new ArrayList<>();
		for (TavilySearchResponse.ResultInfo resultInfo : response.getResults()) {
			results.add(new SearchedContent(resultInfo.getTitle(), resultInfo.getContent()));
		}
		logger.info("✅ 搜索结果: {}", results);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("background_investigation_results", results);
		return resultMap;
	}

}
