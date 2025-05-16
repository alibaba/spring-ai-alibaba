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

import com.alibaba.cloud.ai.functioncalling.duckduckgo.DuckDuckGoAutoConfiguration;
import com.alibaba.cloud.ai.functioncalling.duckduckgo.DuckDuckGoQueryNewsService;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * @author sixiyida
 */
@SpringBootTest(properties = "debug=true",
		classes = { DuckDuckGoAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
public class DuckDuckGoQueryNewsServiceTest {

	@Autowired
	private DuckDuckGoQueryNewsService duckDuckGoQueryNewsService;

	@Test
	void testDuckDuckGoQueryNewsServiceInjected() {
		Assertions.assertNotNull(duckDuckGoQueryNewsService);
	}

	@Test
	void testApplyWithValidRequest() {
		DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest request = new DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest(
				"Alibaba Cloud", "us-en");

		Map<String, Object> response = duckDuckGoQueryNewsService.apply(request);

		Assertions.assertNotNull(response, "Response should not be null");
		Assertions.assertFalse(response.isEmpty(), "Response map should not be empty");
	}

	@Test
	void testApplyWithInvalidRequest() {
		DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest invalidRequest = new DuckDuckGoQueryNewsService.DuckDuckGoQueryNewsRequest(
				"", "en-us");

		Map<String, Object> response = duckDuckGoQueryNewsService.apply(invalidRequest);

		Assertions.assertNull(response, "Response should be null for invalid request");
	}

}