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

package com.alibaba.cloud.ai.manus.runtime.executor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for level-based executor pools
 */
@Component
@ConfigurationProperties(prefix = "manus.executor.level-based")
public class LevelBasedExecutorConfig {

	/**
	 * Maximum depth level supported (default: 20)
	 */
	private int maxDepthLevel = 20;

	/**
	 * Default core pool size for each level (default: 5)
	 */
	private int defaultCorePoolSize = 5;

	/**
	 * Default maximum pool size for each level (default: 20)
	 */
	private int defaultMaxPoolSize = 20;

	/**
	 * Default queue capacity for each level (default: 100)
	 */
	private int defaultQueueCapacity = 100;

	/**
	 * Default keep-alive time in seconds (default: 60)
	 */
	private long defaultKeepAliveTime = 60L;

	/**
	 * Custom pool configurations for specific levels Key: depth level, Value: pool
	 * configuration
	 */
	private Map<Integer, LevelPoolConfig> levelConfigs = new HashMap<>();

	/**
	 * Configuration for a specific level pool
	 */
	public static class LevelPoolConfig {

		private int corePoolSize;

		private int maxPoolSize;

		private int queueCapacity;

		private long keepAliveTime;

		public LevelPoolConfig() {
		}

		public LevelPoolConfig(int corePoolSize, int maxPoolSize, int queueCapacity, long keepAliveTime) {
			this.corePoolSize = corePoolSize;
			this.maxPoolSize = maxPoolSize;
			this.queueCapacity = queueCapacity;
			this.keepAliveTime = keepAliveTime;
		}

		// Getters and setters
		public int getCorePoolSize() {
			return corePoolSize;
		}

		public void setCorePoolSize(int corePoolSize) {
			this.corePoolSize = corePoolSize;
		}

		public int getMaxPoolSize() {
			return maxPoolSize;
		}

		public void setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
		}

		public int getQueueCapacity() {
			return queueCapacity;
		}

		public void setQueueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
		}

		public long getKeepAliveTime() {
			return keepAliveTime;
		}

		public void setKeepAliveTime(long keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
		}

	}

	// Getters and setters
	public int getMaxDepthLevel() {
		return maxDepthLevel;
	}

	public void setMaxDepthLevel(int maxDepthLevel) {
		this.maxDepthLevel = maxDepthLevel;
	}

	public int getDefaultCorePoolSize() {
		return defaultCorePoolSize;
	}

	public void setDefaultCorePoolSize(int defaultCorePoolSize) {
		this.defaultCorePoolSize = defaultCorePoolSize;
	}

	public int getDefaultMaxPoolSize() {
		return defaultMaxPoolSize;
	}

	public void setDefaultMaxPoolSize(int defaultMaxPoolSize) {
		this.defaultMaxPoolSize = defaultMaxPoolSize;
	}

	public int getDefaultQueueCapacity() {
		return defaultQueueCapacity;
	}

	public void setDefaultQueueCapacity(int defaultQueueCapacity) {
		this.defaultQueueCapacity = defaultQueueCapacity;
	}

	public long getDefaultKeepAliveTime() {
		return defaultKeepAliveTime;
	}

	public void setDefaultKeepAliveTime(long defaultKeepAliveTime) {
		this.defaultKeepAliveTime = defaultKeepAliveTime;
	}

	public Map<Integer, LevelPoolConfig> getLevelConfigs() {
		return levelConfigs;
	}

	public void setLevelConfigs(Map<Integer, LevelPoolConfig> levelConfigs) {
		this.levelConfigs = levelConfigs;
	}

	/**
	 * Get configuration for a specific level
	 * @param level The depth level
	 * @return LevelPoolConfig for the level, or null if not configured
	 */
	public LevelPoolConfig getLevelConfig(int level) {
		return levelConfigs.get(level);
	}

	/**
	 * Add custom configuration for a specific level
	 * @param level The depth level
	 * @param config The pool configuration
	 */
	public void addLevelConfig(int level, LevelPoolConfig config) {
		levelConfigs.put(level, config);
	}

}
