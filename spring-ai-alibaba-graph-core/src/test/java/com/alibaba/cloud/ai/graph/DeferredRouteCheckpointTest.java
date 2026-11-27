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
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.ParallelGraphFlux;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeferredRouteCheckpointTest {

	private static Map<String, Object> result(String mode) {
		Map<String, Object> delta = Map.of("events", List.of("review"));
		var flux = Flux.just(GraphResponse.done(delta));
		return switch (mode) {
			case "flux" -> Map.of("stream", flux);
			case "graphflux" -> Map.of("stream", GraphFlux.of("review", flux));
			case "parallel" -> Map.of("stream", ParallelGraphFlux.of(List.of(GraphFlux.of("review", flux))));
			default -> delta;
		};
	}

	private static final class BeforeAction implements AsyncNodeActionWithConfig, InterruptableAction {

		private final String id;

		private final List<String> hooks;

		private final List<String> actions;

		private boolean failAction;

		private BeforeAction(String id, List<String> hooks, List<String> actions) {
			this.id = id;
			this.hooks = hooks;
			this.actions = actions;
		}

		public Optional<InterruptionMetadata> interrupt(String n, OverAllState s, RunnableConfig c) {
			hooks.add(id);
			return c.metadata("approved").isPresent() ? Optional.empty()
					: Optional.of(InterruptionMetadata.builder(n, s).build());
		}

		public CompletableFuture<Map<String, Object>> apply(OverAllState s, RunnableConfig c) {
			actions.add(id);
			if (failAction)
				return CompletableFuture.failedFuture(new IOException("successor failed"));
			return CompletableFuture.completedFuture(Map.of("events", List.of(id)));
		}

	}

	private static final class FailingSaver implements BaseCheckpointSaver {

		private final MemorySaver delegate = MemorySaver.builder().build();

		private boolean failResolved;

		public Collection<Checkpoint> list(RunnableConfig c) {
			return delegate.list(c);
		}

		public Optional<Checkpoint> get(RunnableConfig c) {
			return delegate.get(c);
		}

		public RunnableConfig put(RunnableConfig c, Checkpoint cp) throws Exception {
			if (failResolved && "review".equals(cp.getNodeId()) && "a".equals(cp.getNextNodeId()))
				throw new IOException("resolved route write failed");
			return delegate.put(c, cp);
		}

		public Tag release(RunnableConfig c) throws Exception {
			return delegate.release(c);
		}

	}

	private static final class Fixture {

		private final FailingSaver saver = new FailingSaver();

		private final AtomicInteger routes = new AtomicInteger();

		private final List<String> hooks = new ArrayList<>();

		private final List<String> actions = new ArrayList<>();

		private final List<String> lifecycle = new ArrayList<>();

		private final BeforeAction a = new BeforeAction("a", hooks, actions);

		private final RunnableConfig config = RunnableConfig.builder().threadId("review").build();

		private final CompiledGraph graph;

		private Fixture(String mode) throws Exception {
			var listener = new GraphLifecycleListener() {
				public void onStart(String n, Map<String, Object> s, RunnableConfig c) {
					lifecycle.add("start");
				}

				public void before(String n, Map<String, Object> s, RunnableConfig c, Long t) {
					lifecycle.add("before:" + n);
				}

				public void after(String n, Map<String, Object> s, RunnableConfig c, Long t) {
					lifecycle.add("after:" + n);
				}

				public void onComplete(String n, Map<String, Object> s, RunnableConfig c) {
					lifecycle.add("complete");
				}

				public void onError(String n, Map<String, Object> s, Throwable e, RunnableConfig c) {
					lifecycle.add("error");
				}
			};
			graph = new StateGraph(new KeyStrategyFactoryBuilder().addStrategy("route")
				.addStrategy("events", KeyStrategy.APPEND)
				.build()).addNode("review", node_async((s, c) -> result(mode)))
				.addNode("a", a)
				.addNode("b", new BeforeAction("b", hooks, actions))
				.addEdge(START, "review")
				.addConditionalEdges("review", AsyncCommandAction.node_async((s, c) -> {
					routes.incrementAndGet();
					return new Command(s.value("route").orElseThrow().toString(), Map.of("events", List.of("edge")));
				}), Map.of("a", "a", "b", "b"))
				.addEdge("a", END)
				.addEdge("b", END)
				.compile(CompileConfig.builder()
					.saverConfig(SaverConfig.builder().register(saver).build())
					.interruptAfter("review")
					.interruptBeforeEdge(true)
					.withLifecycleListener(listener)
					.build());
		}

		private Checkpoint start() {
			var outputs = graph.streamSnapshots(Map.of("route", "a", "events", List.of("input")), config)
				.collectList()
				.block();
			assertInstanceOf(InterruptionMetadata.class, outputs.get(outputs.size() - 1));
			return saver.get(config).orElseThrow();
		}

		private List<NodeOutput> resume(boolean approved) {
			var c = RunnableConfig.builder(config).resume();
			if (approved)
				c.addMetadata("approved", true);
			return graph.stream(Map.of(), c.build()).collectList().block();
		}

	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void successorPauseRetainsResolvedRouteAndCommandUpdates(String mode) throws Exception {
		var f = new Fixture(mode);
		var original = f.start();
		assertEquals(0, f.routes.get());
		assertEquals(2, f.saver.list(f.config).size());
		var first = f.resume(false);
		assertEquals(List.of("a"), first.stream().map(NodeOutput::node).toList());
		assertInstanceOf(InterruptionMetadata.class, first.get(0));
		var resolved = f.saver.get(f.config).orElseThrow();
		assertNotEquals(original.getId(), resolved.getId());
		assertEquals("review", resolved.getNodeId());
		assertEquals("a", resolved.getNextNodeId());
		assertEquals(List.of("input", "review", "edge"), resolved.getState().get("events"));
		assertEquals(3, f.saver.list(f.config).size());
		assertEquals(1, f.routes.get());
		assertEquals(List.of("start", "before:review", "after:review"), f.lifecycle);
		// Feedback for pending A changes a prior routing field. The resolved route must
		// stay A.
		f.graph.updateState(f.config, Map.of("route", "b", "events", List.of("feedback")));
		var second = f.resume(false);
		assertEquals(List.of("a"), second.stream().map(NodeOutput::node).toList());
		assertEquals(List.of("input", "review", "edge", "feedback"),
				second.get(0).state().value("events").orElseThrow());
		assertEquals(1, f.routes.get());
		assertEquals(4, f.saver.list(f.config).size());
		assertEquals(List.of("a", "a"), f.hooks);
		assertEquals(List.of(), f.actions);
		var done = f.resume(true);
		assertEquals(List.of("a", END), done.stream().map(NodeOutput::node).toList());
		assertEquals(List.of("input", "review", "edge", "feedback", "a"),
				done.get(done.size() - 1).state().value("events").orElseThrow());
		assertEquals(List.of("a"), f.actions);
		assertEquals(1, f.routes.get());
		assertEquals(List.of("start", "before:review", "after:review", "before:a", "after:a", "complete"), f.lifecycle);
		assertEquals(5, f.saver.list(f.config).size());
		assertEquals(List.of(END), f.resume(true).stream().map(NodeOutput::node).toList());
		assertEquals(1, f.routes.get());
		var historical = f.config.withCheckPointId(original.getId());
		assertEquals(GraphRunnerContext.INTERRUPT_AFTER, f.saver.get(historical).orElseThrow().getNextNodeId());
		assertEquals(List.of("input", "review"), f.saver.get(historical).orElseThrow().getState().get("events"));
		var replay = f.graph.stream(Map.of(), historical.withResume()).collectList().block();
		assertEquals(List.of("a"), replay.stream().map(NodeOutput::node).toList());
		assertEquals(2, f.routes.get());
		assertEquals(List.of("input", "review", "edge"), replay.get(0).state().value("events").orElseThrow());
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void resolvedCheckpointFailureStopsBeforeSuccessor(String mode) throws Exception {
		var f = new Fixture(mode);
		var original = f.start();
		f.saver.failResolved = true;
		assertThrows(Exception.class, () -> f.resume(false));
		assertEquals(List.of(), f.hooks);
		assertEquals(List.of(), f.actions);
		assertEquals(original.getId(), f.saver.get(f.config).orElseThrow().getId());
		assertEquals(2, f.saver.list(f.config).size());
		assertEquals(1, f.routes.get());
		assertFalse(f.lifecycle.contains("complete"));
		f.saver.failResolved = false;
		f.resume(false);
		assertEquals(2, f.routes.get());
		assertEquals(List.of("input", "review", "edge"), f.saver.get(f.config).orElseThrow().getState().get("events"));
		f.resume(true);
		assertEquals(2, f.routes.get());
		assertEquals(List.of("a"), f.actions);
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "flux", "graphflux", "parallel" })
	void successorFailureRetriesSuccessorWithoutRerouting(String mode) throws Exception {
		var f = new Fixture(mode);
		f.start();
		f.a.failAction = true;
		assertThrows(Exception.class, () -> f.resume(true));
		assertEquals("a", f.saver.get(f.config).orElseThrow().getNextNodeId());
		assertEquals(1, f.routes.get());
		f.a.failAction = false;
		var done = f.resume(true);
		assertEquals(List.of("a", "a"), f.actions);
		assertEquals(1, f.routes.get());
		assertEquals(List.of("input", "review", "edge", "a"),
				done.get(done.size() - 1).state().value("events").orElseThrow());
	}

}
