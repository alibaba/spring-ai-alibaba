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
package io.agentscope.core;

import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReActAgentTest {

	@Mock
	private Model model;

	@Mock
	private ChatModel chatModel;

	@Mock
	private Toolkit toolkit;

	private Memory memory;

	private ReActAgent agent;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		memory = new InMemoryMemory();

		when(model.chatModel()).thenReturn(chatModel);
		when(toolkit.toolCallbackProvider()).thenReturn(() -> new org.springframework.ai.tool.ToolCallback[0]);

		agent = ReActAgent.builder()
			.name("TestAgent")
			.sysPrompt("You are a helpful assistant.")
			.model(model)
			.toolkit(toolkit)
			.memory(memory)
			.maxIters(3)
			.build();
	}

	@Test
	void testCallWithSingleMessage() {
		// Arrange
		Msg inputMsg = Msg.builder()
			.name("user")
			.role(MsgRole.USER)
			.content(TextBlock.builder().text("Hello, how are you?").build())
			.build();

		AssistantMessage responseMessage = new AssistantMessage("I'm doing well, thank you!");
		ChatResponse chatResponse = new ChatResponse(java.util.List.of(new Generation(responseMessage)));

		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

		// Act
		Msg result = agent.call(inputMsg);

		// Assert
		assertNotNull(result);
		assertEquals("TestAgent", result.getName());
		assertEquals(MsgRole.ASSISTANT, result.getRole());
		assertTrue(result.getContent() instanceof TextBlock);
		assertEquals("I'm doing well, thank you!", ((TextBlock) result.getContent()).getText());

		verify(chatModel, times(1)).call(any(Prompt.class));
	}

	@Test
	void testStreamWithSingleMessage() {
		// Arrange
		Msg inputMsg = Msg.builder()
			.name("user")
			.role(MsgRole.USER)
			.content(TextBlock.builder().text("Hello!").build())
			.build();

		AssistantMessage responseMessage = new AssistantMessage("Hello there!");
		ChatResponse chatResponse = new ChatResponse(java.util.List.of(new Generation(responseMessage)));

		// Mock both call and stream methods
		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
		when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(chatResponse));

		// Act
		Msg result = agent.stream(inputMsg).blockLast();

		// Assert
		assertNotNull(result);
		assertEquals("TestAgent", result.getName());
		assertEquals(MsgRole.ASSISTANT, result.getRole());
		assertTrue(result.getContent() instanceof TextBlock);
		assertEquals("Hello there!", ((TextBlock) result.getContent()).getText());
	}

	@Test
	void testBuilderPattern() {
		// Act
		ReActAgent testAgent = ReActAgent.builder()
			.name("CustomAgent")
			.sysPrompt("Custom system prompt")
			.model(model)
			.toolkit(toolkit)
			.memory(memory)
			.maxIters(5)
			.build();

		// Assert
		assertNotNull(testAgent);
		// Note: We can't directly test private fields, but we can test behavior
		// The agent should be properly constructed without throwing exceptions
	}

	@Test
	void testMemoryIntegration() {
		// Arrange
		Msg inputMsg = Msg.builder()
			.name("user")
			.role(MsgRole.USER)
			.content(TextBlock.builder().text("Remember this: my name is John").build())
			.build();

		AssistantMessage responseMessage = new AssistantMessage("I'll remember that your name is John.");
		ChatResponse chatResponse = new ChatResponse(java.util.List.of(new Generation(responseMessage)));

		when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

		// Act
		agent.call(inputMsg);

		// Assert
		assertEquals(2, memory.getMessages().size()); // Input + Response
	}

}
