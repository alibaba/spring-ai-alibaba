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
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for {@link SubGraphRunnableConfigBridge} gate / namespace / HITL rules.
 */
class SubGraphRunnableConfigBridgeTest {

	private static final String NODE_ID = "qa_agent";

	@Test
	void prepare_stripsResumeMarkersImmediately() {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		RunnableConfig parent = RunnableConfig.builder()
				.threadId("t1")
				.resume()
				.addMetadata(resumeSubGraphId(NODE_ID), true)
				.build();

		RunnableConfig child = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID, compileConfig,
				compileConfig);

		assertTrue(child.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isEmpty());
		assertTrue(child.metadata(resumeSubGraphId(NODE_ID)).isEmpty());
		assertEquals("t1_" + subGraphId(NODE_ID), child.threadId().orElseThrow());
	}

	@Test
	void prepare_preservesParentContextCopy() {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		RunnableConfig parent = RunnableConfig.builder().threadId("t1").build();
		parent.context().put("request-id", "context-value");

		RunnableConfig child = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID, compileConfig,
				compileConfig);

		assertEquals("context-value", child.context().get("request-id"));
		child.context().put("child-only", "x");
		assertFalse(parent.context().containsKey("child-only"), "child writes must not leak to parent context");
	}

	@Test
	void prepare_forceNamespace_whenSaversDiffer() {
		MemorySaver parentSaver = MemorySaver.builder().build();
		MemorySaver childSaver = MemorySaver.builder().build();
		CompileConfig parentCompile = compileConfig(parentSaver);
		CompileConfig childCompile = compileConfig(childSaver);
		RunnableConfig parent = RunnableConfig.builder().threadId("conv").build();

		RunnableConfig withoutForce = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				subGraphId(NODE_ID), parentCompile, childCompile, false);
		assertEquals("conv", withoutForce.threadId().orElseThrow());

		RunnableConfig withForce = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				subGraphId(NODE_ID), parentCompile, childCompile, true);
		assertEquals("conv_" + subGraphId(NODE_ID), withForce.threadId().orElseThrow());
	}

	@Test
	void prepare_forceNamespace_parentWithoutSaver_childWithSaver_succeeds() {
		CompileConfig parentWithoutSaver = compileConfigWithoutSaver();
		CompileConfig childWithSaver = compileConfig(MemorySaver.builder().build());
		RunnableConfig parent = RunnableConfig.builder().threadId("conv-no-parent-saver").resume().build();

		RunnableConfig child = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				subGraphId(NODE_ID), parentWithoutSaver, childWithSaver, true);

		assertEquals("conv-no-parent-saver_" + subGraphId(NODE_ID), child.threadId().orElseThrow());
		assertTrue(child.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isEmpty());
	}

	@Test
	void prepare_forceNamespace_neitherHasSaver_stillNamespaces() {
		CompileConfig noSaver = compileConfigWithoutSaver();
		RunnableConfig parent = RunnableConfig.builder().threadId("conv-none").build();

		RunnableConfig child = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				subGraphId(NODE_ID), noSaver, noSaver, true);

		assertEquals("conv-none_" + subGraphId(NODE_ID), child.threadId().orElseThrow());
	}

	@Test
	void prepare_withoutForceNamespace_parentWithoutSaver_childWithSaver_throws() {
		CompileConfig parentWithoutSaver = compileConfigWithoutSaver();
		CompileConfig childWithSaver = compileConfig(MemorySaver.builder().build());
		RunnableConfig parent = RunnableConfig.builder().threadId("conv").build();

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID, subGraphId(NODE_ID),
						parentWithoutSaver, childWithSaver, false));
		assertTrue(ex.getMessage().contains("Missing CheckpointSaver"));
	}

	@Test
	void prepare_callerThreadIdEndingWithSuffix_stillNamespaces() {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		String namespace = subGraphId(NODE_ID);
		// Caller-controlled id that already ends with the suffix must still be namespaced
		RunnableConfig parent = RunnableConfig.builder().threadId("x_" + namespace).build();

		RunnableConfig child = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID, compileConfig,
				compileConfig);

		assertEquals("x_" + namespace + "_" + namespace, child.threadId().orElseThrow());
	}

	@Test
	void prepare_sameNamespaceKey_isIdempotentViaProvenance() {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		RunnableConfig parent = RunnableConfig.builder().threadId("t1").build();

		RunnableConfig once = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID, compileConfig,
				compileConfig);
		RunnableConfig twice = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(once, NODE_ID, compileConfig,
				compileConfig);

		assertEquals(once.threadId().orElseThrow(), twice.threadId().orElseThrow());
	}

	@Test
	void prepare_nestedDifferentNamespaceKeys_append() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		RunnableConfig parent = RunnableConfig.builder().threadId("t1").build();

		RunnableConfig outer = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, "outer",
				subGraphId("outer"), compileConfig, compileConfig, true);
		RunnableConfig inner = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(outer, "inner",
				subGraphId("inner"), compileConfig, compileConfig, true);

		assertEquals("t1_subgraph_outer_subgraph_inner", inner.threadId().orElseThrow());
	}

	@Test
	void resolve_coldParent_withStaleChildCheckpoint_doesNotUpdateState() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		CompiledGraph childGraph = buildChildGraph(saver);

		String parentThread = "conv-stale-" + System.nanoTime();
		RunnableConfig parent = RunnableConfig.builder().threadId(parentThread).build();
		RunnableConfig prepared = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				compileConfig, compileConfig);

		saver.put(prepared, Checkpoint.builder()
				.id(UUID.randomUUID().toString())
				.nodeId("child_work")
				.nextNodeId(END)
				.state(Map.of("qa_result", "stale"))
				.build());

		RunnableConfig resolved = SubGraphRunnableConfigBridge.resolveForCompiledChildResume(Map.of("input", "x"),
				childGraph, prepared, parent, NODE_ID);

		assertTrue(resolved.checkPointId().isEmpty(), "cold parent must not resume stale child CP");
		assertTrue(resolved.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isEmpty());
	}

	@Test
	void resolve_parentResume_withChildCheckpoint_updatesAndForwardsInterruptionMetadata() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		CompiledGraph childGraph = buildChildGraph(saver);

		String parentThread = "conv-hitl-" + System.nanoTime();
		InterruptionMetadata feedback = InterruptionMetadata.builder(NODE_ID, OverAllStateBuilder.builder().build())
				.build();
		RunnableConfig parent = RunnableConfig.builder()
				.threadId(parentThread)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
				.addMetadata(resumeSubGraphId(NODE_ID), true)
				.build();
		RunnableConfig prepared = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				compileConfig, compileConfig);

		saver.put(prepared, Checkpoint.builder()
				.id(UUID.randomUUID().toString())
				.nodeId("child_work")
				.nextNodeId(END)
				.state(Map.of("qa_result", "paused"))
				.build());

		RunnableConfig resolved = SubGraphRunnableConfigBridge.resolveForCompiledChildResume(Map.of("input", "x"),
				childGraph, prepared, parent, NODE_ID);

		assertTrue(resolved.checkPointId().isPresent(), "parent resume + child CP should updateState");
		assertInstanceOf(InterruptionMetadata.class,
				resolved.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow());
	}

	@Test
	void resolve_parentInterruptionMetadata_withoutChildCheckpoint_doesNotForwardFeedback() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompileConfig compileConfig = compileConfig(saver);
		CompiledGraph childGraph = buildChildGraph(saver);

		InterruptionMetadata feedback = InterruptionMetadata.builder(NODE_ID, OverAllStateBuilder.builder().build())
				.build();
		RunnableConfig parent = RunnableConfig.builder()
				.threadId("conv-cold-hitl-" + System.nanoTime())
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedback)
				.build();
		RunnableConfig prepared = SubGraphRunnableConfigBridge.prepareChildRunnableConfig(parent, NODE_ID,
				compileConfig, compileConfig);

		RunnableConfig resolved = SubGraphRunnableConfigBridge.resolveForCompiledChildResume(Map.of("input", "x"),
				childGraph, prepared, parent, NODE_ID);

		assertTrue(resolved.checkPointId().isEmpty());
		assertTrue(resolved.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isEmpty(),
				"cold child must not receive InterruptionMetadata");
	}

	@Test
	void hasParentResumeIntent_acceptsHumanFeedbackOrResumeSubgraphFlag() {
		assertFalse(SubGraphRunnableConfigBridge.hasParentResumeIntent(RunnableConfig.builder().build(), NODE_ID));
		assertTrue(SubGraphRunnableConfigBridge.hasParentResumeIntent(
				RunnableConfig.builder().resume().build(), NODE_ID));
		assertTrue(SubGraphRunnableConfigBridge.hasParentResumeIntent(
				RunnableConfig.builder().addMetadata(resumeSubGraphId(NODE_ID), true).build(), NODE_ID));
	}

	private static CompileConfig compileConfig(MemorySaver saver) {
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build()).build();
	}

	/** Explicit empty SaverConfig — {@code CompileConfig.builder().build()} still defaults to MemorySaver. */
	private static CompileConfig compileConfigWithoutSaver() {
		return CompileConfig.builder().saverConfig(new SaverConfig()).build();
	}

	private static CompiledGraph buildChildGraph(MemorySaver saver) throws Exception {
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("input", new ReplaceStrategy());
			strategies.put("qa_result", new ReplaceStrategy());
			return strategies;
		};
		return new StateGraph(keyStrategyFactory)
				.addNode("child_work", node_async(state -> Map.of("qa_result", "ok")))
				.addEdge(START, "child_work")
				.addEdge("child_work", END)
				.compile(compileConfig(saver));
	}

}
