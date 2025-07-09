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
package com.alibaba.cloud.ai.toolcalling.duckduckgo;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author vlsmb
 */
@SpringBootTest(classes = { DuckDuckGoAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("DuckDuckGo Query News Service Test")
public class DuckDuckGoTest {

	@Autowired
	private DuckDuckGoQueryNewsService queryNewsService;

	private static final Logger log = LoggerFactory.getLogger(DuckDuckGoTest.class);

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = DuckDuckGoConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void testDuckDuckGoQueryNews() {
		var resp = queryNewsService
			.apply(new DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest("Spring AI Alibaba", "us-en"));
		assert resp != null && !resp.isEmpty();
		log.info("DuckDuckGo Query News Response: {}", resp);
	}

}
