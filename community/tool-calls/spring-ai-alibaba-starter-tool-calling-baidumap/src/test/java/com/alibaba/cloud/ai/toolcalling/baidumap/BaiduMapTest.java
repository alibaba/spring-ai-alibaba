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
package com.alibaba.cloud.ai.toolcalling.baidumap;

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
@SpringBootTest(classes = { BaiDuMapAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("Baidu Map Test")
public class BaiduMapTest {

	@Autowired
	private BaiduMapSearchInfoService baiduMapSearchInfoService;

	@Autowired
	private BaiDuMapWeatherService baiDuMapWeatherService;

	private static final Logger log = LoggerFactory.getLogger(BaiduMapTest.class);

	@Test
	@EnabledIfEnvironmentVariable(named = BaiduMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Get Weather Tool-Calling Test")
	public void testGetWeather() {
		var resp = baiDuMapWeatherService.apply(new BaiDuMapWeatherService.Request("沈阳市浑南区"));
		assert resp != null && StringUtils.hasText(resp.message());
		log.info("Response: {}", resp.message());
		assertThat(resp.message()).doesNotContain("failed", "Error");
	}

	@Test
	@EnabledIfEnvironmentVariable(named = BaiduMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Get Address Tool-Calling Test")
	public void testGetAddress() {
		var resp = baiduMapSearchInfoService.apply(new BaiduMapSearchInfoService.Request("北京故宫"));
		assert resp != null && StringUtils.hasText(resp.message());
		log.info("Response: {}", resp.message());
		assertThat(resp.message()).doesNotContain("Failed");
	}

}
