/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.core.server;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphAgentExecutorTest {

	@Test
	void cancelDisposesNeverEndingStreamAndUnblocksExecute() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch cancelled = new CountDownLatch(1);
		GraphAgentExecutor executor = executorFor(
				Flux.<NodeOutput>never().doOnSubscribe(ignored -> subscribed.countDown()).doOnCancel(cancelled::countDown));
		RequestContext context = streamingContext();
		AgentEmitter emitter = emitter();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, emitter, failure);

		assertTrue(subscribed.await(2, TimeUnit.SECONDS));
		executor.cancel(context, emitter);
		worker.join(Duration.ofSeconds(2).toMillis());

		assertFalse(worker.isAlive());
		assertTrue(cancelled.await(2, TimeUnit.SECONDS));
		assertThat(failure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
		verify(emitter).cancel();
		verify(emitter, never()).complete();
	}

	@Test
	void cancelStopsSynchronousInfiniteSourceBeforeSubscribeReturns() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch cancelled = new CountDownLatch(1);
		NodeOutput output = mock(NodeOutput.class);
		when(output.node()).thenReturn("preLlm");
		Flux<NodeOutput> synchronousInfinite = Flux.<NodeOutput>generate(sink -> sink.next(output))
			.doOnSubscribe(ignored -> subscribed.countDown())
			.doOnCancel(cancelled::countDown);
		GraphAgentExecutor executor = executorFor(synchronousInfinite);
		RequestContext context = streamingContext();
		AgentEmitter emitter = emitter();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, emitter, failure);

		assertTrue(subscribed.await(2, TimeUnit.SECONDS));
		executor.cancel(context, emitter);
		worker.join(Duration.ofSeconds(2).toMillis());

		assertFalse(worker.isAlive());
		assertTrue(cancelled.await(2, TimeUnit.SECONDS));
		assertThat(failure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
	}

	@Test
	void cancelKeepsTaskRegisteredUntilCancelSignalFinishes() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch cancelEntered = new CountDownLatch(1);
		CountDownLatch releaseCancel = new CountDownLatch(1);
		GraphAgentExecutor executor = executorFor(
				Flux.<NodeOutput>never().doOnSubscribe(ignored -> subscribed.countDown()));
		RequestContext context = streamingContext();
		AgentEmitter firstEmitter = emitter();
		doAnswer(invocation -> {
			cancelEntered.countDown();
			releaseCancel.await();
			return null;
		}).when(firstEmitter).cancel();
		AtomicReference<Throwable> executeFailure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, firstEmitter, executeFailure);
		assertTrue(subscribed.await(2, TimeUnit.SECONDS));

		AtomicReference<Throwable> cancelFailure = new AtomicReference<>();
		Thread cancelWorker = new Thread(() -> {
			try {
				executor.cancel(context, firstEmitter);
			}
			catch (Throwable ex) {
				cancelFailure.set(ex);
			}
		});
		try {
			cancelWorker.start();
			assertTrue(cancelEntered.await(2, TimeUnit.SECONDS));
			AgentEmitter duplicateEmitter = emitter();

			executor.execute(context, duplicateEmitter);

			verify(duplicateEmitter, never()).startWork();
			verify(duplicateEmitter).fail(any(Message.class));
		}
		finally {
			releaseCancel.countDown();
			cancelWorker.join(Duration.ofSeconds(2).toMillis());
			worker.join(Duration.ofSeconds(2).toMillis());
		}
		assertThat(cancelFailure.get()).isNull();
		assertThat(executeFailure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
	}

	@Test
	void interruptDisposesNeverEndingStreamAndUnblocksExecute() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch cancelled = new CountDownLatch(1);
		GraphAgentExecutor executor = executorFor(
				Flux.<NodeOutput>never().doOnSubscribe(ignored -> subscribed.countDown()).doOnCancel(cancelled::countDown));
		RequestContext context = streamingContext();
		AgentEmitter emitter = emitter();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, emitter, failure);

		assertTrue(subscribed.await(2, TimeUnit.SECONDS));
		worker.interrupt();
		worker.join(Duration.ofSeconds(2).toMillis());

		assertFalse(worker.isAlive());
		assertTrue(cancelled.await(2, TimeUnit.SECONDS));
		assertThat(failure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
		verify(emitter).fail(any(Message.class));
		verify(emitter, never()).complete();
	}

	@Test
	void interruptKeepsTaskRegisteredUntilFailureSignalFinishes() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		CountDownLatch failEntered = new CountDownLatch(1);
		CountDownLatch releaseFail = new CountDownLatch(1);
		GraphAgentExecutor executor = executorFor(
				Flux.<NodeOutput>never().doOnSubscribe(ignored -> subscribed.countDown()));
		RequestContext context = streamingContext();
		AgentEmitter firstEmitter = emitter();
		doAnswer(invocation -> {
			boolean interrupted = Thread.interrupted();
			failEntered.countDown();
			releaseFail.await();
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
			return null;
		}).when(firstEmitter).fail(any(Message.class));
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, firstEmitter, failure);
		assertTrue(subscribed.await(2, TimeUnit.SECONDS));

		try {
			worker.interrupt();
			assertTrue(failEntered.await(2, TimeUnit.SECONDS));
			AgentEmitter duplicateEmitter = emitter();
			executor.execute(context, duplicateEmitter);

			verify(duplicateEmitter, never()).startWork();
			verify(duplicateEmitter).fail(any(Message.class));
		}
		finally {
			releaseFail.countDown();
			worker.join(Duration.ofSeconds(2).toMillis());
		}
		assertFalse(worker.isAlive());
		assertThat(failure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
	}

	@Test
	void duplicateStreamIsRejectedBeforeChangingTaskState() throws Exception {
		CountDownLatch subscribed = new CountDownLatch(1);
		GraphAgentExecutor executor = executorFor(
				Flux.<NodeOutput>never().doOnSubscribe(ignored -> subscribed.countDown()));
		RequestContext context = streamingContext();
		AgentEmitter firstEmitter = emitter();
		AgentEmitter duplicateEmitter = emitter();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, firstEmitter, failure);

		assertTrue(subscribed.await(2, TimeUnit.SECONDS));
		executor.execute(context, duplicateEmitter);

		verify(duplicateEmitter, never()).submit();
		verify(duplicateEmitter, never()).startWork();
		verify(duplicateEmitter).fail(any(Message.class));

		executor.cancel(context, firstEmitter);
		worker.join(Duration.ofSeconds(2).toMillis());
		assertFalse(worker.isAlive());
		assertThat(failure.get()).isNull();
		assertThat(activeStreams(executor)).isEmpty();
	}

	@Test
	void synchronousCompletionCleansUpStreamRegistration() throws Exception {
		GraphAgentExecutor executor = executorFor(Flux.empty());
		AgentEmitter emitter = emitter();

		executor.execute(streamingContext(), emitter);

		assertThat(activeStreams(executor)).isEmpty();
		verify(emitter).complete();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void streamErrorCleansUpWithoutExposingInternalMessage() throws Exception {
		GraphAgentExecutor executor = executorFor(Flux.error(new IllegalStateException("database password is hunter2")));
		AgentEmitter emitter = emitter();

		executor.execute(streamingContext(), emitter);

		assertThat(activeStreams(executor)).isEmpty();
		ArgumentCaptor<List<Part<?>>> parts = ArgumentCaptor.forClass((Class) List.class);
		verify(emitter).newAgentMessage(parts.capture(), anyMap());
		assertThat(((TextPart) parts.getValue().get(0)).text()).isEqualTo("Agent execution failed")
			.doesNotContain("hunter2");
		verify(emitter).fail(any(Message.class));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void streamSetupFailureDoesNotExposeInternalMessage() throws Exception {
		Agent agent = mock(Agent.class);
		when(agent.stream(anyString(), any(RunnableConfig.class)))
			.thenThrow(new IllegalStateException("database password is hunter2"));
		GraphAgentExecutor executor = new GraphAgentExecutor(agent);
		AgentEmitter emitter = emitter();

		executor.execute(streamingContext(), emitter);

		ArgumentCaptor<List<Part<?>>> parts = ArgumentCaptor.forClass((Class) List.class);
		verify(emitter).newAgentMessage(parts.capture(), anyMap());
		assertThat(((TextPart) parts.getValue().get(0)).text()).isEqualTo("Agent execution failed")
			.doesNotContain("hunter2");
	}

	@Test
	void reactiveFatalErrorIsNotConvertedIntoAgentFailure() throws Exception {
		AssertionError fatal = new AssertionError("fatal");
		Agent agent = mock(Agent.class);
		when(agent.stream(anyString(), any(RunnableConfig.class))).thenReturn(Flux.error(fatal));
		GraphAgentExecutor executor = new GraphAgentExecutor(agent);
		AgentEmitter emitter = emitter();

		org.junit.jupiter.api.Assertions.assertSame(fatal,
				org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class,
						() -> executor.execute(streamingContext(), emitter)));

		assertThat(activeStreams(executor)).isEmpty();
		verify(emitter, never()).fail(any(Message.class));
	}

	@Test
	void cancelPreventsLateNonStreamArtifactsAndCompletion() throws Exception {
		CountDownLatch invocationStarted = new CountDownLatch(1);
		CountDownLatch invocationInterrupted = new CountDownLatch(1);
		CountDownLatch releaseInvocation = new CountDownLatch(1);
		ReactAgent agent = mock(ReactAgent.class);
		when(agent.getOutputKey()).thenReturn("output");
		doAnswer(invocation -> {
			invocationStarted.countDown();
			while (true) {
				try {
					releaseInvocation.await();
					break;
				}
				catch (InterruptedException ex) {
					invocationInterrupted.countDown();
				}
			}
			return Optional.of(new OverAllState(Map.of("output", "late result")));
		}).when(agent).invoke(anyString(), any(RunnableConfig.class));
		GraphAgentExecutor executor = new GraphAgentExecutor(agent);
		RequestContext context = nonStreamingContext();
		AgentEmitter emitter = emitter();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = executeInThread(executor, context, emitter, failure);

		try {
			assertTrue(invocationStarted.await(2, TimeUnit.SECONDS));
			executor.cancel(context, emitter);
			assertTrue(invocationInterrupted.await(2, TimeUnit.SECONDS));
			verify(emitter).cancel();
		}
		finally {
			releaseInvocation.countDown();
			worker.join(Duration.ofSeconds(2).toMillis());
		}

		assertFalse(worker.isAlive());
		assertThat(failure.get()).isNull();
		assertThat(activeNonStreamExecutions(executor)).isEmpty();
		verify(emitter, never()).addArtifact(anyList(), isNull(), anyString(), anyMap());
		verify(emitter, never()).complete();
		verify(emitter, never()).fail(any(Message.class));
	}

	private static GraphAgentExecutor executorFor(Flux<NodeOutput> stream) throws Exception {
		Agent agent = mock(Agent.class);
		when(agent.stream(anyString(), any(RunnableConfig.class))).thenReturn(stream);
		return new GraphAgentExecutor(agent);
	}

	private static RequestContext streamingContext() {
		RequestContext context = mock(RequestContext.class);
		when(context.getUserInput()).thenReturn("hello");
		when(context.getTaskId()).thenReturn("task-1");
		when(context.getMetadata()).thenReturn(Map.of(GraphAgentExecutor.STREAMING_METADATA_KEY, true));
		return context;
	}

	private static RequestContext nonStreamingContext() {
		RequestContext context = mock(RequestContext.class);
		when(context.getUserInput()).thenReturn("hello");
		when(context.getTaskId()).thenReturn("task-1");
		when(context.getMetadata()).thenReturn(Map.of());
		return context;
	}

	private static AgentEmitter emitter() {
		AgentEmitter emitter = mock(AgentEmitter.class);
		when(emitter.getTaskId()).thenReturn("task-1");
		when(emitter.newAgentMessage(anyList(), anyMap())).thenReturn(mock(Message.class));
		return emitter;
	}

	private static Thread executeInThread(GraphAgentExecutor executor, RequestContext context, AgentEmitter emitter,
			AtomicReference<Throwable> failure) {
		Thread worker = new Thread(() -> {
			try {
				executor.execute(context, emitter);
			}
			catch (Throwable ex) {
				failure.set(ex);
			}
		});
		worker.start();
		return worker;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ?> activeStreams(GraphAgentExecutor executor) {
		return (Map<String, ?>) ReflectionTestUtils.getField(executor, "activeStreams");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ?> activeNonStreamExecutions(GraphAgentExecutor executor) {
		return (Map<String, ?>) ReflectionTestUtils.getField(executor, "activeNonStreamExecutions");
	}

}
