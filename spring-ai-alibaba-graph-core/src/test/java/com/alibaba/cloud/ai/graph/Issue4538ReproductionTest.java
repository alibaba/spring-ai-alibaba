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

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #4538 Reproduction Test: embedded graphResponseStream fails when
 * releaseThread=true and CheckpointSaver returns BaseCheckpointSaver.Tag
 *
 * <p>This test reproduces the bug reported in Issue #4538:
 * when an outer graph node returns an embedded {@code graphResponseStream()},
 * and the inner graph is compiled with {@code releaseThread(true)} plus a
 * {@code CheckpointSaver}, the inner completion emits
 * {@code GraphResponse.done(BaseCheckpointSaver.Tag)}.
 * The outer graph's {@code NodeExecutor#processGraphResponseFlux()} assumes
 * the final done value must be a {@code Map<String, Object>} and emits
 * {@code IllegalArgumentException}.
 *
 * <p>Root Cause:
 * <ul>
 *   <li>{@code BaseGraphExecutor#handleCompletion()} may legally emit done(Tag)</li>
 *   <li>{@code NodeExecutor#processGraphResponseFlux()} only accepts done(Map)</li>
 *   <li>When an embedded graphResponseStream is consumed by an outer graph, these
 *   two internal contracts conflict</li>
 * </ul>
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4538">Issue #4538</a>
 */
public class Issue4538ReproductionTest {

	private static final Logger log = LoggerFactory.getLogger(Issue4538ReproductionTest.class);

	private static final String INNER_THREAD_ID = "issue-4538-inner-thread";

	private static final String OUTER_THREAD_ID = "issue-4538-outer-thread";

	private static KeyStrategyFactory createKeyStrategyFactory() {
		return new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE).build();
	}

	/**
	 * Creates the inner compiled graph that will emit BaseCheckpointSaver.Tag on completion.
	 */
	private static CompiledGraph createInnerCompiledGraph(
			KeyStrategyFactory keyStrategyFactory, MemorySaver saver) throws Exception {
		StateGraph innerGraph = new StateGraph(keyStrategyFactory)
			.addNode("inner", node_async(state -> Map.of("payload", "inner-ok")))
			.addEdge(START, "inner")
			.addEdge("inner", END);

		return innerGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.releaseThread(true)
			.build());
	}

	/**
	 * Creates the outer compiled graph that consumes the inner graphResponseStream as embedded Flux.
	 */
	private static CompiledGraph createOuterCompiledGraph(
			KeyStrategyFactory keyStrategyFactory, CompiledGraph innerCompiledGraph) throws Exception {
		StateGraph outerGraph = new StateGraph(keyStrategyFactory)
			.addNode("outer", AsyncNodeActionWithConfig.node_async((state, config) -> {
				var innerConfig = RunnableConfig.builder().threadId(INNER_THREAD_ID).build();
				return Map.of("embedded", innerCompiledGraph.graphResponseStream(Map.of(), innerConfig));
			}))
			.addEdge(START, "outer")
			.addEdge("outer", END);

		return outerGraph.compile();
	}

	/**
	 * Collects root-cause error messages from GraphResponse.error entries.
	 */
	private static List<String> collectErrorMessages(List<GraphResponse<NodeOutput>> responses) {
		List<String> errorMessages = new ArrayList<>();

		for (GraphResponse<NodeOutput> response : responses) {
			if (!response.isError()) {
				continue;
			}
			try {
				response.getOutput().join();
			}
			catch (Throwable throwable) {
				Throwable root = throwable;
				while (root.getCause() != null) {
					root = root.getCause();
				}
				errorMessages.add(root.getClass().getSimpleName() + ": " + root.getMessage());
			}
		}

		return errorMessages;
	}

	private static void logGraphResponses(String prefix, List<GraphResponse<NodeOutput>> responses) {
		for (GraphResponse<NodeOutput> response : responses) {
			if (response.isError()) {
				try {
					response.getOutput().join();
				}
				catch (Throwable throwable) {
					Throwable root = throwable;
					while (root.getCause() != null) {
						root = root.getCause();
					}
					log.error("{} ❌ 检测到图错误: {}", prefix, root.toString());
				}
				continue;
			}

			if (response.getOutput() != null && !response.getOutput().isCompletedExceptionally()) {
				NodeOutput output = response.getOutput().join();
				log.info("{} ✅ 接收到流式输出: {}", prefix, output);
			}

			if (response.resultValue().isPresent()) {
				log.info("{} ℹ️ 接收到完成结果: {}", prefix, response.resultValue().get());
			}
		}
	}

	/**
	 * Test that an embedded graphResponseStream with auto-release should not emit type errors.
	 *
	 * <p>This test verifies that:
	 * <ol>
	 *   <li>The inner graph really emits {@code BaseCheckpointSaver.Tag} when {@code releaseThread=true}</li>
	 *   <li>The outer graph consumes that embedded stream</li>
	 *   <li>No {@code IllegalArgumentException} is emitted as a GraphResponse.error</li>
	 * </ol>
	 */
	@Test
	public void testEmbeddedGraphResponseStreamWithAutoReleaseShouldNotEmitTypeError() throws Exception {
		// 1. Create minimal saver and graph configuration
		KeyStrategyFactory keyStrategyFactory = createKeyStrategyFactory();
		MemorySaver saver = MemorySaver.builder().build();

		// 2. Build inner and outer graphs
		CompiledGraph innerCompiledGraph = createInnerCompiledGraph(keyStrategyFactory, saver);
		CompiledGraph outerCompiledGraph = createOuterCompiledGraph(keyStrategyFactory, innerCompiledGraph);

		// 3. Execute inner graph directly and verify it really emits BaseCheckpointSaver.Tag
		List<GraphResponse<NodeOutput>> innerResponses = assertDoesNotThrow(
			() -> innerCompiledGraph.graphResponseStream(
				Map.of(),
				RunnableConfig.builder().threadId(INNER_THREAD_ID).build())
				.collectList()
				.block());

		assertNotNull(innerResponses);
		logGraphResponses("[inner]", innerResponses);
		assertTrue(innerResponses.stream()
			.anyMatch(response -> response.resultValue().isPresent()
				&& response.resultValue().get() instanceof BaseCheckpointSaver.Tag),
			"Inner graph should emit BaseCheckpointSaver.Tag when releaseThread=true");

		// 4. Execute outer graph that embeds the inner graphResponseStream
		List<GraphResponse<NodeOutput>> outerResponses = assertDoesNotThrow(
			() -> outerCompiledGraph.graphResponseStream(
				Map.of(),
				RunnableConfig.builder().threadId(OUTER_THREAD_ID).build())
				.collectList()
				.block());

		assertNotNull(outerResponses);
		assertFalse(outerResponses.isEmpty());
		logGraphResponses("[outer]", outerResponses);

		// 5. Collect any wrapped GraphResponse.error failures
		List<String> errorMessages = collectErrorMessages(outerResponses);
		if (!errorMessages.isEmpty()) {
			log.error("Detected wrapped graph errors for Issue #4538 reproduction: {}", errorMessages);
		}

		// 6. Verify no type mismatch error is emitted
		assertTrue(errorMessages.isEmpty(), "Unexpected graph errors: " + errorMessages);
		log.info("✅ 测试通过：embedded graphResponseStream 在 releaseThread + CheckpointSaver 场景下不再触发 Tag/Map 类型冲突（Issue #4538）");
	}

}
