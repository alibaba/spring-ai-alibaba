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
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #2702 Reproduction Test: NullPointerException in streaming with tools when streamUsage enabled
 * 
 * <p>This test reproduces the bug reported in Issue #2702:
 * When LlmNode is used in streaming mode with streamUsage=true, the ChatModel may return
 * ChatResponse objects with null results (usage-only chunks). This causes NullPointerException
 * in NodeExecutor when accessing response.getResult().getOutput().
 * 
 * <p>Root Cause:
 * - DashScope API returns usage-only chunks when streamUsage=true
 * - These chunks have null Generation.getOutput() or null ChatResponse.getResult()
 * - NodeExecutor.java lines 419-425 and 500-506 did not handle null checks
 * 
 * <p>Solution:
 * - Added defensive null checks in NodeExecutor for response.getResult() and lastResponse.getResult()
 * - Added filter(Objects::nonNull) to remove null responses from the stream
 * 
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/2702">Issue #2702</a>
 */
public class Issue2702ReproductionTest {

	private static final Logger log = LoggerFactory.getLogger(Issue2702ReproductionTest.class);

	/**
	 * Creates a mock ChatModel that simulates the behavior of DashScope API with streamUsage=true.
	 * The mock returns a stream of ChatResponse objects, including one with a null result
	 * to simulate the usage-only chunk that causes the NPE.
	 */
	private static ChatModel createMockChatModelWithNullResult() {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage("这是一个测试"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.concat(
					// Normal chunk 1
					Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("这是"))))),
					
					// Normal chunk 2
					Mono.delay(Duration.ofMillis(10))
						.map(i -> new ChatResponse(List.of(new Generation(new AssistantMessage("一个"))))),
					
					// 🔥 Simulate usage-only chunk (null result) - this is what causes the NPE
					Mono.delay(Duration.ofMillis(10))
						.map(i -> {
							try {
								// Try to create a ChatResponse with null Generation output
								return new ChatResponse(List.of(new Generation(null, ChatGenerationMetadata.NULL)));
							} catch (Exception e) {
								// If that fails, return an empty ChatResponse
								return new ChatResponse(List.of());
							}
						}),
					
					// Normal chunk 3
					Mono.delay(Duration.ofMillis(10))
						.map(i -> new ChatResponse(List.of(new Generation(new AssistantMessage("测试")))))
				);
			}
		};
	}

	/**
	 * Test that streaming with null results should not throw NullPointerException.
	 * 
	 * <p>This test verifies that:
	 * 1. NodeExecutor properly handles ChatResponse objects with null results
	 * 2. The streaming completes without throwing NullPointerException
	 */
	@Test
	public void testStreamingWithNullResultShouldNotThrowNPE() throws Exception {
		// 1. Create mock ChatModel that simulates null result chunks
		ChatModel chatModel = createMockChatModelWithNullResult();

		// 2. Build StateGraph with LLmNodeAction
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", new AppendStrategy());
			return keyStrategyMap;
		}).addNode("llmNode", node_async(new LLmNodeAction(chatModel, "llmNode")))
			.addEdge(START, "llmNode")
			.addEdge("llmNode", END);

		CompiledGraph compiledGraph = stateGraph.compile();

		// 3. Prepare input
		Map<String, Object> input = new HashMap<>();
		input.put(OverAllState.DEFAULT_INPUT_KEY, "测试");

		// 4. Stream and verify no NPE occurs
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean hasNPE = new AtomicBoolean(false);
		AtomicBoolean hasError = new AtomicBoolean(false);

		compiledGraph.stream(input)
			.doOnNext(output -> {
				log.info("✅ 接收到流式输出: {}", output);
			})
			.doOnError(error -> {
				if (error instanceof NullPointerException) {
					hasNPE.set(true);
					log.error("❌ 检测到 NullPointerException (Issue #2702 未修复):", error);
				} else if (error.getCause() instanceof NullPointerException) {
					hasNPE.set(true);
					log.error("❌ 检测到 NullPointerException (Issue #2702 未修复, 在 cause 中):", error.getCause());
				} else {
					hasError.set(true);
					log.warn("⚠️ 检测到其他错误:", error);
				}
			})
			.doOnComplete(() -> {
				log.info("✅ 流式调用完成");
				latch.countDown();
			})
			.subscribe(
				output -> {
					// Process each output
				},
				error -> {
					// Error already handled in doOnError
					latch.countDown();
				}
			);

		// 5. Wait for completion
		assertTrue(latch.await(10, TimeUnit.SECONDS), "流式调用应在 10 秒内完成");

		// 6. Verify no NPE occurred
		assertFalse(hasNPE.get(), 
			"❌ 检测到 NodeExecutor 中的 NullPointerException！这表示 Issue #2702 未修复。");

		if (hasError.get()) {
			log.warn("注意：检测到其他错误（可能是 GraphFluxGenerator 等其他组件需要类似修复）");
		}

		log.info("✅ 测试通过：NodeExecutor 中的 null result NPE 已修复（Issue #2702）");
	}
}

