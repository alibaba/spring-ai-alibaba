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
package com.alibaba.cloud.ai.toolcalling.dingtalk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

@SpringBootTest(classes = { DingTalkAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("dingTalk robot tool call test")
public class DingTalkRobotTest {

	@Autowired
	private DingTalkRobotService dingTalkRobotService;

	private static final Logger log = LoggerFactory.getLogger(DingTalkRobotTest.class);

	@Test
	@EnabledIfEnvironmentVariable(named = DingTalkConstants.ACCESS_TOKEN_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = DingTalkConstants.SIGNATURE_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@DisplayName("DingTalk Robot Test")
	public void testDingTalkRobot() {
		DingTalkRobotService.Response response = dingTalkRobotService
			.apply(new DingTalkRobotService.Request("spring ai alibaba dingtalk robot tool call testing..."));
		log.info("DingTalk robot service response: {}", response.message());
		Assertions.assertNotNull(response, "response body should not be null!");
		Assertions.assertNotNull(response.message(), "response message should not be null!");
	}

}
