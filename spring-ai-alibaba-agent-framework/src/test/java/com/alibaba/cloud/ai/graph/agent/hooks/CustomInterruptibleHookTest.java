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
package com.alibaba.cloud.ai.graph.agent.hooks;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomInterruptibleHookTest {

	@Test
	void shouldPreservePositionCallbacksForInterruptibleModelHook() throws Exception {
		CustomInterruptibleHook hook = new CustomInterruptibleHook();
		ReactAgent agent = ReactAgent.builder()
				.name("custom-interruptible-hook-agent")
				.model(new StubChatModel())
				.hooks(hook)
				.saver(new MemorySaver())
				.build();

		Optional<NodeOutput> output = agent.invokeAndGetOutput("pause after the model", RunnableConfig.builder()
				.threadId("custom-interruptible-hook-thread")
				.build());

		assertTrue(output.isPresent());
		InterruptionMetadata interruption = assertInstanceOf(InterruptionMetadata.class, output.get());
		assertEquals("custom", interruption.metadata("source").orElseThrow());
		assertEquals(1, hook.beforeModelCalls.get());
		assertEquals(1, hook.afterModelCalls.get());
		assertEquals(0, hook.applyCalls.get());
	}

	@Test
	void shouldPreserveInterruptionForMessagesModelHook() throws Exception {
		CustomInterruptibleMessagesHook hook = new CustomInterruptibleMessagesHook();
		ReactAgent agent = ReactAgent.builder()
				.name("custom-interruptible-messages-hook-agent")
				.model(new StubChatModel())
				.hooks(hook)
				.saver(new MemorySaver())
				.build();

		Optional<NodeOutput> output = agent.invokeAndGetOutput("pause after the model",
				RunnableConfig.builder().threadId("custom-interruptible-messages-hook-thread").build());

		InterruptionMetadata interruption =
				assertInstanceOf(InterruptionMetadata.class, output.orElseThrow());
		assertEquals("messages", interruption.metadata("source").orElseThrow());
		assertEquals(1, hook.afterModelCalls.get());
	}

	@HookPositions({ HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL })
	private static final class CustomInterruptibleHook extends ModelHook
			implements AsyncNodeActionWithConfig, InterruptableAction {

		private final AtomicInteger beforeModelCalls = new AtomicInteger();

		private final AtomicInteger afterModelCalls = new AtomicInteger();

		private final AtomicInteger applyCalls = new AtomicInteger();

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			this.applyCalls.incrementAndGet();
			return CompletableFuture.completedFuture(Map.of());
		}

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			this.beforeModelCalls.incrementAndGet();
			return CompletableFuture.completedFuture(Map.of("before_model_called", true));
		}

		@Override
		public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
			this.afterModelCalls.incrementAndGet();
			return CompletableFuture.completedFuture(Map.of("after_model_called", true));
		}

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			return Optional.empty();
		}

		@Override
		public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
				Map<String, Object> actionResult, RunnableConfig config) {
			if (!nodeId.endsWith(".afterModel")) {
				return Optional.empty();
			}
			assertEquals(true, actionResult.get("after_model_called"));
			return Optional.of(InterruptionMetadata.builder(nodeId, state)
					.addMetadata("source", "custom")
					.build());
		}

		@Override
		public String getName() {
			return "CUSTOM_INTERRUPTION";
		}

	}

	@HookPositions(HookPosition.AFTER_MODEL)
	private static final class CustomInterruptibleMessagesHook extends MessagesModelHook
			implements InterruptableAction {

		private final AtomicInteger afterModelCalls = new AtomicInteger();

		@Override
		public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
			this.afterModelCalls.incrementAndGet();
			return new AgentCommand(previousMessages);
		}

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			return Optional.empty();
		}

		@Override
		public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
				Map<String, Object> actionResult, RunnableConfig config) {
			return Optional.of(InterruptionMetadata.builder(nodeId, state)
					.addMetadata("source", "messages")
					.build());
		}

		@Override
		public String getName() {
			return "CUSTOM_MESSAGES_INTERRUPTION";
		}

	}

	private static final class StubChatModel implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage("model response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

	}

}
