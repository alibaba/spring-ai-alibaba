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
package com.alibaba.cloud.ai.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for verifying that keys without explicitly set strategies automatically use
 * the default REPLACE strategy.
 *
 * @author disaster
 * @since 1.0.0.1
 */
public class DefaultKeyStrategyTest {

	@Test
	public void testDefaultKeyStrategyIsReplace() {
		// Create state without any explicit key strategies
		OverAllState state = OverAllStateBuilder.builder()
			.putData("key1", "original_value1")
			.putData("key2", "original_value2")
			.build();

		// Update with new values - should use REPLACE strategy by default
		Map<String, Object> updates = new HashMap<>();
		updates.put("key1", "new_value1");
		updates.put("key3", "new_value3"); // New key

		state.updateState(updates);

		// Verify that replace strategy was used (old values replaced, new key added)
		assertEquals("new_value1", state.data().get("key1"));
		assertEquals("original_value2", state.data().get("key2")); // Unchanged
		assertEquals("new_value3", state.data().get("key3")); // New key added
	}

	@Test
	public void testExplicitStrategyOverridesDefault() {
		// Create state with explicit APPEND strategy for one key
		OverAllState state = OverAllStateBuilder.builder()
			.putData("append_key", "original")
			.putData("replace_key", "original")
			.withKeyStrategy("append_key", KeyStrategy.APPEND)
			// No explicit strategy for replace_key - should use default REPLACE
			.build();

		// Update both keys
		Map<String, Object> updates = new HashMap<>();
		updates.put("append_key", "_appended");
		updates.put("replace_key", "replaced");

		state.updateState(updates);

		// Verify different strategies were applied
		// APPEND strategy creates a list when oldValue is not already a List
		assertEquals(List.of("_appended"), state.data().get("append_key")); // APPEND used
		assertEquals("replaced", state.data().get("replace_key")); // REPLACE used
		// (default)
	}

	@Test
	public void testDefaultStrategyWithNullKeyStrategies() {
		// Create state with null key strategies map
		Map<String, Object> initialData = new HashMap<>();
		initialData.put("key1", "value1");
		OverAllState state = new OverAllState(initialData, null, // null key strategies
				false);

		// Update should still work with default REPLACE strategy
		Map<String, Object> updates = new HashMap<>();
		updates.put("key1", "new_value1");
		updates.put("key2", "value2");
		state.updateState(updates);

		assertEquals("new_value1", state.data().get("key1"));
		assertEquals("value2", state.data().get("key2"));
	}

}
