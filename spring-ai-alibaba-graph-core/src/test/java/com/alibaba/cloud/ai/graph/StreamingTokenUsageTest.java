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

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.stream.LLmNodeAction;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamingTokenUsageTest {

	private static final class TestUsage implements Usage {
		private final Integer promptTokens;
		private final Integer completionTokens;
		private final Integer totalTokens;
		private final Map<String, Object> nativeUsage;

		private TestUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
			this.promptTokens = promptTokens;
			this.completionTokens = completionTokens;
			this.totalTokens = totalTokens;
			this.nativeUsage = Map.of("prompt_tokens", promptTokens,
					"completion_tokens", completionTokens,
					"total_tokens", totalTokens);
		}

		@Override
		public Integer getPromptTokens() {
			return promptTokens;
		}

		@Override
		public Integer getCompletionTokens() {
			return completionTokens;
		}

		@Override
		public Integer getTotalTokens() {
			return totalTokens;
		}

		@Override
		public Map<String, Object> getNativeUsage() {
			return nativeUsage;
		}
	}

	private static ChatResponse createUsageOnlyChatResponse(Usage usage) {
		try {
			Class<?> metadataClass = Class.forName("org.springframework.ai.chat.metadata.ChatResponseMetadata");
			Object metadata = buildChatResponseMetadata(metadataClass, usage);
			Constructor<ChatResponse> ctor = ChatResponse.class.getConstructor(List.class, metadataClass);
			return ctor.newInstance(List.of(new Generation(null, ChatGenerationMetadata.NULL)), metadata);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to construct ChatResponse with usage metadata", ex);
		}
	}

	private static Object buildChatResponseMetadata(Class<?> metadataClass, Usage usage) throws Exception {
		try {
			Method builderMethod = metadataClass.getMethod("builder");
			Object builder = builderMethod.invoke(null);
			Method usageMethod = builder.getClass().getMethod("usage", Usage.class);
			Object builderWithUsage = usageMethod.invoke(builder, usage);
			Method buildMethod = builderWithUsage.getClass().getMethod("build");
			return buildMethod.invoke(builderWithUsage);
		} catch (NoSuchMethodException ignored) {
			// Fall through to other construction strategies
		}

		try {
			Constructor<?> ctor = metadataClass.getConstructor(Usage.class);
			return ctor.newInstance(usage);
		} catch (NoSuchMethodException ignored) {
			// Fall through to static factory
		}

		for (Method method : metadataClass.getMethods()) {
			if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1
					&& method.getParameterTypes()[0].isAssignableFrom(Usage.class)
					&& metadataClass.isAssignableFrom(method.getReturnType())) {
				return method.invoke(null, usage);
			}
		}

		throw new NoSuchMethodException("No suitable ChatResponseMetadata factory found");
	}

	private static ChatModel createMockChatModelWithUsageChunk(Usage usage) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage("test"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.concat(
					Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("hi"))))),
					Flux.just(createUsageOnlyChatResponse(usage)).delayElements(Duration.ofMillis(5)),
					Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("!"))))).delayElements(Duration.ofMillis(5))
				);
			}
		};
	}

	@Test
	public void testUsageOnlyChunkIsNotDropped() throws Exception {
		Usage usage = new TestUsage(1, 2, 3);
		ChatModel chatModel = createMockChatModelWithUsageChunk(usage);

		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		})
			.addNode("llmNode", node_async(new LLmNodeAction(chatModel, "llmNode")))
			.addEdge(START, "llmNode")
			.addEdge("llmNode", END);

		CompiledGraph compiledGraph = stateGraph.compile();

		Map<String, Object> input = new HashMap<>();
		input.put(OverAllState.DEFAULT_INPUT_KEY, "test");

		List<NodeOutput> outputs = compiledGraph.stream(input)
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(outputs, "Streaming outputs should not be null");
		assertTrue(outputs.stream().anyMatch(output -> output.tokenUsage().getTotalTokens() == 3),
			"Usage-only chunk should propagate token usage into StreamingOutput");
	}
}
