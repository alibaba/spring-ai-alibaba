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

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingBeforeEdgeInterruptionTest {

	private static Stream<Arguments> eagerControls() {
		return Stream.of("plain", "flux", "graphflux", "parallel")
			.flatMap(mode -> Stream.of("after-edge", "unconfigured").map(gate -> Arguments.of(mode, gate)));
	}

	private static Map<String, Object> result(String mode, Map<String, Object> delta) {
		return result(mode, Flux.just(GraphResponse.done(delta)), delta);
	}

	private static Map<String, Object> result(String mode, Flux<?> flux, Map<String, Object> delta) {
		return switch (mode) {
			case "flux" -> Map.of("stream", flux);
			case "graphflux" -> Map.of("stream", GraphFlux.of("review", flux));
			case "parallel" -> Map.of("stream", ParallelGraphFlux.of(List.of(GraphFlux.of("review", flux))));
			default -> delta;
		};
	}

	private static final class ReviewAction implements AsyncNodeActionWithConfig, InterruptableAction {

		private final String mode;

		private final boolean interrupt;

		private final Map<String, Object> delta;

		private ReviewAction(String mode, boolean interrupt, Map<String, Object> delta) {
			this.mode = mode;
			this.interrupt = interrupt;
			this.delta = delta;
		}

		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(result(mode, delta));
		}

		public Optional<InterruptionMetadata> interrupt(String node, OverAllState state, RunnableConfig config) {
			return Optional.empty();
		}

		public Optional<InterruptionMetadata> interruptAfter(String node, OverAllState state,
				Map<String, Object> result, RunnableConfig config) {
			assertEquals(delta, result);
			assertEquals(List.of("input"), state.value("trail").orElseThrow());
			return interrupt
					? Optional.of(InterruptionMetadata.builder(node, state).addMetadata("reason", "custom").build())
					: Optional.empty();
		}

	}

	private static final class Fixture {

		private final MemorySaver saver = MemorySaver.builder().build();

		private final AtomicInteger routes = new AtomicInteger();

		private final List<String> events = new ArrayList<>();

		private final RunnableConfig config = RunnableConfig.builder().threadId("review").build();

		private final CompiledGraph graph;

		private Fixture(String mode, boolean beforeEdge, boolean configured, boolean hook, boolean terminal,
				boolean empty) throws Exception {
			var listener = new GraphLifecycleListener() {
				public void onStart(String n, Map<String, Object> s, RunnableConfig c) {
					events.add("start");
				}

				public void before(String n, Map<String, Object> s, RunnableConfig c, Long t) {
					events.add("before:" + n);
				}

				public void after(String n, Map<String, Object> s, RunnableConfig c, Long t) {
					events.add("after:" + n);
				}

				public void onComplete(String n, Map<String, Object> s, RunnableConfig c) {
					events.add("complete");
				}
			};
			var builder = CompileConfig.builder()
				.saverConfig(SaverConfig.builder().register(saver).build())
				.interruptBeforeEdge(beforeEdge)
				.withLifecycleListener(listener);
			if (configured)
				builder.interruptAfter("review");
			var factory = new KeyStrategyFactoryBuilder().addStrategy("decision")
				.addStrategy("trail", KeyStrategy.APPEND)
				.build();
			Map<String, Object> delta = empty ? Map.of() : Map.of("trail", List.of("node"));
			graph = new StateGraph(factory).addNode("review", new ReviewAction(mode, hook, delta))
				.addNode("approve", node_async((s, c) -> Map.of("trail", List.of("approve"))))
				.addNode("reject", node_async((s, c) -> Map.of("trail", List.of("reject"))))
				.addEdge(START, "review")
				.addConditionalEdges("review", AsyncCommandAction.node_async((s, c) -> {
					routes.incrementAndGet();
					return new Command(s.value("decision").orElseThrow().toString(), Map.of("trail", List.of("edge")));
				}), Map.of("approve", terminal ? END : "approve", "reject", "reject"))
				.addEdge("approve", END)
				.addEdge("reject", END)
				.compile(builder.build());
		}

		private List<NodeOutput> start(String decision) {
			return graph.stream(Map.of("decision", decision, "trail", List.of("input")), config).collectList().block();
		}

		private List<NodeOutput> feedback(String decision) throws Exception {
			var updated = graph.updateState(config, Map.of("decision", decision, "trail", List.of("feedback")));
			return graph.stream(Map.of(), updated.withResume()).collectList().block();
		}

	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void defersRoutingUntilHumanFeedback(String mode) throws Exception {
		var f = new Fixture(mode, true, true, false, false, false);
		var outputs = f.start("approve");
		assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
		assertEquals(0, f.routes.get());
		assertEquals(List.of("start", "before:review", "after:review"), f.events);
		assertEquals(2, f.saver.list(f.config).size());
		var checkpoint = f.saver.get(f.config).orElseThrow();
		assertEquals(GraphRunnerContext.INTERRUPT_AFTER, checkpoint.getNextNodeId());
		assertEquals(List.of("input", "node"), checkpoint.getState().get("trail"));
		var resumed = f.feedback("reject");
		assertEquals(List.of("reject", END), resumed.stream().map(NodeOutput::node).toList());
		assertEquals(List.of("input", "node", "feedback", "edge", "reject"),
				resumed.get(resumed.size() - 1).state().value("trail").orElseThrow());
		assertEquals(1, f.routes.get());
		assertEquals(List.of("start", "before:review", "after:review", "before:reject", "after:reject", "complete"),
				f.events);
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void canPauseBeforeDecisionIsValid(String mode) throws Exception {
		var f = new Fixture(mode, true, true, false, false, false);
		var outputs = f.start("awaiting-review");
		assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
		assertEquals(0, f.routes.get());
		assertEquals(List.of("reject", END), f.feedback("reject").stream().map(NodeOutput::node).toList());
	}

	@ParameterizedTest
	@MethodSource("eagerControls")
	void preservesEagerRoutingWhenNotDeferred(String mode, String gate) throws Exception {
		boolean configured = gate.equals("after-edge");
		var f = new Fixture(mode, !configured, configured, false, false, false);
		var outputs = f.start("approve");
		assertEquals(1, f.routes.get());
		if (configured) {
			assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
			assertEquals("approve", f.saver.get(f.config).orElseThrow().getNextNodeId());
			assertEquals(List.of("approve", END), f.feedback("reject").stream().map(NodeOutput::node).toList());
			assertEquals(1, f.routes.get());
		}
		else {
			assertFalse(outputs.stream().anyMatch(InterruptionMetadata.class::isInstance));
			assertEquals(END, outputs.get(outputs.size() - 1).node());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void preservesCustomAfterHookPrecedence(String mode) throws Exception {
		var f = new Fixture(mode, true, true, true, false, false);
		var outputs = f.start("approve");
		var interruption = assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
		assertEquals("custom", interruption.metadata().orElseThrow().get("reason"));
		assertEquals("approve", f.saver.get(f.config).orElseThrow().getNextNodeId());
		assertEquals(1, f.routes.get());
		assertEquals(List.of("approve", END), f.feedback("reject").stream().map(NodeOutput::node).toList());
		assertEquals(1, f.routes.get());
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void persistsResolvedEndAndRetainsHistoricalCheckpoint(String mode) throws Exception {
		var f = new Fixture(mode, true, true, false, true, false);
		var initial = f.graph.streamSnapshots(Map.of("decision", "approve", "trail", List.of("input")), f.config)
			.collectList()
			.block();
		assertInstanceOf(InterruptionMetadata.class, initial.get(initial.size() - 1));
		var historical = f.saver.get(f.config).orElseThrow();
		var resumed = f.feedback("approve");
		var expected = List.of("input", "node", "feedback", "edge");
		assertEquals(List.of(END), resumed.stream().map(NodeOutput::node).toList());
		assertEquals(expected, resumed.get(0).state().value("trail").orElseThrow());
		assertEquals(expected, f.saver.get(f.config).orElseThrow().getState().get("trail"));
		assertEquals(END, f.saver.get(f.config).orElseThrow().getNextNodeId());
		assertEquals(4, f.saver.list(f.config).size());
		var replay = f.graph.stream(Map.of(), f.config.withResume()).collectList().block();
		assertEquals(List.of(END), replay.stream().map(NodeOutput::node).toList());
		assertEquals(expected, replay.get(0).state().value("trail").orElseThrow());
		assertEquals(1, f.routes.get());
		assertEquals(4, f.saver.list(f.config).size());
		var oldConfig = f.config.withCheckPointId(historical.getId());
		assertEquals(GraphRunnerContext.INTERRUPT_AFTER, f.saver.get(oldConfig).orElseThrow().getNextNodeId());
		assertEquals(List.of("input", "node"), f.saver.get(oldConfig).orElseThrow().getState().get("trail"));
		var fork = f.graph.stream(Map.of(), oldConfig.withResume()).collectList().block();
		assertEquals(List.of(END), fork.stream().map(NodeOutput::node).toList());
		assertEquals(List.of("input", "node", "edge"), fork.get(0).state().value("trail").orElseThrow());
		assertEquals(2, f.routes.get());
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void emptySuccessfulResultStillPauses(String mode) throws Exception {
		var f = new Fixture(mode, true, true, false, false, true);
		var outputs = f.start("approve");
		assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
		assertEquals(0, f.routes.get());
		var resumed = f.feedback("reject");
		assertEquals(List.of("input", "feedback", "edge", "reject"),
				resumed.get(resumed.size() - 1).state().value("trail").orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = { "flux", "graphflux", "parallel" })
	void trulyEmptyFluxRetainsErrorBehavior(String mode) throws Exception {
		var graph = new StateGraph().addNode("review", node_async((s, c) -> result(mode, Flux.empty(), Map.of())))
			.addEdge(START, "review")
			.addEdge("review", END)
			.compile(CompileConfig.builder().interruptAfter("review").interruptBeforeEdge(true).build());
		assertThrows(Exception.class, () -> graph.stream(Map.of()).collectList().block());
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void terminalAfterEdgeRetainsExistingCompletionBehavior(String mode) throws Exception {
		var f = new Fixture(mode, false, true, false, true, false);
		var outputs = f.start("approve");
		assertFalse(outputs.stream().anyMatch(InterruptionMetadata.class::isInstance));
		assertEquals(END, outputs.get(outputs.size() - 1).node());
		assertEquals(List.of("input", "node", "edge"),
				outputs.get(outputs.size() - 1).state().value("trail").orElseThrow());
		assertEquals(END, f.saver.get(f.config).orElseThrow().getNextNodeId());
		assertEquals(1, f.routes.get());
	}

}
