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

package com.alibaba.cloud.ai.toolcalling.youdaotranslate;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(classes = { YoudaoTranslateAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("youdao translate tool call Test")
class YoudaotranslateTest {

	@Autowired
	private YoudaoTranslateService youdaoTranslateService;

	private static final Logger logger = LoggerFactory.getLogger(YoudaotranslateTest.class);

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = YoudaoTranslateConstants.APP_ID_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = YoudaoTranslateConstants.SECRET_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testYoudaoTranslate() {
		YoudaoTranslateService.Response resp = youdaoTranslateService
			.apply(new YoudaoTranslateService.Request("你好,明天", "zh", "en"));
		logger.info("youdao translate result: {}", resp);
		Assertions.assertNotNull(resp, "response body should not be null!");
		Assertions.assertNotNull(resp.translatedTexts(), "translated message should not be null!");
	}

}
