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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
	public void testDeepCopyPreservesUserMessageType() {
		UserMessage userMessage = UserMessage.builder()
			.text("Hello from user")
			.metadata(Map.of("user_id", "123"))
			.build();

		Map<String, Object> original = new HashMap<>();
		original.put("messages", List.of(userMessage));

		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		Object copiedMessages = copied.get("messages");
		assertInstanceOf(List.class, copiedMessages);
		Object copiedMessage = ((List<?>) copiedMessages).get(0);
		assertInstanceOf(UserMessage.class, copiedMessage);
		assertNotSame(userMessage, copiedMessage, "UserMessage should be deep copied instead of reused");

		UserMessage copiedUserMessage = (UserMessage) copiedMessage;
		assertEquals(userMessage.getText(), copiedUserMessage.getText());
		assertEquals(userMessage.getMetadata(), copiedUserMessage.getMetadata());
	}

	@Test
	public void testDeepCopyPreservesSystemMessageType() {
		SystemMessage systemMessage = SystemMessage.builder()
			.text("You are a helpful assistant")
			.metadata(Map.of("source", "system"))
			.build();

		Map<String, Object> original = new HashMap<>();
		original.put("messages", List.of(systemMessage));

		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		Object copiedMessages = copied.get("messages");
		assertInstanceOf(List.class, copiedMessages);
		Object copiedMessage = ((List<?>) copiedMessages).get(0);
		assertInstanceOf(SystemMessage.class, copiedMessage);
		assertNotSame(systemMessage, copiedMessage, "SystemMessage should be deep copied instead of reused");

		SystemMessage copiedSystemMessage = (SystemMessage) copiedMessage;
		assertEquals(systemMessage.getText(), copiedSystemMessage.getText());
		assertEquals(systemMessage.getMetadata(), copiedSystemMessage.getMetadata());
	}

	@Test
	public void testDeepCopyValueOfUserMessage() {
		UserMessage userMessage = UserMessage.builder()
			.text("Hello from user")
			.metadata(Map.of("user_id", "123"))
			.build();

		Object copied = SerializationUtils.deepCopyValue(userMessage);

		assertInstanceOf(UserMessage.class, copied);
		assertNotSame(userMessage, copied, "UserMessage should be a new instance after deep copy");

		UserMessage copiedUserMessage = (UserMessage) copied;
		assertEquals(userMessage.getText(), copiedUserMessage.getText());
		assertEquals(userMessage.getMetadata(), copiedUserMessage.getMetadata());
	}

	@Test
	public void testDeepCopyValueOfSystemMessage() {
		SystemMessage systemMessage = SystemMessage.builder()
			.text("You are a helpful assistant")
			.metadata(Map.of("priority", 1))
			.build();

		Object copied = SerializationUtils.deepCopyValue(systemMessage);

		assertInstanceOf(SystemMessage.class, copied);
		assertNotSame(systemMessage, copied, "SystemMessage should be a new instance after deep copy");

		SystemMessage copiedSystemMessage = (SystemMessage) copied;
		assertEquals(systemMessage.getText(), copiedSystemMessage.getText());
		assertEquals(systemMessage.getMetadata(), copiedSystemMessage.getMetadata());
	}

	@Test
	public void testDeepCopyUserMessageMetadataIsDecoupled() {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("user_id", "123");
		metadata.put("nested", new HashMap<>(Map.of("deep", "value")));

		UserMessage userMessage = UserMessage.builder()
			.text("Hello")
			.metadata(metadata)
			.build();

		UserMessage copied = (UserMessage) SerializationUtils.deepCopyValue(userMessage);

		// Mutating the original message's metadata must not affect the copy, proving a deep copy.
		userMessage.getMetadata().put("user_id", "changed");

		assertNotEquals(userMessage.getMetadata().get("user_id"), copied.getMetadata().get("user_id"),
				"Copied UserMessage metadata should be decoupled from the original");
		assertEquals("123", copied.getMetadata().get("user_id"));
	}

	@Test
	public void testDeepCopyMixedMessagesInList() {
		UserMessage userMessage = UserMessage.builder().text("Hello").build();
		SystemMessage systemMessage = SystemMessage.builder().text("System prompt").build();

		Map<String, Object> original = new HashMap<>();
		original.put("messages", List.of(systemMessage, userMessage));

		Map<String, Object> copied = SerializationUtils.deepCopyMap(original);

		List<?> copiedMessages = (List<?>) copied.get("messages");
		assertEquals(2, copiedMessages.size());
		assertInstanceOf(SystemMessage.class, copiedMessages.get(0));
		assertInstanceOf(UserMessage.class, copiedMessages.get(1));
		assertNotSame(systemMessage, copiedMessages.get(0));
		assertNotSame(userMessage, copiedMessages.get(1));
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

