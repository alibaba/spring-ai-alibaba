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
import com.alibaba.cloud.ai.example.deepresearch.util.SearchBeanUtil;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * @author vlsmb
 * @since 2025/7/11
 */
@SpringBootTest
@DisplayName("DeepResearch Search Filter Service Test")
public class SearchFilterServiceTest {

	private static class TestSearchFilterService extends SearchFilterService {

		public TestSearchFilterService(SearchBeanUtil searchBeanUtil) {
			super(searchBeanUtil);
		}

		@Override
		protected Map<String, Double> loadWebsiteWeight() {
			return Map.of("1.example.com", 1.0, "2.example.com", 0.8, "3.example.com", 0.4, "4.example.com", -0.4,
					"5.example.com", -1.0);
		}

	}

	@Autowired
	private SearchBeanUtil searchBeanUtil;

	private SearchFilterService searchFilterService;

	private static final Logger log = LoggerFactory.getLogger(SearchFilterServiceTest.class);

	private final SearchService.SearchResult searchResult = new SearchService.SearchResult(
			List.of(new SearchService.SearchContent("2", "2", "http://2.example.com/test", null),
					new SearchService.SearchContent("1", "1", "https://1.example.com/test", null),
					new SearchService.SearchContent("3", "3", "3.example.com/test", null),
					new SearchService.SearchContent("5", "5", "https://5.example.com/test", null),
					new SearchService.SearchContent("4", "4", "http://4.example.com/test", null),
					new SearchService.SearchContent("unknown", "unknown", "http://unknown.example.com", null)));

	@BeforeEach
	public void setUp() {
		searchFilterService = new TestSearchFilterService(searchBeanUtil);
	}

	@Test
	@DisplayName("Run search result sorting method")
	public void testSearchResultSorting() {
		var resp = searchFilterService.sortSearchResult(true, searchResult);
		log.debug("sorted resp: {}", resp);
		assert resp != null && resp.size() == searchResult.results().size();
		Assertions.assertIterableEquals(resp.stream()
			.map(SearchFilterService.SearchContentWithWeight::content)
			.map(SearchService.SearchContent::title)
			.toList(), List.of("1", "3", "4", "5", "unknown", "2"));
	}

	@Test
	@DisplayName("Run search result filtering method")
	public void testSearchResultFiltering() {
		var resp = searchFilterService.filterSearchResult(true, searchResult);
		log.debug("filtered resp: {}", resp);
		assert resp != null && resp.size() != searchResult.results().size();
		Assertions.assertIterableEquals(resp.stream()
			.map(SearchFilterService.SearchContentWithWeight::content)
			.map(SearchService.SearchContent::title)
			.toList(), List.of("1", "3", "unknown", "2"));
	}

}
