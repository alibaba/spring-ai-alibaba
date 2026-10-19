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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.scheduling.TaskScheduler;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_COMPLETED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_FAILED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.EXECUTION_STARTED;
import static com.alibaba.cloud.ai.graph.scheduling.ScheduleLifecycleListener.ScheduleEvent.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledAgentTaskRetryTest {

	private final TaskScheduler scheduler = mock(TaskScheduler.class);

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
		task.start();
		ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
		verify(scheduler).schedule(runnable.capture(), any(Instant.class));
		runnable.getValue().run();
	}

}
