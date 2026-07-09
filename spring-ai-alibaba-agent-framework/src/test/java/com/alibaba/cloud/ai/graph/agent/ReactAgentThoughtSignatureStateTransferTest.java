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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactAgentThoughtSignatureStateTransferTest {

	private static final String THOUGHT_SIGNATURES_KEY = "thoughtSignatures";

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldPreserveThoughtSignaturesAcrossToolRoundWithMemorySaver(boolean parallelToolExecution) throws Exception {
		List<byte[]> signatures = List.of(new byte[] { 21, 22, 23, 24 }, new byte[] { 31, 32, 33, 34 });
		SignatureAwareChatModel chatModel = new SignatureAwareChatModel(signatures);
		RunnableConfig config = RunnableConfig.builder()
			.threadId("thought-signature-thread-" + parallelToolExecution)
			.build();
		ReactAgent agent = ReactAgent.builder()
			.name("thought_signature_agent")
			.model(chatModel)
			.tools(tool("read_skill", "skill-content"), tool("search_skill", "search-content"))
			.parallelToolExecution(parallelToolExecution)
			.saver(new MemorySaver())
			.build();

		AssistantMessage firstResult = agent.call("Read the skill before answering.", config);
		AssistantMessage secondResult = agent.call("Continue with the saved context.", config);

		assertEquals("done", firstResult.getText());
		assertEquals("continued", secondResult.getText());
		assertEquals(3, chatModel.callCount());
		assertTrue(chatModel.toolRoundPromptValidated(),
				"The prompt after tool execution should include the preserved assistant thought signature");
		assertTrue(chatModel.resumedPromptValidated(),
				"The resumed model prompt should include the preserved assistant thought signature");
	}

	@Test
	void shouldNotDowngradeAssistantMessageSubclassesWhenRestoringThoughtSignatures() throws Exception {
		SubclassAwareChatModel chatModel = new SubclassAwareChatModel();
		DeepSeekAssistantMessage assistantMessage = new DeepSeekAssistantMessage.Builder()
			.content("Need reasoning")
			.reasoningContent("reasoning-content")
			.properties(Map.of(THOUGHT_SIGNATURES_KEY, List.of(Map.of("data", List.of(1, 2, 3)))))
			.build();
		ReactAgent agent = ReactAgent.builder().name("thought_signature_subclass_agent").model(chatModel).build();

		AssistantMessage result = agent.call(
				List.of(new UserMessage("Start"), assistantMessage, new UserMessage("Continue")));

		assertEquals("ok", result.getText());
		assertTrue(chatModel.promptValidated(),
				"The prompt should keep provider-specific assistant message subclasses untouched");
	}

	private static ToolCallback tool(String name, String result) {
		return new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder()
					.name(name)
					.description("Test tool")
					.inputSchema("{}")
					.build();
			}

			@Override
			public String call(String toolInput, ToolContext toolContext) {
				return result;
			}

			@Override
			public String call(String toolInput) {
				return call(toolInput, new ToolContext(Map.of()));
			}
		};
	}

	private static final class SignatureAwareChatModel implements ChatModel {

		private final List<byte[]> expectedSignatures;

		private final AtomicInteger callCount = new AtomicInteger();

		private final AtomicBoolean toolRoundPromptValidated = new AtomicBoolean();

		private final AtomicBoolean resumedPromptValidated = new AtomicBoolean();

		private SignatureAwareChatModel(List<byte[]> expectedSignatures) {
			this.expectedSignatures = expectedSignatures;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			int currentCall = callCount.incrementAndGet();
			if (currentCall == 1) {
				return response(toolCallMessage());
			}
			if (currentCall == 2) {
				assertThoughtSignaturePreserved(prompt);
				toolRoundPromptValidated.set(true);
				return response(new AssistantMessage("done"));
			}
			if (currentCall == 3) {
				assertThoughtSignaturePreserved(prompt);
				resumedPromptValidated.set(true);
				return response(new AssistantMessage("continued"));
			}
			throw new AssertionError("Unexpected model call count: " + currentCall);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return callCount.get();
		}

		private boolean toolRoundPromptValidated() {
			return toolRoundPromptValidated.get();
		}

		private boolean resumedPromptValidated() {
			return resumedPromptValidated.get();
		}

		private AssistantMessage toolCallMessage() {
			AssistantMessage.ToolCall readSkillCall = new AssistantMessage.ToolCall("call-1", "function", "read_skill",
					"{\"input\":\"x\"}");
			AssistantMessage.ToolCall searchSkillCall = new AssistantMessage.ToolCall("call-2", "function",
					"search_skill",
					"{\"input\":\"x\"}");
			return AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(readSkillCall, searchSkillCall))
				.properties(Map.of(THOUGHT_SIGNATURES_KEY, expectedSignatures))
				.build();
		}

		private void assertThoughtSignaturePreserved(Prompt prompt) {
			List<Message> instructions = prompt.getInstructions();
			AssistantMessage assistantMessage = instructions.stream()
				.filter(AssistantMessage.class::isInstance)
				.map(AssistantMessage.class::cast)
				.filter(AssistantMessage::hasToolCalls)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Tool-calling assistant message was not found"));
			assertTrue(instructions.stream().anyMatch(ToolResponseMessage.class::isInstance),
					"Tool response should be present before the second model call");
			ToolResponseMessage toolResponseMessage = instructions.stream()
				.filter(ToolResponseMessage.class::isInstance)
				.map(ToolResponseMessage.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Tool response message was not found"));
			assertEquals(2, toolResponseMessage.getResponses().size());

			Object signaturesValue = assistantMessage.getMetadata().get(THOUGHT_SIGNATURES_KEY);
			List<?> signatures = assertInstanceOf(List.class, signaturesValue);
			assertEquals(expectedSignatures.size(), signatures.size());
			for (int i = 0; i < expectedSignatures.size(); i++) {
				byte[] signature = assertInstanceOf(byte[].class, signatures.get(i));
				assertArrayEquals(expectedSignatures.get(i), signature);
			}
		}

		private ChatResponse response(AssistantMessage message) {
			return new ChatResponse(List.of(new Generation(message)));
		}

	}

	private static final class SubclassAwareChatModel implements ChatModel {

		private final AtomicBoolean promptValidated = new AtomicBoolean();

		@Override
		public ChatResponse call(Prompt prompt) {
			DeepSeekAssistantMessage assistantMessage = prompt.getInstructions()
				.stream()
				.filter(DeepSeekAssistantMessage.class::isInstance)
				.map(DeepSeekAssistantMessage.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("DeepSeek assistant message was not found"));

			assertEquals("reasoning-content", assistantMessage.getReasoningContent());
			Object signaturesValue = assistantMessage.getMetadata().get(THOUGHT_SIGNATURES_KEY);
			List<?> signatures = assertInstanceOf(List.class, signaturesValue);
			assertInstanceOf(Map.class, signatures.get(0));
			promptValidated.set(true);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private boolean promptValidated() {
			return promptValidated.get();
		}

	}

}
