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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OverAllStateNullValueTest {

	@Test
	void testOverAllStateWithNullValues() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("nullVar", null);
		variables.put("validVar", "valid");

		OverAllState state = new OverAllState(variables);

		Optional<Object> nullVarOpt = state.value("nullVar");
		Optional<Object> validVarOpt = state.value("validVar");
		Optional<Object> missingVarOpt = state.value("missingVar");

		System.out.println("nullVar present: " + nullVarOpt.isPresent());
		System.out.println("nullVar value: " + nullVarOpt.orElse("NOT_PRESENT"));

		System.out.println("validVar present: " + validVarOpt.isPresent());
		System.out.println("validVar value: " + validVarOpt.orElse("NOT_PRESENT"));

		System.out.println("missingVar present: " + missingVarOpt.isPresent());
		System.out.println("missingVar value: " + missingVarOpt.orElse("NOT_PRESENT"));
	}

}
