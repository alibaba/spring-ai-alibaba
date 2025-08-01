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
package com.alibaba.cloud.ai.toolcalling.worldbankdata;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { CommonToolCallAutoConfiguration.class, WorldBankDataAutoConfiguration.class })
@DisplayName("World Bank Data Test")
public class WorldBankDataTest {

	@Autowired
	private WorldBankDataService worldBankDataService;

	private static final Logger log = Logger.getLogger(WorldBankDataTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test - Specific Implementation")
	public void testWorldBankDataService() {
		// Test with a popular indicator - GDP Current US$
		var resp = worldBankDataService.apply(WorldBankDataService.Request.simpleQuery("NY.GDP.MKTP.CD"));
		assert resp != null && resp.results() != null;
		log.info("GDP Data results: " + resp.results());

		// Test with country specific query
		var chinaPopResp = worldBankDataService
			.apply(new WorldBankDataService.Request("中国人口", "CHN", "SP.POP.TOTL", "2020:2023", "data", 1, 5, null));
		assert chinaPopResp != null && chinaPopResp.results() != null;
		log.info("China Population results: " + chinaPopResp.results());
	}

	@Autowired
	private SearchService searchService;

	@Test
	@DisplayName("Abstract Search Service Test")
	public void testAbstractSearch() {
		// Test using abstract SearchService interface
		var resp = searchService.query("SP.POP.TOTL");
		assert resp != null && resp.getSearchResult() != null && resp.getSearchResult().results() != null
				&& !resp.getSearchResult().results().isEmpty();
		log.info("Abstract search results: " + resp.getSearchResult());

		// Test with another common indicator - Life Expectancy
		var lifeExpResp = searchService.query("SP.DYN.LE00.IN");
		assert lifeExpResp != null && lifeExpResp.getSearchResult() != null;
		log.info("Life Expectancy search results: " + lifeExpResp.getSearchResult());
	}

	@Test
	@DisplayName("Country Code Detection Test")
	public void testCountryCodeDetection() {
		// Test automatic country code detection
		var usaResp = worldBankDataService.apply(WorldBankDataService.Request.simpleQuery("USA"));
		assert usaResp != null && usaResp.results() != null;
		log.info("USA country info: " + usaResp.results());

		// Test with China
		var chnResp = worldBankDataService.apply(WorldBankDataService.Request.simpleQuery("CHN"));
		assert chnResp != null && chnResp.results() != null;
		log.info("China country info: " + chnResp.results());
	}

	@Test
	@DisplayName("Indicator Code Detection Test")
	public void testIndicatorCodeDetection() {
		// Test automatic indicator code detection
		var gdpResp = worldBankDataService.apply(WorldBankDataService.Request.simpleQuery("NY.GDP.MKTP.CD"));
		assert gdpResp != null && gdpResp.results() != null;
		log.info("GDP indicator results: " + gdpResp.results());

		// Test with another indicator
		var popResp = worldBankDataService.apply(WorldBankDataService.Request.simpleQuery("SP.POP.TOTL"));
		assert popResp != null && popResp.results() != null;
		log.info("Population indicator results: " + popResp.results());
	}

}
