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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4639">#4639</a> regression for
 * {@link SubCompiledGraphNodeAction} ({@link StateGraph#addNode(String, CompiledGraph)}).
 */
class SubCompiledGraphNodeResumeCheckpointTest {

	@Nested
	class ResumeCheckpointRegression {

		private static final String AGENT_NAME = "qa_agent";

		@Test
		void parentInterruptBeforeSubCompiledNode_resumeWithParentThreadId_succeeds() throws Exception {
			MemorySaver saver = MemorySaver.builder().build();
			AtomicInteger childNodeRuns = new AtomicInteger();

			CompiledGraph childGraph = buildChildGraph(saver, childNodeRuns);
			CompiledGraph parentGraph = buildParentGraph(childGraph, saver);

			String threadId = "conv-subcompiled-" + System.nanoTime();
			RunnableConfig invokeConfig = RunnableConfig.builder().threadId(threadId).build();

			AtomicReference<NodeOutput> last = new AtomicReference<>();
			parentGraph.stream(Map.of("input", "x"), invokeConfig).doOnNext(last::set).blockLast();

			assertInstanceOf(InterruptionMetadata.class, last.get(), "应在进入子图节点前中断");
			assertEquals(0, childNodeRuns.get(), "首次中断前子图节点不应执行");

			RunnableConfig resumeConfig = RunnableConfig.builder().threadId(threadId).resume().build();

			assertFalse(throwsCheckpointFailure(parentGraph, resumeConfig),
					"父 threadId resume 不应因子 namespace 无 checkpoint 失败");
			assertTrue(childNodeRuns.get() >= 1, "resume 后应冷启动子图并执行子节点");
		}

		private static CompiledGraph buildChildGraph(MemorySaver saver, AtomicInteger childNodeRuns)
				throws Exception {
			KeyStrategyFactory keyStrategyFactory = () -> {
				Map<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("input", new ReplaceStrategy());
				strategies.put("qa_result", new ReplaceStrategy());
				return strategies;
			};

			return new StateGraph(keyStrategyFactory)
					.addNode("child_work", node_async(state -> {
						childNodeRuns.incrementAndGet();
						return Map.of("qa_result", "ok");
					}))
					.addEdge(START, "child_work")
					.addEdge("child_work", END)
					.compile(CompileConfig.builder()
							.saverConfig(SaverConfig.builder().register(saver).build())
							.build());
		}

		private static CompiledGraph buildParentGraph(CompiledGraph childGraph, MemorySaver saver)
				throws Exception {
			KeyStrategyFactory keyStrategyFactory = () -> {
				Map<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put("input", new ReplaceStrategy());
				strategies.put("prep_marker", new ReplaceStrategy());
				strategies.put(ResumableSubGraphAction.outputKeyToParent(AGENT_NAME), new ReplaceStrategy());
				return strategies;
			};

			StateGraph workflow = new StateGraph(keyStrategyFactory)
					.addNode("prep", node_async(state -> Map.of("prep_marker", "ok")))
					.addNode(AGENT_NAME, childGraph)
					.addEdge(START, "prep")
					.addEdge("prep", AGENT_NAME)
					.addEdge(AGENT_NAME, END);

			return workflow.compile(CompileConfig.builder()
					.saverConfig(SaverConfig.builder().register(saver).build())
					.interruptBefore(AGENT_NAME)
					.build());
		}

		private static boolean throwsCheckpointFailure(CompiledGraph graph, RunnableConfig resumeConfig) {
			try {
				graph.stream(null, resumeConfig).blockLast();
				return false;
			}
			catch (Exception ex) {
				Throwable root = ex;
				while (root.getCause() != null && root.getCause() != root) {
					root = root.getCause();
				}
				String message = root.getMessage();
				return message != null
						&& (message.contains("valid checkpoint") || message.contains("Missing Checkpoint"));
			}
		}

	}

}
