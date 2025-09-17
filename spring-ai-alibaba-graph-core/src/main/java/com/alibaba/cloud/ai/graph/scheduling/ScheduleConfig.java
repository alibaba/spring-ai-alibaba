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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import org.springframework.scheduling.Trigger;

/**
 * Configuration for scheduled graph execution
 */
public class ScheduleConfig {

	private String cronExpression;

	private Long fixedDelay;

	private Long fixedRate;

	private Long initialDelay;

	private Trigger trigger;

	private Map<String, Object> inputs;

	private RunnableConfig runnableConfig;

	private ScheduleMode mode;

	private int maxRetries;

	private Duration retryDelay;

	private Function<Exception, Boolean> retryPredicate;

	private List<ScheduleLifecycleListener> listeners;

	public enum ScheduleMode {

		CRON, FIXED_DELAY, FIXED_RATE, ONE_TIME, TRIGGER

	}

	// Constructor
	private ScheduleConfig(Builder builder) {
		this.cronExpression = builder.cronExpression;
		this.fixedDelay = builder.fixedDelay;
		this.fixedRate = builder.fixedRate;
		this.initialDelay = builder.initialDelay;
		this.inputs = builder.inputs;
		this.runnableConfig = builder.runnableConfig;
		this.maxRetries = builder.maxRetries;
		this.retryDelay = builder.retryDelay;
		this.retryPredicate = builder.retryPredicate;
		this.listeners = builder.listeners;
		this.trigger = builder.trigger;

		// Determine schedule mode based on configuration
		if (cronExpression != null) {
			this.mode = ScheduleMode.CRON;
		}
		else if (fixedDelay != null) {
			this.mode = ScheduleMode.FIXED_DELAY;
		}
		else if (fixedRate != null) {
			this.mode = ScheduleMode.FIXED_RATE;
		}
		else if (initialDelay != null) {
			this.mode = ScheduleMode.ONE_TIME;
		}
		else if (trigger != null) {
			this.mode = ScheduleMode.TRIGGER;
		}
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public Long getFixedDelay() {
		return fixedDelay;
	}

	public Long getFixedRate() {
		return fixedRate;
	}

	public Long getInitialDelay() {
		return initialDelay;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public RunnableConfig getRunnableConfig() {
		return runnableConfig;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public ScheduleMode getMode() {
		return mode;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public Duration getRetryDelay() {
		return retryDelay;
	}

	public Function<Exception, Boolean> getRetryPredicate() {
		return retryPredicate;
	}

	public List<ScheduleLifecycleListener> getListeners() {
		return listeners;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String cronExpression;

		private Long fixedDelay;

		private Long fixedRate;

		private Long initialDelay;

		private Map<String, Object> inputs = new HashMap<>();

		private RunnableConfig runnableConfig;

		private int maxRetries = 0;

		private Duration retryDelay = Duration.ofSeconds(30);

		private Function<Exception, Boolean> retryPredicate = ex -> true;

		private List<ScheduleLifecycleListener> listeners = new ArrayList<>();

		private Trigger trigger;

		public Builder cronExpression(String cronExpression) {
			this.cronExpression = cronExpression;
			return this;
		}

		public Builder fixedDelay(long fixedDelay) {
			this.fixedDelay = fixedDelay;
			return this;
		}

		public Builder fixedRate(long fixedRate) {
			this.fixedRate = fixedRate;
			return this;
		}

		public Builder initialDelay(long initialDelay) {
			this.initialDelay = initialDelay;
			return this;
		}

		public Builder inputs(Map<String, Object> inputs) {
			this.inputs = inputs != null ? inputs : new HashMap<>();
			return this;
		}

		public Builder runnableConfig(RunnableConfig config) {
			this.runnableConfig = config != null ? config : RunnableConfig.builder().build();
			return this;
		}

		public Builder maxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder retryDelay(Duration retryDelay) {
			this.retryDelay = retryDelay;
			return this;
		}

		public Builder retryPredicate(Function<Exception, Boolean> retryPredicate) {
			this.retryPredicate = retryPredicate;
			return this;
		}

		public Builder addListener(ScheduleLifecycleListener listener) {
			this.listeners.add(listener);
			return this;
		}

		public Builder trigger(Trigger trigger) {
			this.trigger = trigger;
			return this;
		}

		public ScheduleConfig build() {
			return new ScheduleConfig(this);
		}

	}

}
