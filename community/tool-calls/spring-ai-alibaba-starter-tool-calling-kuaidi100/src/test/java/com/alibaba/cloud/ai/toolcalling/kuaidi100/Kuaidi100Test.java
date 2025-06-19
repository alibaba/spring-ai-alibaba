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

package com.alibaba.cloud.ai.toolcalling.kuaidi100;

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

@SpringBootTest(classes = { Kuaidi100AutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("kuaidi100 tool call Test")
class Kuaidi100Test {

	@Autowired
	private Kuaidi100Service kuaidi100Service;

	private static final Logger logger = LoggerFactory.getLogger(Kuaidi100Test.class);

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = Kuaidi100Constants.APP_ID_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = Kuaidi100Constants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testKuaidi100() {
		Kuaidi100Service.QueryTrackResponse resp = kuaidi100Service
			.apply(new Kuaidi100Service.Request("YT2237659878059"));
		logger.info("kuaidi100 result: {}", resp);
		Assertions.assertNotNull(resp, "response body should not be null!");
	}

}
