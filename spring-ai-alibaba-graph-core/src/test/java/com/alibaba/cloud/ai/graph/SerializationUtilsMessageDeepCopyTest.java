/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests for {@link SerializationUtils#deepCopyValue(Object)} on Spring AI
 * {@code Message} types. Jackson serialization of some Message subtypes fails
 * (they lack deserialization creators), and previously such messages silently
 * fell back to a shallow copy, sharing internal state between the original and
 * the "copy".
 */
public class SerializationUtilsMessageDeepCopyTest {

	@Test
	public void testDeepCopyUserMessageReturnsNewInstance() {
		UserMessage original = new UserMessage("hello user");

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(UserMessage.class, copied);
		assertNotSame(original, copied, "deep copy should return a new instance");
		assertEquals(original.getText(), ((UserMessage) copied).getText());
	}

	@Test
	public void testDeepCopySystemMessageReturnsNewInstance() {
		SystemMessage original = new SystemMessage("system instructions");

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(SystemMessage.class, copied);
		assertNotSame(original, copied, "deep copy should return a new instance");
		assertEquals(original.getText(), ((SystemMessage) copied).getText());
	}

	@Test
	public void testDeepCopyAssistantMessageWithToolCalls() {
		AssistantMessage original = AssistantMessage.builder()
			.content("calling a tool")
			.properties(new HashMap<>(Map.of("customKey", "customValue")))
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "search", "{\"query\":\"test\"}")))
			.build();

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(AssistantMessage.class, copied);
		AssistantMessage copiedMessage = (AssistantMessage) copied;
		assertNotSame(original, copiedMessage, "deep copy should return a new instance, not a shallow copy");
		assertEquals(original.getText(), copiedMessage.getText());
		assertEquals(original.getToolCalls(), copiedMessage.getToolCalls());
		assertEquals("customValue", copiedMessage.getMetadata().get("customKey"));
	}

	@Test
	public void testDeepCopyAssistantMessageMetadataIsIndependent() {
		AssistantMessage original = AssistantMessage.builder()
			.content("answer")
			.properties(new HashMap<>(Map.of("customKey", "customValue")))
			.build();

		AssistantMessage copiedMessage = (AssistantMessage) SerializationUtils.deepCopyValue(original);

		original.getMetadata().put("mutatedAfterCopy", true);
		assertFalse(copiedMessage.getMetadata().containsKey("mutatedAfterCopy"),
				"mutating the original metadata must not affect the copy");
	}

	@Test
	public void testDeepCopyMessageListCopiesElements() {
		AssistantMessage assistantMessage = AssistantMessage.builder()
			.content("assistant reply")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-2", "function", "lookup", "{}")))
			.build();
		List<Object> original = List.of(new UserMessage("user question"), assistantMessage);

		Object copied = SerializationUtils.deepCopyValue(original);

		assertInstanceOf(List.class, copied);
		List<?> copiedList = (List<?>) copied;
		assertEquals(original.size(), copiedList.size());
		assertNotSame(original.get(0), copiedList.get(0), "list elements should be deep copied");
		assertNotSame(original.get(1), copiedList.get(1), "list elements should be deep copied");
		assertEquals(assistantMessage.getToolCalls(), ((AssistantMessage) copiedList.get(1)).getToolCalls());
	}

}
