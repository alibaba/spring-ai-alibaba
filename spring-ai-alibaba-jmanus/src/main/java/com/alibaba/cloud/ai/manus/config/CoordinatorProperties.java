/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Coordinator 配置属性
 */
@Component
@ConfigurationProperties(prefix = "coordinator.tool")
public class CoordinatorProperties {

	/**
	 * 是否启用CoordinatorTool功能
	 */
	private boolean enabled = true;

	/**
	 * 轮询配置
	 */
	private Polling polling = new Polling();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Polling getPolling() {
		return polling;
	}

	public void setPolling(Polling polling) {
		this.polling = polling;
	}

	/**
	 * 轮询配置内部类
	 */
	public static class Polling {

		/**
		 * 最大轮询次数
		 */
		private int maxAttempts = 60;

		/**
		 * 轮询间隔（毫秒）
		 */
		private long pollInterval = 10000;

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public long getPollInterval() {
			return pollInterval;
		}

		public void setPollInterval(long pollInterval) {
			this.pollInterval = pollInterval;
		}

	}

}
