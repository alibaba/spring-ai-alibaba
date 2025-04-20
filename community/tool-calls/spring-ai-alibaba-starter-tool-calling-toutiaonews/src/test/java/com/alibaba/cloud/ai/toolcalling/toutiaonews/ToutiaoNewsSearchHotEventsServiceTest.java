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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ToutiaoNewsSearchHotEventsServiceTest
 *
 * @author zhangshenghang
 */
class ToutiaoNewsSearchHotEventsServiceTest {

	private ToutiaoNewsSearchHotEventsService toutiaoNewsSearchHotEventsService = new ToutiaoNewsSearchHotEventsService();

	@Test
	void apply() {
		ToutiaoNewsSearchHotEventsService.Request request = new ToutiaoNewsSearchHotEventsService.Request();
		ToutiaoNewsSearchHotEventsService.Response apply = toutiaoNewsSearchHotEventsService.apply(request);
		Assertions.assertNotNull(apply);
		// Verify the size of events and the title of each event
		Assertions.assertEquals(50, apply.events().size());
		apply.events().forEach(event -> {
			Assertions.assertNotNull(event.title());
		});
	}

}