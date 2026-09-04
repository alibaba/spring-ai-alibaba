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
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4639">#4639</a> regression for
 * {@link SubCompiledGraphNodeAction} ({@link StateGraph#addNode(String, CompiledGraph)}).
 */
class SubCompiledGraphNodeResumeCheckpointTest {

	private static final String AGENT_NAME = "qa_agent";

	@Test
	void parentInterruptBeforeSubCompiledNode_resumeWithParentThreadId_succeeds() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		AtomicInteger childNodeRuns = new AtomicInteger();

		CompiledGraph childGraph = buildChildGraph(saver, childNodeRuns);
		CompiledGraph parentGraph = buildParentGraph(childGraph, saver, true);

		String threadId = "conv-subcompiled-" + System.nanoTime();
		RunnableConfig invokeConfig = RunnableConfig.builder().threadId(threadId).build();

		AtomicReference<NodeOutput> last = new AtomicReference<>();
		parentGraph.stream(Map.of("input", "x"), invokeConfig).doOnNext(last::set).blockLast();

		assertInstanceOf(InterruptionMetadata.class, last.get(), "应在进入子图节点前中断");
		assertEquals(0, childNodeRuns.get(), "首次中断前子图节点不应执行");

		RunnableConfig resumeConfig = RunnableConfig.builder().threadId(threadId).resume().build();
		AtomicReference<NodeOutput> resumeLast = new AtomicReference<>();
		parentGraph.stream(null, resumeConfig).doOnNext(resumeLast::set).blockLast();

		assertTrue(childNodeRuns.get() >= 1, "resume 后应冷启动子图并执行子节点");
		OverAllState state = resumeLast.get().state();
		assertEquals("ok", state.value("prep_marker").orElse(null), "父终态应保留 prep 结果");
		assertEquals("ok", state.value("qa_result").orElse(null), "父终态应包含子图输出");
	}

	@Test
	void reusedThreadColdStart_withStaleChildCheckpoint_rerunsChildFromStart() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		AtomicInteger childNodeRuns = new AtomicInteger();

		CompiledGraph childGraph = buildChildGraph(saver, childNodeRuns);
		CompiledGraph parentGraph = buildParentGraph(childGraph, saver, false);

		String threadId = "conv-reuse-" + System.nanoTime();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		parentGraph.stream(Map.of("input", "first"), config).blockLast();
		assertEquals(1, childNodeRuns.get());

		// Same threadId, no resume(): strategy A must not continue from child CP
		parentGraph.stream(Map.of("input", "second"), config).blockLast();
		assertEquals(2, childNodeRuns.get(), "冷启动应再次从 START 跑子节点，而非接着旧 CP");
	}

	@Test
	void subCompiled_resumeWithChildCheckpoint_forwardsInterruptionMetadata() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		AtomicReference<Object> feedbackSeen = new AtomicReference<>();

		KeyStrategyFactory childKeys = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			return strategies;
		};
		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build();
		CompiledGraph childGraph = new StateGraph(childKeys)
				.addNode("child_work", AsyncNodeActionWithConfig.node_async((state, config) -> {
					feedbackSeen.set(config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElse(null));
					return Map.of("qa_result", "ok");
				}))
				.addEdge(START, "child_work")
				.addEdge("child_work", END)
				.compile(compileConfig);

		String threadId = "conv-hitl-" + System.nanoTime();
		RunnableConfig prepared = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(
				RunnableConfig.builder().threadId(threadId).build(), AGENT_NAME, compileConfig, compileConfig);
		saver.put(prepared, Checkpoint.builder()
				.id(UUID.randomUUID().toString())
				.nodeId(START)
				.nextNodeId("child_work")
				.state(Map.of("qa_result", "paused"))
				.build());

		InterruptionMetadata feedback = InterruptionMetadata.builder(AGENT_NAME, OverAllStateBuilder.builder().build())
				.build();
		RunnableConfig parentConfig = RunnableConfig.builder()
				.threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
				.addMetadata(resumeSubGraphId(AGENT_NAME), true)
				.build();

		SubCompiledGraphNodeAction action = new SubCompiledGraphNodeAction(AGENT_NAME, compileConfig, childGraph);
		OverAllState state = OverAllStateBuilder.builder()
				.withData(Map.of("input", "x"))
				.withKeyStrategies(childKeys.apply())
				.build();
		Map<String, Object> result = action.apply(state, parentConfig).join();
		@SuppressWarnings("unchecked")
		Flux<GraphResponse<NodeOutput>> flux = (Flux<GraphResponse<NodeOutput>>) result
				.get(outputKeyToParent(AGENT_NAME));
		flux.blockLast();

		assertInstanceOf(InterruptionMetadata.class, feedbackSeen.get(), "子图应收到真实 HITL 反馈");
	}

	@Test
	void subCompiled_interruptionMetadataWithoutChildCheckpoint_doesNotForward() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		AtomicReference<Object> feedbackSeen = new AtomicReference<>("unset");

		KeyStrategyFactory childKeys = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			return strategies;
		};
		CompileConfig compileConfig = CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build();
		CompiledGraph childGraph = new StateGraph(childKeys)
				.addNode("child_work", AsyncNodeActionWithConfig.node_async((state, config) -> {
					feedbackSeen.set(config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElse(null));
					return Map.of("qa_result", "ok");
				}))
				.addEdge(START, "child_work")
				.addEdge("child_work", END)
				.compile(compileConfig);

		InterruptionMetadata feedback = InterruptionMetadata.builder(AGENT_NAME, OverAllStateBuilder.builder().build())
				.build();
		RunnableConfig parentConfig = RunnableConfig.builder()
				.threadId("conv-cold-hitl-" + System.nanoTime())
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
				.build();

		SubCompiledGraphNodeAction action = new SubCompiledGraphNodeAction(AGENT_NAME, compileConfig, childGraph);
		OverAllState state = OverAllStateBuilder.builder()
				.withData(Map.of("input", "x"))
				.withKeyStrategies(childKeys.apply())
				.build();
		Map<String, Object> result = action.apply(state, parentConfig).join();
		@SuppressWarnings("unchecked")
		Flux<GraphResponse<NodeOutput>> flux = (Flux<GraphResponse<NodeOutput>>) result
				.get(outputKeyToParent(AGENT_NAME));
		flux.blockLast();

		assertNull(feedbackSeen.get(), "冷子图不应写回 InterruptionMetadata");
	}

	private static CompiledGraph buildChildGraph(MemorySaver saver, AtomicInteger childNodeRuns) throws Exception {
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

	private static CompiledGraph buildParentGraph(CompiledGraph childGraph, MemorySaver saver, boolean interruptBefore)
			throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("prep_marker", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			strategies.put(ResumableSubGraphAction.outputKeyToParent(AGENT_NAME), new ReplaceStrategy());
			return strategies;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
				.addNode("prep", node_async(state -> Map.of("prep_marker", "ok")))
				.addNode(AGENT_NAME, childGraph)
				.addEdge(START, "prep")
				.addEdge("prep", AGENT_NAME)
				.addEdge(AGENT_NAME, END);

		CompileConfig.Builder compileBuilder = CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build());
		if (interruptBefore) {
			compileBuilder.interruptBefore(AGENT_NAME);
		}
		return workflow.compile(compileBuilder.build());
	}

}
