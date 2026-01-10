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

import com.alibaba.cloud.ai.graph.utils.SerializationUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link SerializationUtils#deepCopyMap(Map)} to verify that custom Map types
 * (like fastjson2's JSONObject) preserve their type after deep copy operations.
 * <p>
 * This is a regression test for issue #2877 where custom Map implementations extending
 * LinkedHashMap were incorrectly converted to HashMap during deep copy.
 * </p>
 *
 * @author Spring AI Alibaba
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/2877">Issue #2877</a>
 * @since 1.0.0.5
 */
public class SerializationUtilsCustomMapTypeTest {

	/**
	 * Mock JSONObject class that mimics fastjson2's JSONObject behavior.
	 * fastjson2's JSONObject extends LinkedHashMap, so this mock does the same.
	 */
	static class MockJSONObject extends LinkedHashMap<String, Object> {

		public MockJSONObject() {
			super();
		}

		public MockJSONObject(Map<String, Object> map) {
			super(map);
		}

		// JSONObject has additional methods
		public String toJSONString() {
			return this.toString();
		}

	}

	@Test
	public void testDeepCopyPreservesCustomMapType() {
		// Create a MockJSONObject (simulating fastjson2's JSONObject)
		MockJSONObject jsonObject = new MockJSONObject();
		jsonObject.put("name", "test");
		jsonObject.put("age", 25);

		// Put it in a map
		Map<String, Object> original = new HashMap<>();
		original.put("data", jsonObject);

		// Deep copy
		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		// Get the copied value
		Object copiedData = copied.get("data");

		// Verify the type is preserved
		assertInstanceOf(MockJSONObject.class, copiedData,
				"Custom Map type (MockJSONObject) should be preserved after deepCopyMap");

		// Verify the data is correctly copied
		MockJSONObject copiedJson = (MockJSONObject) copiedData;
		assertEquals("test", copiedJson.get("name"));
		assertEquals(25, copiedJson.get("age"));
	}

	@Test
	public void testDeepCopyPreservesNestedCustomMapType() {
		// Create nested JSONObjects
		MockJSONObject innerJson = new MockJSONObject();
		innerJson.put("inner", "value");

		MockJSONObject outerJson = new MockJSONObject();
		outerJson.put("nested", innerJson);
		outerJson.put("name", "outer");

		Map<String, Object> original = new HashMap<>();
		original.put("data", outerJson);

		// Deep copy
		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		Object copiedData = copied.get("data");

		// Verify outer type is preserved
		assertInstanceOf(MockJSONObject.class, copiedData, "Outer MockJSONObject type should be preserved");

		// Verify nested type is also preserved
		MockJSONObject copiedOuter = (MockJSONObject) copiedData;
		Object nestedData = copiedOuter.get("nested");
		assertInstanceOf(MockJSONObject.class, nestedData, "Nested MockJSONObject type should be preserved");

		// Verify data integrity
		MockJSONObject copiedInner = (MockJSONObject) nestedData;
		assertEquals("value", copiedInner.get("inner"));
		assertEquals("outer", copiedOuter.get("name"));
	}

	@Test
	public void testOverAllStatePreservesCustomMapType() {
		// Test with OverAllStateBuilder which uses deepCopyMap internally
		MockJSONObject jsonObject = new MockJSONObject();
		jsonObject.put("key1", "value1");
		jsonObject.put("key2", 123);

		Map<String, Object> inputData = new HashMap<>();
		inputData.put("jsonData", jsonObject);

		OverAllState state = OverAllStateBuilder.builder().withData(inputData).build();

		Object retrievedData = state.data().get("jsonData");

		// Verify type is preserved in OverAllState
		assertInstanceOf(MockJSONObject.class, retrievedData,
				"MockJSONObject type should be preserved in OverAllState");

		// Verify data integrity
		MockJSONObject retrieved = (MockJSONObject) retrievedData;
		assertEquals("value1", retrieved.get("key1"));
		assertEquals(123, retrieved.get("key2"));
	}
}

