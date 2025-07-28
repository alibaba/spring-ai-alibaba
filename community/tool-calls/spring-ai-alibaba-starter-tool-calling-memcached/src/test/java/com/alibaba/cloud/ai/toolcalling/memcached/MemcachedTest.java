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

package com.alibaba.cloud.ai.toolcalling.memcached;

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
 * @author dahua
 */
@SpringBootTest(classes = { MemcachedAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("memcached tool call Test")
class MemcachedTest {

	private static final Logger logger = LoggerFactory.getLogger(MemcachedTest.class);

	@Autowired
	private MemcachedService memcachedService;

	@Test
	@DisplayName("Tool-Calling Test Memcached Setter")
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.IP, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.PORT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMemcachedSetter() {
		Boolean apply = memcachedService.setter()
			.apply(new MemcachedService.MemcachedServiceSetter.Request("memcachedKey", "memcachedValue", 0));
		logger.info("set result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Memcached Getter")
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.IP, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.PORT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMemcachedGetter() {
		Object apply = memcachedService.getter()
			.apply(new MemcachedService.MemcachedServiceGetter.Request("spring_ai_alibaba_chat_memory:memcachedId"));
		logger.info("get result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Memcached Deleter")
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.IP, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.PORT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMemcachedDeleter() {
		Boolean apply = memcachedService.deleter()
			.apply(new MemcachedService.MemcachedServiceDeleter.Request("memcachedKey"));
		logger.info("delete result: {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Memcached Replacer")
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.IP, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.PORT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMemcachedReplacer() {
		Boolean apply = memcachedService.replacer()
			.apply(new MemcachedService.MemcachedServiceReplacer.Request("memcachedKey", "memcachedValueNew", 60));
		logger.info("replace result {}", apply);
	}

	@Test
	@DisplayName("Tool-Calling Test Memcached Appender")
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.IP, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = MemcachedConstants.PORT, matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	void testMemcachedAppender() {
		Boolean apply = memcachedService.appender()
			.apply(new MemcachedService.MemcachedServiceAppender.Request("memcachedKey", "memcachedValueAppender"));
		logger.info("append result: {}", apply);
	}

}
