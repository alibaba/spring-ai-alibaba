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
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InterruptionTest {

	private AsyncNodeActionWithConfig _nodeAction(String id) {
		return node_async((state, config) -> Map.of("messages", id));
	}

	@Test
	public void interruptAfterEdgeEvaluation() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();
		var workflow = new StateGraph(keyStrategyFactory).addNode("A", _nodeAction("A"))
			.addNode("B", _nodeAction("B"))
			.addNode("C", _nodeAction("C"))
			.addNode("D", _nodeAction("D"))
			.addConditionalEdges("B", edge_async(state -> {
				var message = state.value("messages").orElse(END);
				return message.equals("B") ? "D" : message.toString();
			}), EdgeMappings.builder().to("A").to("C").to("D").toEND().build())
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("C", END)
			.addEdge("D", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptAfter("B")
				.build());

		var runnableConfig = RunnableConfig.builder().build();

		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();
		var results = workflow.stream(Map.of(), runnableConfig)
			.doOnNext( output -> {
				System.out.println(output);
				lastOutputRef.set(output);
			})
			.map(NodeOutput::node)
			.collectList()
			.block();

		//The last 'B' node is duplicated because an InterruptionMetadata NodeOutput is emitted after the edge evaluation of node 'B'.
		assertIterableEquals(List.of(START, "A", "B", "B"), results);
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());


		RunnableConfig resumeConfig = RunnableConfig.builder()
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("D", END), results);

		var snapshotForNodeB = workflow.getStateHistory(runnableConfig)
			.stream()
			.filter(s -> s.node().equals("B"))
			.findFirst()
			.orElseThrow();

		// FIXME
		runnableConfig = workflow.updateState(snapshotForNodeB.config(), Map.of("messages", "C"));
		resumeConfig = RunnableConfig.builder(runnableConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("D", END), results);
	}

	@Test
	public void interruptBeforeEdgeEvaluation() throws Exception {

		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();
		var workflow = new StateGraph(keyStrategyFactory).addNode("A", _nodeAction("A"))
			.addNode("B", _nodeAction("B"))
			.addNode("C", _nodeAction("C"))
			.addConditionalEdges("B", edge_async(state -> state.value("messages").orElse(END).toString()),
					EdgeMappings.builder().to("A").to("C").toEND().build())
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptAfter("B")
				.interruptBeforeEdge(true)
				.build());

		var runnableConfig = RunnableConfig.builder().build();

		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();
		var results = workflow.stream(Map.of(), runnableConfig)
			.doOnNext( output -> {
				System.out.println(output);
				lastOutputRef.set(output);
			})
			.map(NodeOutput::node)
			.collectList()
			.block();

		assertIterableEquals(List.of(START, "A", "B", "B"), results);
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());

		runnableConfig = workflow.updateState(runnableConfig, Map.of("messages", "C"));
		// FIXME
		RunnableConfig resumeConfig = RunnableConfig.builder(runnableConfig)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
				.build();
		results = workflow.stream(null, resumeConfig)
			.doOnNext(System.out::println)
			.map(NodeOutput::node)
			.collectList()
			.block();
		assertIterableEquals(List.of("C", END), results);
	}

	/**
	 * Test for InterruptableAction.interruptAfter() - the new after hook mechanism.
	 * This test verifies that:
	 * 1. The interruptAfter() method is called after apply() execution
	 * 2. The action result is available in interruptAfter() for inspection
	 * 3. The graph correctly interrupts when interruptAfter() returns InterruptionMetadata
	 * 4. The state is correctly preserved and the graph can resume properly
	 */
	@Test
	public void interruptAfterActionExecution() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();

		// Create a node that implements InterruptableAction with interruptAfter() logic
		// It will interrupt after execution if the action result contains "interrupt_after" = true
		var interruptAfterNode = new InterruptableAfterNodeAction();

		var workflow = new StateGraph(keyStrategyFactory)
			.addNode("A", _nodeAction("A"))
			.addNode("B", interruptAfterNode)
			.addNode("C", _nodeAction("C"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build());

		// First run - trigger interrupt after B's execution
		var runnableConfig = RunnableConfig.builder().build();
		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();

		var results = workflow.stream(Map.of("trigger_interrupt_after", true), runnableConfig)
			.doOnNext(output -> {
				System.out.println("Output: " + output);
				lastOutputRef.set(output);
			})
			.map(NodeOutput::node)
			.collectList()
			.block();

		// Should execute START -> A -> B, then interrupt after B
		assertIterableEquals(List.of(START, "A", "B"), results);
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());

		// Verify the interruption metadata contains our custom message
		InterruptionMetadata metadata = (InterruptionMetadata) lastOutputRef.get();
		assertTrue(metadata.metadata().isPresent());
		assertTrue(metadata.metadata().get().containsKey("reason"));
		assertEquals("interrupted_after_execution", metadata.metadata().get().get("reason"));

		// Resume execution - should continue from C
		RunnableConfig resumeConfig = RunnableConfig.builder()
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "continue")
			.build();

		results = workflow.stream(null, resumeConfig)
			.doOnNext(output -> System.out.println("Resume output: " + output))
			.map(NodeOutput::node)
			.collectList()
			.block();

		assertIterableEquals(List.of("C", END), results);
	}

	/**
	 * Test that interruptAfter() is NOT called when the action result doesn't trigger interruption.
	 */
	@Test
	public void noInterruptAfterWhenNotTriggered() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder().defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();

		var interruptAfterNode = new InterruptableAfterNodeAction();

		var workflow = new StateGraph(keyStrategyFactory)
			.addNode("A", _nodeAction("A"))
			.addNode("B", interruptAfterNode)
			.addNode("C", _nodeAction("C"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build());

		// Run without triggering interrupt - should complete normally
		var runnableConfig = RunnableConfig.builder().build();

		var results = workflow.stream(Map.of("trigger_interrupt_after", false), runnableConfig)
			.doOnNext(output -> System.out.println("Output: " + output))
			.map(NodeOutput::node)
			.collectList()
			.block();

		// Should execute all nodes without interruption
		assertIterableEquals(List.of(START, "A", "B", "C", END), results);
	}

	/**
	 * A test node action that implements InterruptableAction with interruptAfter() logic.
	 * It will interrupt after execution if the state contains "trigger_interrupt_after" = true.
	 */
	static class InterruptableAfterNodeAction implements AsyncNodeActionWithConfig, InterruptableAction {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// Normal execution - return result with the node identifier
			return CompletableFuture.completedFuture(Map.of("messages", "B"));
		}

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			// No interruption before execution
			return Optional.empty();
		}

		@Override
		public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
				Map<String, Object> actionResult, RunnableConfig config) {
			// Check if we should interrupt after execution based on state
			Boolean shouldInterrupt = (Boolean) state.value("trigger_interrupt_after").orElse(false);

			if (Boolean.TRUE.equals(shouldInterrupt)) {
				// Interrupt after execution with custom metadata
				return Optional.of(InterruptionMetadata.builder(nodeId, state)
					.addMetadata("reason", "interrupted_after_execution")
					.addMetadata("action_result", actionResult)
					.build());
			}

			return Optional.empty();
		}
	}

	// ==================== Streaming Node Tests ====================

	/**
	 * Test for InterruptableAction.interruptAfter() with streaming nodes (Flux).
	 * Verifies that interruptAfter() works correctly when a node returns a Flux.
	 */
	@Test
	public void interruptAfterWithStreamingFlux() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();

		// Create a streaming node that implements InterruptableAction
		var streamingInterruptAfterNode = new StreamingInterruptableAfterNodeAction();

		var workflow = new StateGraph(keyStrategyFactory)
			.addNode("A", _nodeAction("A"))
			.addNode("B", streamingInterruptAfterNode)
			.addNode("C", _nodeAction("C"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build());

		// First run - trigger interrupt after B's streaming execution
		var runnableConfig = RunnableConfig.builder().build();
		AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();
		AtomicReference<Boolean> sawNodeA = new AtomicReference<>(false);
		AtomicReference<Boolean> sawNodeB = new AtomicReference<>(false);

		var results = workflow.stream(Map.of("trigger_interrupt_after", true), runnableConfig)
			.doOnNext(output -> {
				System.out.println("Output: " + output);
				// Track streaming nodes
				if (output instanceof StreamingOutput && "A".equals(output.node())) {
					sawNodeA.set(true);
				}
				if (output instanceof StreamingOutput && "B".equals(output.node())) {
					sawNodeB.set(true);
				}
				// Track non-streaming outputs for lastOutput
				if (!(output instanceof StreamingOutput)) {
					lastOutputRef.set(output);
				}
			})
			.filter(output -> !(output instanceof StreamingOutput)) // Filter streaming chunks
			.map(NodeOutput::node)
			.collectList()
			.block();

		// Verify streaming nodes A and B were executed
		assertTrue(sawNodeA.get(), "Node A should have been executed");
		assertTrue(sawNodeB.get(), "Node B should have been executed");

		// The last non-streaming output should be InterruptionMetadata
		assertInstanceOf(InterruptionMetadata.class, lastOutputRef.get());

		// Verify the interruption metadata
		InterruptionMetadata metadata = (InterruptionMetadata) lastOutputRef.get();
		assertTrue(metadata.metadata().isPresent());
		assertEquals("streaming_interrupted_after", metadata.metadata().get().get("reason"));

		// Resume execution - should continue from C
		RunnableConfig resumeConfig = RunnableConfig.builder()
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "continue")
			.build();

		AtomicReference<Boolean> sawNodeC = new AtomicReference<>(false);
		results = workflow.stream(null, resumeConfig)
			.doOnNext(output -> {
				System.out.println("Resume output: " + output);
				if (output instanceof StreamingOutput && "C".equals(output.node())) {
					sawNodeC.set(true);
				}
			})
			.filter(output -> !(output instanceof StreamingOutput))
			.map(NodeOutput::node)
			.collectList()
			.block();

		// Verify C was executed (it may be streaming or not)
		// The END node should be in the non-streaming results
		assertTrue(results.contains(END), "Should contain END node");
		assertTrue(sawNodeC.get(), "Node C should have been executed after resume");
	}

	/**
	 * Test that streaming nodes complete normally when interruptAfter returns empty.
	 */
	@Test
	public void noInterruptAfterForStreamingWhenNotTriggered() throws Exception {
		var saver = MemorySaver.builder().build();
		KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
			.defaultStrategy(KeyStrategy.REPLACE)
			.addStrategy("messages")
			.build();

		var streamingNode = new StreamingInterruptableAfterNodeAction();

		var workflow = new StateGraph(keyStrategyFactory)
			.addNode("A", _nodeAction("A"))
			.addNode("B", streamingNode)
			.addNode("C", _nodeAction("C"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", END)
			.compile(CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.build());

		var runnableConfig = RunnableConfig.builder().build();
		AtomicReference<Boolean> sawNodeA = new AtomicReference<>(false);
		AtomicReference<Boolean> sawNodeB = new AtomicReference<>(false);
		AtomicReference<Boolean> sawNodeC = new AtomicReference<>(false);
		AtomicReference<Boolean> sawInterruptionMetadata = new AtomicReference<>(false);

		var results = workflow.stream(Map.of("trigger_interrupt_after", false), runnableConfig)
			.doOnNext(output -> {
				System.out.println("Output: " + output);
				// Track streaming nodes
				if (output instanceof StreamingOutput) {
					String node = output.node();
					if ("A".equals(node)) sawNodeA.set(true);
					if ("B".equals(node)) sawNodeB.set(true);
					if ("C".equals(node)) sawNodeC.set(true);
				}
				if (output instanceof InterruptionMetadata) {
					sawInterruptionMetadata.set(true);
				}
			})
			.filter(output -> !(output instanceof StreamingOutput))
			.map(NodeOutput::node)
			.collectList()
			.block();

		// Verify all nodes were executed (streaming nodes are tracked separately)
		assertTrue(sawNodeA.get(), "Node A should have been executed");
		assertTrue(sawNodeB.get(), "Node B should have been executed");
		assertTrue(sawNodeC.get(), "Node C should have been executed in normal flow");
		assertTrue(results.contains(START), "Should contain START node");
		assertTrue(results.contains(END), "Should contain END node");

		// Verify no interruption occurred
		assertFalse(sawInterruptionMetadata.get(), "Should not have InterruptionMetadata when not triggered");
	}

	/**
	 * A streaming test node that implements InterruptableAction with interruptAfter().
	 * Returns a Flux of strings to simulate streaming behavior.
	 */
	static class StreamingInterruptableAfterNodeAction implements AsyncNodeActionWithConfig, InterruptableAction {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			// Return a streaming Flux of strings
			Flux<String> streamingFlux = Flux.just("chunk1", "chunk2", "chunk3");

			Map<String, Object> result = new HashMap<>();
			result.put("streaming_data", streamingFlux);
			result.put("messages", "B");
			return CompletableFuture.completedFuture(result);
		}

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			return Optional.empty();
		}

		@Override
		public Optional<InterruptionMetadata> interruptAfter(String nodeId, OverAllState state,
				Map<String, Object> actionResult, RunnableConfig config) {
			Boolean shouldInterrupt = (Boolean) state.value("trigger_interrupt_after").orElse(false);

			if (Boolean.TRUE.equals(shouldInterrupt)) {
				return Optional.of(InterruptionMetadata.builder(nodeId, state)
					.addMetadata("reason", "streaming_interrupted_after")
					.build());
			}
			return Optional.empty();
		}
	}

}
