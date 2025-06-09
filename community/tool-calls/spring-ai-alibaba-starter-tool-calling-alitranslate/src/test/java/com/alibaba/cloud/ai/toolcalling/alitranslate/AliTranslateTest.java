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

package com.alibaba.cloud.ai.toolcalling.alitranslate;

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

/**
 * @author vlsmb
 */
@SpringBootTest(classes = { AliTranslateAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("ALiTranslate Service Test")
public class AliTranslateTest {

	@Autowired
	private AliTranslateService aliTranslateService;

	private static final Logger log = LoggerFactory.getLogger(AliTranslateTest.class);

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = AliTranslateConstants.ACCESS_KEY_SECRET_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = AliTranslateConstants.ACCESS_KEY_ID_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void testAliTranslate() {
		AliTranslateService.Response resp = aliTranslateService.apply(new AliTranslateService.Request("你好"));
		log.info("Ali Translate Service Response: {}", resp);
		assert resp != null && StringUtils.hasText(resp.translatedTexts());
	}

}
