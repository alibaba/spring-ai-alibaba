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
package com.alibaba.cloud.ai.toolcalling.nationalstatistics;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

/**
 * 国家统计局服务测试类
 *
 * @author makoto
 */
@SpringBootTest(classes = { NationalStatisticsAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("National Statistics Test")
class NationalStatisticsServiceTest {

	@Autowired
	private NationalStatisticsService nationalStatisticsService;

	@Autowired
	private SearchService searchService;

	private static final Logger log = Logger.getLogger(NationalStatisticsServiceTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	void nationalStatisticsTest() {
		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", "GDP", 5);
		NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

		assert response != null;
		assert "success".equals(response.status()) || "no_data".equals(response.status());
		assert response.data() != null;

		log.info("Response status: " + response.status());
		log.info("Response message: " + response.message());
		log.info("Data count: " + response.data().size());

		if (!response.data().isEmpty()) {
			log.info("First item: " + response.data().get(0));
		}
	}

	@Test
	@DisplayName("Abstract Search Test")
	void abstractSearchTest() {
		SearchService.Response response = searchService.query("GDP数据");

		assert response != null;
		assert response.getSearchResult() != null;
		assert response.getSearchResult().results() != null;

		log.info("Search results count: " + response.getSearchResult().results().size());

		if (!response.getSearchResult().results().isEmpty()) {
			SearchService.SearchContent first = response.getSearchResult().results().get(0);
			log.info("First search result: " + first.title() + " - " + first.url());
		}
	}

	@Test
	@DisplayName("Test with different data types")
	void testDifferentDataTypes() {
		String[] dataTypes = { "zxfb", "tjgb", "ndsj", "ydsj", "jdsj" };

		for (String dataType : dataTypes) {
			NationalStatisticsService.Request request = new NationalStatisticsService.Request(dataType, null, 3);
			NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

			assert response != null;
			log.info("DataType: " + dataType + ", Status: " + response.status() + ", Count: "
					+ (response.data() != null ? response.data().size() : 0));
		}
	}

	@Test
	@DisplayName("Test error handling")
	void testErrorHandling() {
		// Test null request
		NationalStatisticsService.Response response1 = nationalStatisticsService.apply(null);
		assert response1 != null;
		assert "error".equals(response1.status());
		assert "数据类型不能为空".equals(response1.message());

		// Test empty dataType
		NationalStatisticsService.Request request2 = new NationalStatisticsService.Request("", "test", 10);
		NationalStatisticsService.Response response2 = nationalStatisticsService.apply(request2);
		assert response2 != null;
		assert "error".equals(response2.status());
		assert "数据类型不能为空".equals(response2.message());

		log.info("Error handling tests passed");
	}

	@Test
	@DisplayName("Test keyword filtering")
	void testKeywordFiltering() {
		NationalStatisticsService.Request request = new NationalStatisticsService.Request("zxfb", "工业", 10);
		NationalStatisticsService.Response response = nationalStatisticsService.apply(request);

		assert response != null;
		log.info("Keyword filtering test - Status: " + response.status() + ", Count: "
				+ (response.data() != null ? response.data().size() : 0));
	}

	@Test
	@DisplayName("Test limit parameter")
	void testLimitParameter() {
		// Test with limit 0 (should default to 10)
		NationalStatisticsService.Request request1 = new NationalStatisticsService.Request("zxfb", null, 0);
		assert request1.limit() == 10;

		// Test with specific limit
		NationalStatisticsService.Request request2 = new NationalStatisticsService.Request("zxfb", null, 5);
		assert request2.limit() == 5;

		log.info("Limit parameter tests passed");
	}

}
