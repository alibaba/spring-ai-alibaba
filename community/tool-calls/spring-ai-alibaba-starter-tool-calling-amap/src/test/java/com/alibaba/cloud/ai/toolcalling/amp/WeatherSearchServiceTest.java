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
package com.alibaba.cloud.ai.toolcalling.amp;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author vlsmb
 */
@SpringBootTest(classes = { AmapAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("Amap (Gaode) Search Weather Test")
public class WeatherSearchServiceTest {

	@Autowired
	private WeatherSearchService weatherSearchService;

	private static final Logger log = LoggerFactory.getLogger(WeatherSearchServiceTest.class);

	@Test
	@EnabledIfEnvironmentVariable(named = AmapConstants.API_KEY_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Tool-Calling Test")
	public void testWeatherSearch() {
		WeatherSearchService.Response resp = weatherSearchService.apply(new WeatherSearchService.Request("Beijing"));
		assert resp != null && StringUtils.hasText(resp.message());
		log.info("Weather Search Response: {}", resp.message());
		assertThat(resp.message()).doesNotContain("Error");
	}

}
