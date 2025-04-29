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

package com.alibaba.cloud.ai.toolcalling.baidusearch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaiduSearchServiceTest {

	private final BaiduSearchService baiduSearchService = new BaiduSearchService();

	@Test
	void apply() {
		BaiduSearchService.Request request = new BaiduSearchService.Request("Spring AI", 10);
		BaiduSearchService.Response apply = baiduSearchService.apply(request);
		// assert that the response is not null and contains the expected number of
		// results
		Assertions.assertNotNull(apply);
		Assertions.assertEquals(10, apply.results().size());
		apply.results().forEach(result -> {
			Assertions.assertNotNull(result.title());
			Assertions.assertNotNull(result.abstractText());
		});
	}

}