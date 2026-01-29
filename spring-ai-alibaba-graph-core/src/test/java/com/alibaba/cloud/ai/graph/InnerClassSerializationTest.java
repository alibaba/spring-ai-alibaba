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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for non-static inner class serialization handling.
 * 
 * Issue: Non-static inner classes cannot be deserialized by Jackson because they
 * require an outer class instance. This test verifies that the framework handles
 * this gracefully by degrading to Map instead of throwing exceptions.
 */
public class InnerClassSerializationTest {

	@Test
	void staticNestedClassShouldPreserveTypeAfterSerialization() throws Exception {
		// Static nested class - should work normally with Jackson
		StaticConfig config = new StaticConfig();
		config.model = "gpt-4";
		config.temperature = 1;

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("config", config));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify type is preserved
		Object restoredValue = restoredState.value("config").orElse(null);
		assertNotNull(restoredValue, "Restored value should not be null");
		assertTrue(restoredValue instanceof StaticConfig, 
			"Static nested class should preserve type, but was: " + restoredValue.getClass().getName());

		StaticConfig restoredConfig = (StaticConfig) restoredValue;
		assertEquals("gpt-4", restoredConfig.model, "Model should be preserved");
		assertEquals(1, restoredConfig.temperature, "Temperature should be preserved");
	}

	@Test
	void nonStaticInnerClassShouldDegradeToMapGracefully() throws Exception {
		// Non-static inner class - cannot be deserialized by Jackson
		InnerConfig config = new InnerConfig();
		config.model = "gpt-4";
		config.temperature = 1;

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("config", config));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		
		// Should NOT throw exception
		assertDoesNotThrow(() -> {
			OverAllState restoredState = serializer.cloneObject(originalState);
			
			// Verify data is accessible (degraded to Map)
			Object restoredValue = restoredState.value("config").orElse(null);
			assertNotNull(restoredValue, "Restored value should not be null");
			assertTrue(restoredValue instanceof Map, 
				"Non-static inner class should degrade to Map, but was: " + restoredValue.getClass().getName());

			// Verify data integrity
			@SuppressWarnings("unchecked")
			Map<String, Object> restoredConfig = (Map<String, Object>) restoredValue;
			assertEquals("gpt-4", restoredConfig.get("model"), "Model should be preserved");
			assertEquals(1, restoredConfig.get("temperature"), "Temperature should be preserved");
			
		}, "Non-static inner class serialization should not throw exception");
	}

	@Test
	void nestedMapWithInnerClassShouldSerializeGracefully() throws Exception {
		// Test inner class nested in a Map
		InnerConfig config = new InnerConfig();
		config.model = "gpt-4";
		config.temperature = 1;

		Map<String, Object> nestedMap = new HashMap<>();
		nestedMap.put("innerConfig", config);
		nestedMap.put("someValue", "test");

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("nested", nestedMap));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify nested structure is preserved
		Object restoredValue = restoredState.value("nested").orElse(null);
		assertNotNull(restoredValue, "Nested map should not be null");
		assertTrue(restoredValue instanceof Map, "Nested structure should be a Map");
		
		@SuppressWarnings("unchecked")
		Map<String, Object> restoredNested = (Map<String, Object>) restoredValue;
		
		// Verify nested values
		assertEquals("test", restoredNested.get("someValue"), "Nested value should be preserved");
		assertNotNull(restoredNested.get("innerConfig"), "Inner config should not be null");
		
		// Inner class should degrade to Map
		assertTrue(restoredNested.get("innerConfig") instanceof Map, 
			"Inner class in nested map should degrade to Map");
		
		@SuppressWarnings("unchecked")
		Map<String, Object> restoredConfig = (Map<String, Object>) restoredNested.get("innerConfig");
		assertEquals("gpt-4", restoredConfig.get("model"), "Inner config data should be preserved");
	}

	@Test
	void regularSerializableClassShouldPreserveTypeAfterSerialization() throws Exception {
		// Regular serializable class (simulated as static nested) - should work normally
		TopLevelConfig config = new TopLevelConfig();
		config.model = "gpt-4";
		config.temperature = 1;

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("config", config));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify type is preserved
		Object restoredValue = restoredState.value("config").orElse(null);
		assertNotNull(restoredValue, "Restored value should not be null");
		assertTrue(restoredValue instanceof TopLevelConfig,
			"Regular serializable class should preserve type, but was: " + restoredValue.getClass().getName());

		TopLevelConfig restoredConfig = (TopLevelConfig) restoredValue;
		assertEquals("gpt-4", restoredConfig.model, "Model should be preserved");
		assertEquals(1, restoredConfig.temperature, "Temperature should be preserved");
	}

	@Test
	void mixedStaticAndNonStaticClassesShouldSerializeCorrectly() throws Exception {
		// Test both static and non-static classes in the same state
		StaticConfig staticConfig = new StaticConfig();
		staticConfig.model = "static-model";
		staticConfig.temperature = 1;

		InnerConfig innerConfig = new InnerConfig();
		innerConfig.model = "inner-model";
		innerConfig.temperature = 2;

		Map<String, Object> mixedMap = new HashMap<>();
		mixedMap.put("static", staticConfig);
		mixedMap.put("inner", innerConfig);

		OverAllState originalState = new OverAllState();
		originalState.updateState(Map.of("mixed", mixedMap));

		// Serialize and deserialize
		SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		OverAllState restoredState = serializer.cloneObject(originalState);

		// Verify mixed structure
		@SuppressWarnings("unchecked")
		Map<String, Object> restoredMixed = (Map<String, Object>) restoredState.value("mixed").orElse(null);
		assertNotNull(restoredMixed, "Mixed map should not be null");

		// Static class should preserve type
		Object staticValue = restoredMixed.get("static");
		assertTrue(staticValue instanceof StaticConfig, "Static class should preserve type");
		assertEquals("static-model", ((StaticConfig) staticValue).model);

		// Inner class should degrade to Map
		Object innerValue = restoredMixed.get("inner");
		assertTrue(innerValue instanceof Map, "Inner class should degrade to Map");
		@SuppressWarnings("unchecked")
		Map<String, Object> innerMap = (Map<String, Object>) innerValue;
		assertEquals("inner-model", innerMap.get("model"));
	}

	// ========== Test Classes ==========

	/**
	 * Static nested class - should work normally with Jackson
	 */
	public static class StaticConfig {
		public String model;
		public int temperature;

		public StaticConfig() {
		}
	}

	/**
	 * Non-static inner class - cannot be deserialized by Jackson
	 * (requires outer class instance)
	 */
	public class InnerConfig {
		public String model;
		public int temperature;

		public InnerConfig() {
		}
	}

	/**
	 * Simulates a top-level class for testing - should work normally with Jackson.
	 * Defined as static nested class to keep all test classes within the test file.
	 */
	public static class TopLevelConfig {
		public String model;
		public int temperature;

		public TopLevelConfig() {
		}
	}
}
