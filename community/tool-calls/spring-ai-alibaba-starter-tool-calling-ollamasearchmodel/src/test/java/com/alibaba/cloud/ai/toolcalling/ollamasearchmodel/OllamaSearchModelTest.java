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

package com.alibaba.cloud.ai.toolcalling.ollamasearchmodel;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author dahua
 * @since 2025/07/14
 */
@SpringBootTest(classes = { OllamaSearchModelAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("ollamaSearchModel tool call Test")
class OllamaSearchModelTest {

	@Autowired
	private OllamaSearchModelService ollamaSearchModelService;

	private static final Logger logger = LoggerFactory.getLogger(OllamaSearchModelTest.class);

	@Test
	@DisplayName("Tool-Calling Test")
	void testOllamaSearchModel() {
		OllamaSearchModelService.Response result = ollamaSearchModelService
			.apply(new OllamaSearchModelService.Request("qwen3"));
		logger.info("ollamasearchmodel result: {}", result);
	}

}
