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

import com.alibaba.cloud.ai.example.deepresearch.service.SearchFilterService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * @author vlsmb
 * @since 2025/7/10
 */
public class SearchFilterTool {

	private static final Logger log = LoggerFactory.getLogger(SearchFilterTool.class);

	private final SearchFilterService searchFilterService;

	private final SearchEnum searchEnum;

	private final Boolean isEnabledFilter;

	public SearchFilterTool(SearchFilterService searchFilterService, SearchEnum searchEnum, Boolean isEnabledFilter) {
		this.searchFilterService = searchFilterService;
		this.searchEnum = searchEnum;
		this.isEnabledFilter = isEnabledFilter;
	}

	@Tool(description = "Use SearchService to retrieve relevant information and return search results ranked by website trust weights. Information from untrusted websites will be filtered out.")
	public List<SearchFilterService.SearchContentWithWeight> searchFilterTool(
			@ToolParam(description = "Content to be queried using search engines") String query) {
		log.debug("SearchFilterTool start.");
		return searchFilterService.queryAndFilter(isEnabledFilter, searchEnum, query);
	}

}
