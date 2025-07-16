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

package com.alibaba.cloud.ai.toolcalling.toutiaonews;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { CommonToolCallAutoConfiguration.class, ToutiaoNewsAutoConfiguration.class })
@DisplayName("Toutiao News Test")
class ToutiaoNewsSearchHotEventsTest {

	@Autowired
	private ToutiaoNewsSearchHotEventsService toutiaoNewsSearchHotEventsService;

	private static final Logger log = Logger.getLogger(ToutiaoNewsSearchHotEventsTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	public void testGetHotEventFromToutiaoNews() {
		var resp = toutiaoNewsSearchHotEventsService.apply(new ToutiaoNewsSearchHotEventsService.Request());
		assert resp != null && resp.events() != null;
		log.info("results: " + resp.events());
	}

}
