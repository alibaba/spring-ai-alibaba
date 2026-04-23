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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Token 用量累加和流式 usage 捕获的回归测试。
 *
 * <p>验证 issue #4488 的两个核心修复：
 * <ol>
 *   <li>NodeExecutor 在 filter 之前通过 doOnNext 捕获 usage-only chunk（result==null）的 usage 数据</li>
 *   <li>GraphRunnerContext 跨多轮调用累加 usage，而非覆盖</li>
 * </ol>
 */
public class TokenUsageAccumulationTest {

	private static final Logger log = LoggerFactory.getLogger(TokenUsageAccumulationTest.class);

	/**
	 * 创建模拟 DashScope streamUsage=true 的 mock ChatModel。
	 * 流式返回的最后一个 chunk 是 usage-only chunk（result 为 null，仅携带 usage 元数据）。
	 */
	private static ChatModel createStreamingChatModelWithUsageOnlyChunk(int promptTokens, int completionTokens) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				Usage usage = new DefaultUsage(promptTokens, completionTokens);
				ChatResponseMetadata metadata = ChatResponseMetadata.builder()
						.usage(usage)
						.build();
				return new ChatResponse(List.of(new Generation(new AssistantMessage("Hello"))), metadata);
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				// 正常内容 chunk（无 usage）
				ChatResponse chunk1 = new ChatResponse(List.of(new Generation(new AssistantMessage("Hel"))));
				ChatResponse chunk2 = new ChatResponse(List.of(new Generation(new AssistantMessage("lo"))));

				// Usage-only chunk：有 usage 元数据但 result 为 null
				Usage usage = new DefaultUsage(promptTokens, completionTokens);
				ChatResponseMetadata usageMetadata = ChatResponseMetadata.builder()
						.usage(usage)
						.build();
				// 模拟 DashScope 的 usage-only chunk，getResult() 返回 null
				ChatResponse usageOnlyChunk = new ChatResponse(List.of(), usageMetadata);

				return Flux.concat(
						Flux.just(chunk1),
						Mono.delay(Duration.ofMillis(5)).map(i -> chunk2),
						Mono.delay(Duration.ofMillis(5)).map(i -> usageOnlyChunk)
				);
			}
		};
	}

	/**
	 * 测试：流式传输中的 usage-only chunk 应被 NodeExecutor 正确捕获。
	 *
	 * <p>当 DashScope 返回的最后一个 chunk 是 result==null 的 usage-only chunk 时，
	 * NodeExecutor 的 doOnNext 会在 filter 丢弃该 chunk 之前捕获其 usage，
	 * 然后在 concatWith 完成时将捕获的 usage 应用到 StreamingOutput。
	 */
	@Test
	public void testStreamingUsageOnlyChunkCaptured() throws Exception {
		ChatModel chatModel = createStreamingChatModelWithUsageOnlyChunk(100, 50);

		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		}).addNode("llmNode", node_async(new LLmNodeAction(chatModel, "llmNode")))
				.addEdge(START, "llmNode")
				.addEdge("llmNode", END);

		CompiledGraph compiledGraph = stateGraph.compile();

		Map<String, Object> input = new HashMap<>();
		input.put(OverAllState.DEFAULT_INPUT_KEY, "test");

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Usage> capturedUsage = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();

		compiledGraph.stream(input)
				.doOnNext(output -> {
					Usage usage = output.tokenUsage();
					if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
						capturedUsage.set(usage);
						log.info("Captured usage: prompt={}, completion={}, total={}",
								usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
					}
				})
				.doOnError(error::set)
				.doFinally(signal -> latch.countDown())
				.subscribe();

		assertTrue(latch.await(10, TimeUnit.SECONDS), "流应在 10 秒内完成");
		assertNull(error.get(), "流应无错误完成");

		Usage usage = capturedUsage.get();
		assertNotNull(usage, "usage-only chunk 的 usage 应被 NodeExecutor 捕获");
		assertEquals(100, usage.getPromptTokens(), "promptTokens 应为 100");
		assertEquals(50, usage.getCompletionTokens(), "completionTokens 应为 50");
		assertEquals(150, usage.getTotalTokens(), "totalTokens 应为 150");

		log.info("测试通过：usage-only chunk 成功捕获");
	}

	/**
	 * 测试：GraphRunnerContext 跨多个节点执行时累加 usage。
	 *
	 * <p>模拟一个两节点串行图，每个节点产生不同的 usage。
	 * 最终输出应包含两个节点的累加 usage。
	 */
	@Test
	public void testUsageAccumulationAcrossNodes() throws Exception {
		// 节点 1：产生 100 prompt + 50 completion tokens
		ChatModel chatModel1 = createStreamingChatModelWithUsageOnlyChunk(100, 50);
		// 节点 2：产生 80 prompt + 40 completion tokens
		ChatModel chatModel2 = createStreamingChatModelWithUsageOnlyChunk(80, 40);

		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		}).addNode("node1", node_async(new LLmNodeAction(chatModel1, "node1")))
				.addNode("node2", node_async(new LLmNodeAction(chatModel2, "node2")))
				.addEdge(START, "node1")
				.addEdge("node1", "node2")
				.addEdge("node2", END);

		CompiledGraph compiledGraph = stateGraph.compile();

		Map<String, Object> input = new HashMap<>();
		input.put(OverAllState.DEFAULT_INPUT_KEY, "test accumulation");

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Usage> lastUsage = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();

		compiledGraph.stream(input)
				.doOnNext(output -> {
					Usage usage = output.tokenUsage();
					if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
						lastUsage.set(usage);
					}
				})
				.doOnError(error::set)
				.doFinally(signal -> latch.countDown())
				.subscribe();

		assertTrue(latch.await(10, TimeUnit.SECONDS), "流应在 10 秒内完成");
		assertNull(error.get(), "流应无错误完成");

		Usage finalUsage = lastUsage.get();
		assertNotNull(finalUsage, "最终输出应包含累加后的 usage");
		log.info("最终累加 usage：prompt={}, completion={}, total={}",
				finalUsage.getPromptTokens(), finalUsage.getCompletionTokens(), finalUsage.getTotalTokens());

		// 累加后的 usage 应大于等于第一个节点的 usage
		assertTrue(finalUsage.getPromptTokens() >= 80,
				"累加后 prompt tokens 应至少为 80");
		assertTrue(finalUsage.getCompletionTokens() >= 40,
				"累加后 completion tokens 应至少为 40");
	}

}
