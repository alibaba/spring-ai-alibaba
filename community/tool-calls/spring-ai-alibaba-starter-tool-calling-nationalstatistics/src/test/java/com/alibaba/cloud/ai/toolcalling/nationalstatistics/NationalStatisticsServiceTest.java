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
 * @author Makoto
 */
@SpringBootTest(classes = { NationalStatisticsAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("National Statistics Test")
public class NationalStatisticsServiceTest {

	@Autowired
	private NationalStatisticsService nationalStatisticsService;

	private static final Logger log = Logger.getLogger(NationalStatisticsServiceTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	public void testNationalStatistics() {
		var resp = nationalStatisticsService.apply(NationalStatisticsService.Request.simpleQuery("GDP"));
		assert resp != null;
		log.info("query: " + resp.query());
		log.info("success: " + resp.success());
		log.info("message: " + resp.message());
		log.info("results count: " + (resp.data() != null ? resp.data().size() : 0));
	}

	@Autowired
	private SearchService searchService;

	@Test
	@DisplayName("Abstract Search Test")
	public void testAbstractSearch() {
		var resp = searchService.query("人口");
		assert resp != null && resp.getSearchResult() != null && resp.getSearchResult().results() != null;
		log.info("search results: " + resp.getSearchResult().results().size());
		if (!resp.getSearchResult().results().isEmpty()) {
			var firstResult = resp.getSearchResult().results().get(0);
			log.info("first result: " + firstResult.title() + " - " + firstResult.content());
		}
	}

	@Test
	@DisplayName("Request Record Test")
	public void testRequestRecord() {
		var request = new NationalStatisticsService.Request("GDP", "2023", "全国");
		assert "GDP".equals(request.keyword());
		assert "2023".equals(request.year());
		assert "全国".equals(request.region());

		var simpleRequest = NationalStatisticsService.Request.simpleQuery("人口");
		assert "人口".equals(simpleRequest.keyword());
		assert simpleRequest.year() == null;
		assert simpleRequest.region() == null;
	}

	@Test
	@DisplayName("Response Record Test")
	public void testResponseRecord() {
		var successResponse = NationalStatisticsService.Response.success("GDP", "查询成功", java.util.Arrays.asList());
		assert "GDP".equals(successResponse.query());
		assert successResponse.success();
		assert "查询成功".equals(successResponse.message());
		assert successResponse.data() != null;

		var errorResponse = NationalStatisticsService.Response.error("GDP", "查询失败");
		assert "GDP".equals(errorResponse.query());
		assert !errorResponse.success();
		assert "查询失败".equals(errorResponse.message());
		assert errorResponse.data() != null;
		assert errorResponse.data().isEmpty();
	}

	@Test
	@DisplayName("Statistics Data Test")
	public void testStatisticsData() {
		var data = new NationalStatisticsService.StatisticsData();
		data.setName("国内生产总值");
		data.setValue("1143670");
		data.setUnit("亿元");
		data.setYear("2023");
		data.setCode("A020101");

		assert "国内生产总值".equals(data.getName());
		assert "1143670".equals(data.getValue());
		assert "亿元".equals(data.getUnit());
		assert "2023".equals(data.getYear());
		assert "A020101".equals(data.getCode());
	}

	@Test
	@DisplayName("Properties Test")
	public void testPropertiesDefaultValues() {
		var properties = new NationalStatisticsProperties();
		assert NationalStatisticsConstants.BASE_URL.equals(properties.getBaseUrl());
		assert properties.getMaxResults() == 10;
		assert properties.isEnabled(); // from CommonToolCallProperties
	}

}
