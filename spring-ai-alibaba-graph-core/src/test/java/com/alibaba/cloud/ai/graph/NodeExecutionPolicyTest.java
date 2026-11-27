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

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeExecutionPolicyTest {

	@Test
	void retriesBlockingActionsWithTheSameTaskIdentityAndAFreshState() throws Exception {
		var taskIds = new ArrayList<String>();
		var attempts = new ArrayList<Integer>();
		var events = new CopyOnWriteArrayList<TaskAttemptEvent>();
		var graph = new StateGraph()
				.addNode("write", (state, config) -> {
					assertFalse(Schedulers.isInNonBlockingThread());
					taskIds.add(config.taskId().orElseThrow());
					int attempt = config.taskAttempt().orElseThrow();
					attempts.add(attempt);
					var values = state.<List<String>>value("values").orElseThrow();
					assertEquals(List.of("input"), values);
					values.add("uncommitted");
					if (attempt == 1) {
						throw new IllegalStateException("transient");
					}
					return CompletableFuture.completedFuture(Map.of("result", "done"));
				})
				.addEdge(START, "write").addEdge("write", END)
				.compile(CompileConfig.builder().nodePolicy("write", new NodeExecutionPolicy(
						null, 2, Duration.ZERO, IllegalStateException.class::isInstance, true))
						.withLifecycleListener(new GraphLifecycleListener() {
							@Override
							public void onTaskAttempt(TaskAttemptEvent event, RunnableConfig config) {
								events.add(event);
							}
						}).build());
		var result = graph.stream(Map.of("values", new ArrayList<>(List.of("input"))))
				.subscribeOn(Schedulers.single()).blockLast(Duration.ofSeconds(5)).state();
		assertEquals("done", result.value("result").orElseThrow());
		assertEquals(List.of("input"), result.value("values").orElseThrow());
		assertEquals(List.of(1, 2), attempts);
		assertEquals(taskIds.get(0), taskIds.get(1));
		assertEquals(List.of(TaskAttemptEvent.Phase.STARTED, TaskAttemptEvent.Phase.FAILED,
						TaskAttemptEvent.Phase.STARTED, TaskAttemptEvent.Phase.RESULT_READY),
				events.stream().map(TaskAttemptEvent::phase).toList());
		assertEquals(List.of(1, 1, 2, 2), events.stream().map(TaskAttemptEvent::attempt).toList());
		assertTrue(events.stream().allMatch(event -> event.taskId().equals(taskIds.get(0))));
	}

	@Test
	void timesOutTheEntireStreamWithoutRetryingPublishedWork() throws Exception {
		var calls = new AtomicInteger();
		var cancelled = new AtomicBoolean();
		var config = RunnableConfig.builder().threadId("stream-deadline").build();
		var graph = new StateGraph()
				.addNode("stream", (state, run) -> {
					calls.incrementAndGet();
					var stream = Flux.concat(Mono.just("first"),
							Mono.<String>never().doOnCancel(() -> cancelled.set(true)));
					return CompletableFuture.completedFuture(Map.of("stream", stream));
				})
				.addEdge(START, "stream").addEdge("stream", END)
				.compile(CompileConfig.builder().nodePolicy("stream", new NodeExecutionPolicy(
						Duration.ofMillis(400), 3, Duration.ZERO, error -> true, false)).build());
		var error = assertThrows(RuntimeException.class,
				() -> graph.stream(Map.of(), config).blockLast(Duration.ofSeconds(5)));
		assertInstanceOf(TimeoutException.class, error.getCause());
		assertTrue(cancelled.get());
		assertEquals(1, calls.get());
		assertEquals("stream", graph.getState(config).next());
	}

	@Test
	void retainsTaskAttemptAcrossFileCheckpointRestarts(@TempDir Path folder) throws Exception {
		var identity = new ArrayList<String>();
		var attempts = new ArrayList<Integer>();
		var graph = new StateGraph()
				.addNode("worker", (state, run) -> {
					identity.add(run.taskId().orElseThrow());
					int attempt = run.taskAttempt().orElseThrow();
					attempts.add(attempt);
					if (attempt == 1) {
						throw new IllegalArgumentException("needs restart");
					}
					return CompletableFuture.completedFuture(Map.of("worker", true));
				})
				.addNode("sibling", (state, run) -> CompletableFuture.completedFuture(Map.of("sibling", true)))
				.addEdge(START, "worker").addEdge(START, "sibling")
				.addEdge("worker", END).addEdge("sibling", END);
		var config = RunnableConfig.builder().threadId("attempt-restart").build();
		assertThrows(IllegalArgumentException.class, () -> graph.compile(config(folder))
				.stream(Map.of(), config).blockLast(Duration.ofSeconds(5)));
		var last = graph.compile(config(folder)).stream(Map.of(), config.withResume()).blockLast(Duration.ofSeconds(5));
		assertEquals(Boolean.TRUE, last.state().value("worker").orElseThrow());
		assertEquals(Boolean.TRUE, last.state().value("sibling").orElseThrow());
		assertEquals(List.of(1, 2), attempts);
		assertEquals(identity.get(0), identity.get(1));
	}

	private static CompileConfig config(Path folder) {
		var saver = FileSystemSaver.builder().targetFolder(folder).stateSerializer(new SpringAIStateSerializer()).build();
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build()).taskCheckpoints(true)
				.nodePolicy("worker", new NodeExecutionPolicy(null, 1, Duration.ZERO, error -> false, false)).build();
	}

}
