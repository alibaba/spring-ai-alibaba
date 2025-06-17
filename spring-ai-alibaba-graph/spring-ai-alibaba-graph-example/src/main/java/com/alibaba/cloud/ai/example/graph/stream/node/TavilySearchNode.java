/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.graph.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TavilySearchNode implements NodeAction {

	@Autowired(required = false)
	private TavilySearchService tavilySearchService;

	@Autowired
	private ChatClient.Builder builder;

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Optional<String> value = state.value(OverAllState.DEFAULT_INPUT_KEY, String.class);
		StringBuilder sb = new StringBuilder();

		if (value.isPresent()) {
			String input = value.get();
			TavilySearchService.Request request = new TavilySearchService.Request(input, null, null, null, 1, null,
					null, null, null, null, null, null, null);
			TavilySearchService.Response apply = tavilySearchService.apply(request);
			if (apply != null) {
				List<TavilySearchService.Response.ResultInfo> results = apply.results();
				if (results != null && !results.isEmpty()) {
					for (TavilySearchService.Response.ResultInfo result : results) {
						sb.append(result.title()).append("\n").append(result.content()).append("\n");
					}
				}

			}
		}
		return Map.of("parallel_result", sb.toString());
	}

}
