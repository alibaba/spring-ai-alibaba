/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.SimpleTriggerContext;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_COMPLETED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_FAILED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_STARTED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledAgentTaskRetryTest {

	private final TaskScheduler scheduler = mock(TaskScheduler.class);

	private final ScheduledFuture<?> future = mock(ScheduledFuture.class);

	private final MutableClock clock = new MutableClock();

	private final AtomicInteger attempts = new AtomicInteger();

	private final List<ScheduleLifecycleListener.ScheduleEvent> events = new ArrayList<>();

	private ScheduledAgentManager previousManager;

	@BeforeEach
	void setUp() {
		ScheduledAgentManagerFactory factory = ScheduledAgentManagerFactory.getInstance();
		previousManager = factory.getManager();
		ScheduledAgentManager manager = mock(ScheduledAgentManager.class);
		when(manager.getTaskScheduler()).thenReturn(scheduler);
		when(manager.registerTask(any())).thenReturn("retry-test");
		when(scheduler.getClock()).thenReturn(clock);
		doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Trigger.class));
		factory.registerProvider(() -> manager);
	}

	@AfterEach
	void tearDown() {
		ScheduledAgentManagerFactory.getInstance().registerProvider(() -> previousManager);
	}

	@Test
	void retriesTransientFailure() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(1), config().maxRetries(1).build());

		runScheduled(task);

		assertThat(attempts.get()).isEqualTo(2);
		assertThat(events).containsExactly(STARTED, EXECUTION_STARTED, EXECUTION_FAILED, EXECUTION_STARTED,
				EXECUTION_COMPLETED);
	}

	@Test
	void doesNotRetrySuccessfulExecution() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(0), config().maxRetries(2).build());

		runScheduled(task);

		assertThat(attempts.get()).isEqualTo(1);
		assertThat(events).containsExactly(STARTED, EXECUTION_STARTED, EXECUTION_COMPLETED);
	}

	@Test
	void stopsAfterConfiguredRetryLimit() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(Integer.MAX_VALUE), config().maxRetries(2).build());

		runScheduled(task);

		assertThat(attempts.get()).isEqualTo(3);
		assertThat(events.stream().filter(EXECUTION_FAILED::equals)).hasSize(3);
		assertThat(events).doesNotContain(EXECUTION_COMPLETED);
	}

	@Test
	void retryPredicateCanRejectFailure() throws Exception {
		AtomicInteger predicateCalls = new AtomicInteger();
		ScheduledAgentTask task = new ScheduledAgentTask(graph(1), config().maxRetries(2).retryPredicate(error -> {
			predicateCalls.incrementAndGet();
			assertThat(error).isInstanceOf(IllegalStateException.class).hasMessage("transient failure");
			return false;
		}).build());

		runScheduled(task);

		assertThat(attempts.get()).isEqualTo(1);
		assertThat(predicateCalls.get()).isEqualTo(1);
		assertThat(events).contains(EXECUTION_FAILED).doesNotContain(EXECUTION_COMPLETED);
	}

	@Test
	void doesNotRetryByDefault() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(1), config().build());

		runScheduled(task);

		assertThat(attempts.get()).isEqualTo(1);
		assertThat(events).contains(EXECUTION_FAILED).doesNotContain(EXECUTION_COMPLETED);
	}

	@Test
	void manualExecutionReportsFailureWithoutThrowing() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(1), config().maxRetries(2).build());

		assertDoesNotThrow(() -> task.execute(null, Map.of()));

		assertThat(attempts.get()).isEqualTo(1);
		assertThat(events).contains(EXECUTION_FAILED).doesNotContain(EXECUTION_COMPLETED);
	}

	@Test
	void pendingRetriesLeaveSharedSchedulerAvailable() throws Exception {
		ThreadPoolTaskScheduler sharedScheduler = new ThreadPoolTaskScheduler();
		sharedScheduler.setPoolSize(3);
		sharedScheduler.initialize();
		ScheduledAgentManager manager = mock(ScheduledAgentManager.class);
		when(manager.getTaskScheduler()).thenReturn(sharedScheduler);
		when(manager.registerTask(any())).thenReturn("shared-retry-test");
		ScheduledAgentManagerFactory.getInstance().registerProvider(() -> manager);
		CountDownLatch failures = new CountDownLatch(3);
		List<ScheduledAgentTask> tasks = new ArrayList<>();
		try {
			for (int i = 0; i < 3; i++) {
				ScheduleConfig config = ScheduleConfig.builder()
					.initialDelay(0)
					.maxRetries(1)
					.addListener(new ScheduleLifecycleListener() {
						@Override
						public void onEvent(ScheduleEvent event, Object data) {
							if (event == EXECUTION_FAILED) {
								failures.countDown();
							}
						}
					})
					.build();
				tasks.add(new ScheduledAgentTask(graph(Integer.MAX_VALUE), config).start());
			}
			assertThat(failures.await(5, TimeUnit.SECONDS)).isTrue();
			CountDownLatch unrelated = new CountDownLatch(1);
			sharedScheduler.schedule(unrelated::countDown, Instant.now());

			assertThat(unrelated.await(1, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			tasks.forEach(ScheduledAgentTask::stop);
			sharedScheduler.getScheduledThreadPoolExecutor().shutdownNow();
			sharedScheduler.getScheduledThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	@Test
	void fixedDelayStartsAfterFinalRetryCompletes() throws Exception {
		Instant initial = clock.instant();
		ScheduledInvocation invocation = schedule(new ScheduledAgentTask(graph(1),
				config().fixedDelay(10000).maxRetries(1).retryDelay(Duration.ofSeconds(30)).build()));

		assertThat(invocation.runNext()).isEqualTo(initial);
		assertThat(invocation.runNext()).isEqualTo(initial.plusSeconds(30));
		assertThat(invocation.nextExecution()).isEqualTo(initial.plusSeconds(40));
		assertThat(attempts.get()).isEqualTo(2);
	}

	@Test
	void fixedRateKeepsOriginalScheduleAcrossRetries() throws Exception {
		Instant initial = clock.instant();
		ScheduledInvocation invocation = schedule(new ScheduledAgentTask(graph(1),
				config().fixedRate(10000).maxRetries(1).retryDelay(Duration.ofSeconds(30)).build()));

		assertThat(invocation.runNext()).isEqualTo(initial);
		assertThat(invocation.runNext()).isEqualTo(initial.plusSeconds(30));
		assertThat(invocation.runNext()).isEqualTo(initial.plusSeconds(10));
		assertThat(invocation.nextExecution()).isEqualTo(initial.plusSeconds(20));
		assertThat(attempts.get()).isEqualTo(3);
	}

	@Test
	void cronAdvancesAfterFinalRetryCompletion() throws Exception {
		Instant initial = clock.instant();
		ScheduledInvocation invocation = schedule(new ScheduledAgentTask(graph(1),
				config().cronExpression("*/10 * * * * *").maxRetries(1).retryDelay(Duration.ofSeconds(30)).build()));

		assertThat(invocation.runNext()).isEqualTo(initial.plusSeconds(10));
		assertThat(invocation.runNext()).isEqualTo(initial.plusSeconds(40));
		assertThat(invocation.nextExecution()).isEqualTo(initial.plusSeconds(50));
	}

	@Test
	void customTriggerSeesLogicalExecutionContext() throws Exception {
		Instant initial = clock.instant();
		AtomicInteger triggerCalls = new AtomicInteger();
		Trigger trigger = context -> {
			triggerCalls.incrementAndGet();
			if (context.lastCompletion() == null) {
				return initial;
			}
			assertThat(context.getClock()).isSameAs(clock);
			assertThat(context.lastScheduledExecution()).isEqualTo(initial);
			assertThat(context.lastActualExecution()).isEqualTo(initial);
			assertThat(context.lastCompletion()).isEqualTo(initial.plusSeconds(30));
			return null;
		};
		ScheduleConfig config = ScheduleConfig.builder()
			.trigger(trigger)
			.maxRetries(1)
			.retryDelay(Duration.ofSeconds(30))
			.build();
		ScheduledInvocation invocation = schedule(new ScheduledAgentTask(graph(1), config));

		invocation.runNext();
		invocation.runNext();
		assertThat(invocation.nextExecution()).isNull();
		assertThat(triggerCalls.get()).isEqualTo(2);
	}

	@Test
	void stopCancelsPendingRetry() throws Exception {
		ScheduledAgentTask task = new ScheduledAgentTask(graph(1),
				config().maxRetries(1).retryDelay(Duration.ofSeconds(30)).build());
		ScheduledInvocation invocation = schedule(task);
		invocation.runNext();

		task.stop();

		verify(future).cancel(false);
		assertThat(invocation.nextExecution()).isNull();
		invocation.runnable.run();
		assertThat(attempts.get()).isEqualTo(1);
	}

	@Test
	void predicateFailureDoesNotLeaveStaleRetryState() throws Exception {
		ScheduledInvocation invocation = schedule(new ScheduledAgentTask(graph(Integer.MAX_VALUE),
				config().fixedDelay(10000).maxRetries(2).retryDelay(Duration.ofSeconds(30)).retryPredicate(error -> {
					if (attempts.get() == 2) {
						throw new IllegalStateException("predicate failure");
					}
					return true;
				}).build()));
		Instant initial = invocation.runNext();

		assertThrows(IllegalStateException.class, invocation::runNext);

		assertThat(invocation.nextExecution()).isEqualTo(initial.plusSeconds(40));
	}

	@Test
	void periodicSchedulesStillRejectNonPositivePeriods() throws Exception {
		for (long period : new long[] { 0, -1 }) {
			assertThrows(IllegalArgumentException.class,
					() -> new ScheduledAgentTask(graph(0), config().fixedRate(period).build()).start());
			assertThrows(IllegalArgumentException.class,
					() -> new ScheduledAgentTask(graph(0), config().fixedDelay(period).build()).start());
		}
	}

	private CompiledGraph graph(int failures) throws Exception {
		return new StateGraph(new KeyStrategyFactoryBuilder().build()).addNode("work", node_async(state -> {
			if (attempts.incrementAndGet() <= failures) {
				throw new IllegalStateException("transient failure");
			}
			return Map.of();
		})).addEdge(START, "work").addEdge("work", END).compile();
	}

	private ScheduleConfig.Builder config() {
		return ScheduleConfig.builder()
			.initialDelay(0)
			.retryDelay(Duration.ZERO)
			.addListener(new ScheduleLifecycleListener() {
				@Override
				public void onEvent(ScheduleEvent event, Object data) {
					events.add(event);
				}
			});
	}

	private void runScheduled(ScheduledAgentTask task) {
		ScheduledInvocation invocation = schedule(task);
		int runs = 0;
		for (Instant next = invocation.nextExecution(); next != null; next = invocation.nextExecution()) {
			assertThat(++runs).isLessThan(10);
			invocation.run(next);
		}
	}

	private ScheduledInvocation schedule(ScheduledAgentTask task) {
		task.start();
		ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
		verify(scheduler).schedule(runnable.capture(), trigger.capture());
		return new ScheduledInvocation(runnable.getValue(), trigger.getValue());
	}

	private class ScheduledInvocation {

		private final Runnable runnable;

		private final Trigger trigger;

		private final SimpleTriggerContext context = new SimpleTriggerContext(clock);

		ScheduledInvocation(Runnable runnable, Trigger trigger) {
			this.runnable = runnable;
			this.trigger = trigger;
		}

		Instant nextExecution() {
			return trigger.nextExecution(context);
		}

		Instant runNext() {
			Instant scheduled = nextExecution();
			assertThat(scheduled).isNotNull();
			run(scheduled);
			return scheduled;
		}

		void run(Instant scheduled) {
			clock.advanceTo(scheduled);
			Instant actual = clock.instant();
			try {
				runnable.run();
			}
			finally {
				context.update(scheduled, actual, clock.instant());
			}
		}

	}

	private static class MutableClock extends Clock {

		private final AtomicReference<Instant> time;

		private final ZoneId zone;

		MutableClock() {
			this(new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z")), ZoneOffset.UTC);
		}

		MutableClock(AtomicReference<Instant> time, ZoneId zone) {
			this.time = time;
			this.zone = zone;
		}

		@Override
		public ZoneId getZone() {
			return zone;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return new MutableClock(time, zone);
		}

		@Override
		public Instant instant() {
			return time.get();
		}

		void advanceTo(Instant next) {
			time.updateAndGet(current -> next.isAfter(current) ? next : current);
		}

	}

}
