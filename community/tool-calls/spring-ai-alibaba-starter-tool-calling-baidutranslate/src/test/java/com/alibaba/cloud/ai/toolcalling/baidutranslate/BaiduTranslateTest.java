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

package com.alibaba.cloud.ai.toolcalling.baidutranslate;

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

/**
 * @author wang123494
 */
@SpringBootTest(classes = { BaiduTranslateAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("BaiduTranslate Service Test")
public class BaiduTranslateTest {

	private static final Logger log = LoggerFactory.getLogger(BaiduTranslateTest.class);

	@Autowired
	private BaiduTranslateService baiduTranslateService;

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = BaiduTranslateConstants.APP_ID_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = BaiduTranslateConstants.SECRET_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void testBaiduTranslate() {
		BaiduTranslateService.Response resp = baiduTranslateService
			.apply(new BaiduTranslateService.Request("你好", "zh", "en"));
		log.info("Baidu translate service response {}", resp.toString());
		Assertions.assertNotNull(resp, "response body should not be null!");
		Assertions.assertNotNull(resp.translatedTexts(), "translated message should not be null!");
	}

}
