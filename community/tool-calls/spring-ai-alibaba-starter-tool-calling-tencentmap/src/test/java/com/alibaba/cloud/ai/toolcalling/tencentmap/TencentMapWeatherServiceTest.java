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
package com.alibaba.cloud.ai.toolcalling.tencentmap;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HunterPorter
 */
@SpringBootTest(classes = { TencentMapAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("Tencent Map Weather Test")
public class TencentMapWeatherServiceTest {

	@Autowired
	private TencentMapWeatherService weatherSearchService;

	private static final Logger log = LoggerFactory.getLogger(TencentMapWeatherServiceTest.class);

	@Test
	@EnabledIfEnvironmentVariable(named = TencentMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Tencent Map Tool-Calling Test")
	public void testWeatherSearch() {
		TencentMapWeatherService.Response resp = weatherSearchService
			.apply(new TencentMapWeatherService.Request("北京天安门", null));
		Assertions.assertNotNull(resp, "Response should not be null");
		log.info("Weather Search Response: {}", resp.message());
		assertThat(resp.message()).doesNotContain("Error");
	}

}
