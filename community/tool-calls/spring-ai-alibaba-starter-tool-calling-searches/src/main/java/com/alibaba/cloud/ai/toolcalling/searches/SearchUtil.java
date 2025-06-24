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

import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author vlsmb
 * @since 2025/6/23
 */
public final class SearchUtil {

	private SearchUtil() {

	}

	/**
	 * Query the currently available implementations of the SearchService plugin. If
	 * multiple implementations exist, return the first available instance according to
	 * the defined loading order.
	 * @param context ApplicationContext
	 * @return the first available SearchService.
	 */
	public static Optional<SearchService> getAvailableSearchService(ApplicationContext context) {
		String toolName = getAvailableSearchToolName(context).orElse(null);
		if (toolName == null) {
			return Optional.empty();
		}
		return getSearchService(context, toolName);
	}

	/**
	 * Query the currently available implementations of the SearchService plugin. If
	 * multiple implementations exist, return the first available instance according to
	 * the defined loading order.
	 * @param context ApplicationContext
	 * @return the first available SearchService Tool Name.
	 */
	public static Optional<String> getAvailableSearchToolName(ApplicationContext context) {
		return Arrays.stream(SearchEnum.values())
			.map(SearchEnum::getToolName)
			.filter(context::containsBean)
			.findFirst();
	}

	/**
	 * Get SearchService by tool name.
	 * @param context ApplicationContext
	 * @param toolName search tool name
	 * @return available SearchService.
	 */
	public static Optional<SearchService> getSearchService(ApplicationContext context, String toolName) {
		try {
			return Optional.of(context.getBean(toolName, SearchService.class));
		}
		catch (Exception e) {
			return Optional.empty();
		}
	}

}
