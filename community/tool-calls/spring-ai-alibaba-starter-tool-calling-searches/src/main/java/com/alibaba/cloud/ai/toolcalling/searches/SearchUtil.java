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
package com.alibaba.cloud.ai.toolcalling.searches;

import com.alibaba.cloud.ai.toolcalling.baidusearch.BaiduSearchConstants;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.alibaba.cloud.ai.toolcalling.serpapi.SerpApiConstants;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchConstants;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * @author vlsmb
 * @since 2025/6/23
 */
public final class SearchUtil {

	private static final String[] SEARCH_TOOL_NAMES = { TavilySearchConstants.TOOL_NAME, BaiduSearchConstants.TOOL_NAME,
			SerpApiConstants.TOOL_NAME };

	private SearchUtil() {

	}

	/**
	 * Query the currently available implementations of the SearchService plugin. If
	 * multiple implementations exist, return the first available instance according to
	 * the defined loading order.
	 * @param context ApplicationContext
	 * @return the first available SearchService
	 * @throws RuntimeException When there is no available Service, throw RuntimeException
	 */
	public static SearchService getAvailableSearchService(ApplicationContext context) throws RuntimeException {
		String toolName = getAvailableSearchToolName(context);
		if (toolName == null) {
			throw new RuntimeException("No Available Search Tool");
		}
		return getSearchService(context, toolName);
	}

	/**
	 * Query the currently available implementations of the SearchService plugin. If
	 * multiple implementations exist, return the first available instance according to
	 * the defined loading order.
	 * @param context ApplicationContext
	 * @return the first available SearchService Tool Name, or null.
	 */
	public static String getAvailableSearchToolName(ApplicationContext context) {
		return Arrays.stream(SEARCH_TOOL_NAMES).filter(context::containsBean).findFirst().orElse(null);
	}

	/**
	 * Get SearchService by tool name.
	 * @param context ApplicationContext
	 * @param toolName search tool name
	 * @return available SearchService
	 * @throws RuntimeException When this tool is unavailable, throw RuntimeException
	 */
	public static SearchService getSearchService(ApplicationContext context, String toolName) throws RuntimeException {
		try {
			return context.getBean(toolName, SearchService.class);
		}
		catch (Exception e) {
			throw new RuntimeException("The tool is unavailable.", e);
		}
	}

}
