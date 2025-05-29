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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import com.alibaba.cloud.ai.example.deepresearch.model.TavilySearchResponse;
import com.alibaba.cloud.ai.example.deepresearch.tool.tavily.TavilySearchApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Recommend use {@link com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService} tool.
 */
@Service
@Deprecated
public class WebSearchTool {

	@Autowired
	private TavilySearchApi tavilySearchApi;

	@Tool(description = "Search the web for information")
	public String search(@ToolParam(description = "search content") String query) {
		TavilySearchResponse search = tavilySearchApi.search(query);
		return search.getResults().get(0).getContent();
	}

}
