/*
 * Copyright 2024-2025 the original author or authors.
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
import org.springframework.scheduling.support.CronTrigger;

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

		switch (config.getMode()) {
			case CRON:
				scheduledFuture = taskScheduler.schedule(this::executeGraph,
						new CronTrigger(config.getCronExpression()));
				break;
			case FIXED_DELAY:
				scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::executeGraph,
						Instant.now().plusMillis(config.getInitialDelay()), Duration.ofMillis(config.getFixedDelay()));
				break;
			case FIXED_RATE:
				scheduledFuture = taskScheduler.scheduleAtFixedRate(this::executeGraph,
						Instant.now().plusMillis(config.getInitialDelay()), Duration.ofMillis(config.getFixedRate()));
				break;
			case ONE_TIME:
				scheduledFuture = taskScheduler.schedule(this::executeGraph,
						Instant.now().plusMillis(config.getInitialDelay()));
				break;
			case TRIGGER:
				scheduledFuture = taskScheduler.schedule(this::executeGraph, config.getTrigger());
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + config.getMode());
		}

		started = true;
		notifyListeners(ScheduleLifecycleListener.ScheduleEvent.STARTED);
		return this;
	}

	/**
	 * Stop the scheduled execution
	 */
	public void stop() {
		if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
			scheduledFuture.cancel(false);
		}
		stopped = true;

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
			notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_STARTED);
			OverAllState initialState = createInitialState(inputs);
			if (runnableConfig == null) {
				String threadId = String.format("%s-%d", taskId, System.currentTimeMillis());
				runnableConfig = RunnableConfig.builder().threadId(threadId).build();
			}
			Optional<OverAllState> result = graph.call(initialState, runnableConfig);
			notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_COMPLETED, result.orElse(null));
		}
		catch (Exception e) {
			log.error("Graph execution failed", e);
			notifyListeners(ScheduleLifecycleListener.ScheduleEvent.EXECUTION_FAILED, e);
		}
	}

	/**
	 * Execute the graph with retry logic
	 */
	private void executeGraph() {
		int attempt = 0;
		while (attempt <= config.getMaxRetries()) {
			try {
				execute(config.getRunnableConfig(), config.getInputs());
				return;
			}
			catch (Exception e) {
				log.warn("Graph execution failed (attempt {}): {}", attempt + 1, e.getMessage());
				if (attempt < config.getMaxRetries() && config.getRetryPredicate().apply(e)) {
					attempt++;
					try {
						Thread.sleep(config.getRetryDelay().toMillis());
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				else {
					break;
				}
			}
		}
	}

	private OverAllState createInitialState(Map<String, Object> inputs) {
		return OverAllStateBuilder.builder()
			.withKeyStrategies(graph.stateGraph.getKeyStrategyFactory().apply())
			.withData(inputs)
			.build();
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
