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
package com.alibaba.cloud.ai.toolcalling.yuque;

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
 * @author hiriki
 */
@SpringBootTest(classes = { YuqueAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("Yuque Test")
public class YuqueTest {

	@Autowired
	private YuqueQueryBookService yuqueQueryBookService;

	@Autowired
	private YuqueQueryDocService yuqueQueryDocService;

	@Autowired
	private YuqueUpdateDocService yuqueUpdateDocService;

	@Autowired
	private YuqueDeleteDocService yuqueDeleteDocService;

	private static final Logger log = LoggerFactory.getLogger(YuqueTest.class);

	@Test
	@EnabledIfEnvironmentVariable(named = YuqueConstants.TOKEN_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Query Book Tool-Calling Test")
	public void testQueryBook() {
		var resp = yuqueQueryBookService.apply(new YuqueQueryBookService.queryBookRequest("63184104"));
		assert resp != null && resp.meta() != null && resp.data() != null;
		log.info("Query Book Response: {}", resp);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = YuqueConstants.TOKEN_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Query Doc Tool-Calling Test")
	public void testQueryDoc() {
		var resp = yuqueQueryDocService.apply(new YuqueQueryDocService.queryDocRequest("63184104", "223645097"));
		assert resp != null && resp.data() != null;
		log.info("Query Doc Response: {}", resp);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = YuqueConstants.TOKEN_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Update Doc Tool-Calling Test")
	public void testUpdateDoc() {
		var resp = yuqueUpdateDocService.apply(new YuqueUpdateDocService.updateDocRequest("63184104", "223645097",
				"qy3266iux4zw7", "Update Doc Test", 0, "markdown", "Update Doc Test"));
		assert resp != null && resp.data() != null;
		log.info("Update Doc Response: {}", resp);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = YuqueConstants.TOKEN_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("Delete Doc Tool-Calling Test")
	public void testDeleteDoc() {
		var resp = yuqueDeleteDocService.apply(new YuqueDeleteDocService.deleteDocRequest("63184104", "223645097"));
		assert resp != null;
		log.info("Delete Doc Response: {}", resp);
	}

}
