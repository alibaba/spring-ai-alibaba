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
package com.alibaba.cloud.ai.graph.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.util.Assert;

/**
 * ScheduledGraphExecution
 *
 * @author yaohui &#064;create 2025/8/20 15:04
 **/
public class ScheduledAgentTask {

	private static final Logger log = LoggerFactory.getLogger(CompiledGraph.class);

	private final TaskScheduler taskScheduler;

	private final CompiledGraph graph;

	private final ScheduleConfig config;

	private volatile ScheduledFuture<?> scheduledFuture;

	private volatile boolean started = false;

	private volatile boolean stopped = false;

	private final String taskId;

	public ScheduledAgentTask(CompiledGraph graph, ScheduleConfig config) {
		this.graph = graph;
		this.config = config;
		ScheduledAgentManager scheduledAgentManager = ScheduledAgentManagerFactory.getInstance().getManager();
		this.taskScheduler = scheduledAgentManager.getTaskScheduler();
		// Register with the active manager
		this.taskId = scheduledAgentManager.registerTask(this);
		log.debug("Created ScheduledAgentTask with ID: {}", taskId);
	}

	/**
	 * Start the scheduled execution
	 */
	public ScheduledAgentTask start() {
		if (started) {
			throw new IllegalStateException("Schedule already started");
		}

		RetryingTask task = new RetryingTask(createTrigger());
		scheduledFuture = taskScheduler.schedule(task, task);

		started = true;
		notifyListeners(ScheduleLifecycleListener.ScheduleEvent.STARTED);
		return this;
	}

	private Trigger createTrigger() {
		return switch (config.getMode()) {
			case CRON -> new CronTrigger(config.getCronExpression());
			case FIXED_DELAY, FIXED_RATE -> {
				boolean fixedRate = config.getMode() == ScheduleConfig.ScheduleMode.FIXED_RATE;
				long period = fixedRate ? config.getFixedRate() : config.getFixedDelay();
				Assert.isTrue(period > 0, "Schedule period must be positive");
				PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(period));
				trigger.setFixedRate(fixedRate);
				trigger.setInitialDelay(Duration.ofMillis(config.getInitialDelay()));
				yield trigger;
			}
			case ONE_TIME -> context -> context.lastCompletion() == null
					? context.getClock().instant().plusMillis(config.getInitialDelay()) : null;
			case TRIGGER -> config.getTrigger();
		};
	}

	/**
	 * Stop the scheduled execution
	 */
	public void stop() {
		stopped = true;
		if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
			scheduledFuture.cancel(false);
		}

		// Unregister from active manager
		ScheduledAgentManagerFactory.getInstance().getManager().unregisterTask(taskId);
		log.debug("Stopped and unregistered ScheduledAgentTask with ID: {}", taskId);

		notifyListeners(ScheduleLifecycleListener.ScheduleEvent.STOPPED);
	}

	/**
	 * Execute the graph
	 * @param runnableConfig
	 * @param inputs
	 */
	public void execute(RunnableConfig runnableConfig, Map<String, Object> inputs) {
		try {
			executeOnce(runnableConfig, inputs);
		}
		catch (Exception e) {
			log.error("Graph execution failed", e);
			notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_FAILED, e);
		}
	}

	private void executeOnce(RunnableConfig runnableConfig, Map<String, Object> inputs) throws Exception {
		notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_STARTED);
		OverAllState initialState = createInitialState(inputs);
		if (runnableConfig == null) {
			String threadId = String.format("%s-%d", taskId, System.currentTimeMillis());
			runnableConfig = RunnableConfig.builder().threadId(threadId).build();
		}
		Optional<OverAllState> result = graph.invoke(initialState, runnableConfig);
		notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_COMPLETED, result.orElse(null));
	}

	private class RetryingTask implements Runnable, Trigger {

		private final Trigger trigger;

		private final SimpleTriggerContext executionContext = new SimpleTriggerContext(taskScheduler.getClock());

		private Instant scheduledExecution;

		private Instant actualExecution;

		private Instant nextRetry;

		private int attempt;

		RetryingTask(Trigger trigger) {
			this.trigger = trigger;
		}

		@Override
		public Instant nextExecution(TriggerContext context) {
			if (stopped) {
				return null;
			}
			if (nextRetry != null) {
				return nextRetry;
			}
			scheduledExecution = trigger.nextExecution(executionContext);
			return scheduledExecution;
		}

		@Override
		public void run() {
			if (stopped) {
				return;
			}
			if (attempt == 0) {
				actualExecution = executionContext.getClock().instant();
			}
			boolean retry = false;
			try {
				if (attempt <= config.getMaxRetries()) {
					executeOnce(config.getRunnableConfig(), config.getInputs());
				}
			}
			catch (Exception e) {
				log.warn("Graph execution failed (attempt {}): {}", attempt + 1, e.getMessage());
				notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_FAILED, e);
				if (attempt < config.getMaxRetries() && config.getRetryPredicate().apply(e) && !stopped
						&& !Thread.currentThread().isInterrupted()) {
					long delay = config.getRetryDelay().toMillis();
					Assert.isTrue(delay >= 0, "Retry delay must not be negative");
					nextRetry = executionContext.getClock().instant().plusMillis(delay);
					attempt++;
					retry = true;
				}
			}
			finally {
				if (!retry) {
					// Only a completed retry chain advances the recurring schedule.
					executionContext.update(scheduledExecution, actualExecution, executionContext.getClock().instant());
					nextRetry = null;
					attempt = 0;
				}
			}
		}

	}

	private OverAllState createInitialState(Map<String, Object> inputs) {
		return OverAllStateBuilder.builder().withKeyStrategies(graph.getKeyStrategyMap()).withData(inputs).build();
	}

	private void notifyListeners(ScheduleLifecycleListener.ScheduleEvent event, Object data) {
		config.getListeners().forEach(listener -> {
			try {
				listener.onEvent(event, data);
			}
			catch (Exception e) {
				log.error("Error in schedule listener", e);
			}
		});
	}

	private void notifyListeners(ScheduleLifecycleListener.ScheduleEvent event) {
		notifyListeners(event, null);
	}

	// Getters for monitoring
	public boolean isStarted() {
		return started;
	}

	public boolean isStopped() {
		return stopped;
	}

	/**
	 * Get the unique task ID assigned by the global manager
	 */
	public String getTaskId() {
		return taskId;
	}

	/**
	 * Get the name of the agent associated with this task
	 */
	public String getName() {
		return graph.stateGraph.getName();
	}

}
