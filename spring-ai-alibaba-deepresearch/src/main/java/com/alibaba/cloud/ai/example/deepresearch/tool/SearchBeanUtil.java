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

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.alibaba.cloud.ai.toolcalling.searches.SearchUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author vlsmb
 */
@Service
public class SearchBeanUtil {

	private final ApplicationContext context;

	private final List<SearchEnum> searchList;

	public SearchBeanUtil(ApplicationContext context, DeepResearchProperties properties) {
		this.context = context;
		this.searchList = properties.getSearchList();
	}

	/**
	 * Retrieve the service object based on the enum object.
	 */
	public Optional<SearchService> getSearchService(SearchEnum searchEnum) {
		for (SearchEnum defined : searchList) {
			if (defined.equals(searchEnum)) {
				return SearchUtil.getSearchService(context, searchEnum.getToolName());
			}
		}
		// If the search plugin is not configured in deepresearch, no service object will
		// be returned.
		return Optional.empty();
	}

	public Optional<SearchEnum> getFirstAvailableSearch() {
		for (SearchEnum defined : searchList) {
			if (SearchUtil.getSearchService(context, defined.getToolName()).isPresent()) {
				return Optional.of(defined);
			}
		}
		return Optional.empty();
	}

}
