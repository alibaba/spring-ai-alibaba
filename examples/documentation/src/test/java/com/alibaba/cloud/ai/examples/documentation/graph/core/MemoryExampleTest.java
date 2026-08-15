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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MemoryExampleTest {

	@Test
	void shouldConvertEntireCheckpointHistoryToChatMessages() {
		List<Map<String, String>> history = List.of(
				Map.of("role", "system", "content", "Remember user details"),
				Map.of("role", "user", "content", "My name is Bob"),
				Map.of("role", "assistant", "content", "Hello Bob"),
				Map.of("role", "user", "content", "What is my name?"));

		List<Message> messages = MemoryExample.toChatMessages(history);

		assertEquals(4, messages.size());
		assertInstanceOf(SystemMessage.class, messages.get(0));
		assertInstanceOf(UserMessage.class, messages.get(1));
		assertInstanceOf(AssistantMessage.class, messages.get(2));
		assertInstanceOf(UserMessage.class, messages.get(3));
		assertEquals("My name is Bob", messages.get(1).getText());
		assertEquals("What is my name?", messages.get(3).getText());
	}

}
