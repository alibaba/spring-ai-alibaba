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
package com.alibaba.cloud.ai.memory.sqlite;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

class SQLiteChatMemoryTest {

	private SQliteChatMemory chatMemory;

	@BeforeEach
	void setUp() {
		chatMemory = new SQliteChatMemory("test_chat_memory.db");
	}

	@AfterEach
	void tearDown() throws Exception {

		chatMemory.close();
		new java.io.File("test_chat_memory.db").delete();
	}

	@Test
	void testAddAndGetMessages() {

		String conversationId = "test_conversation_id";
		UserMessage message1 = new UserMessage("Hello");
		UserMessage message2 = new UserMessage("World");

		chatMemory.add(conversationId, List.of(message1, message2));
		List<Message> retrievedMessages = chatMemory.get(conversationId, 2);

		assertEquals(2, retrievedMessages.size());
		assertEquals("Hello", retrievedMessages.get(0).getText());
		assertEquals("World", retrievedMessages.get(1).getText());
	}

	@Test
	void testClearOverLimit() {

		String conversationId = "test_conversation_id";
		UserMessage message1 = new UserMessage("Hello");
		UserMessage message2 = new UserMessage("World");
		UserMessage message3 = new UserMessage("Third Message");

		chatMemory.add(conversationId, List.of(message1, message2, message3));
		chatMemory.clearOverLimit(conversationId, 2, 1);

		List<Message> retrievedMessages = chatMemory.get(conversationId, 2);

		assertEquals(2, retrievedMessages.size());
	}

	@Test
	void testClearMessages() {

		String conversationId = "test_conversation_id";
		Message message1 = new UserMessage("Test");

		chatMemory.add(conversationId, List.of(message1));
		chatMemory.clear(conversationId);

		List<Message> retrievedMessages = chatMemory.get(conversationId, 2);
		assertTrue(retrievedMessages.isEmpty());
	}

}
