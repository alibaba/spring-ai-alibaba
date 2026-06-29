/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.state.strategy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AppendStrategyMutationBugTest {

	@Test
	void apply_singleValue_mutatesInputOldValueList() {
		AppendStrategy strategy = new AppendStrategy();

		List<Object> stateList = new ArrayList<>(Arrays.asList("existing"));

		Object result = strategy.apply(stateList, "newValue");

		@SuppressWarnings("unchecked")
		List<Object> resultList = (List<Object>) result;
		assertEquals(2, resultList.size());


		assertEquals(1, stateList.size(),
				"oldValue MUST remain unchanged (size=1); if this is 2, AppendStrategy mutated the input");
		assertNotSame(stateList, result,
				"returned list MUST be a new instance; if same ref, AppendStrategy mutated & returned the input");
	}

}
