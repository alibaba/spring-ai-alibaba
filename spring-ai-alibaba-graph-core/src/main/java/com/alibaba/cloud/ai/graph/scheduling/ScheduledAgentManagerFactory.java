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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating and managing ScheduledAgentManager instances.
 *
 * @author yaohui
 * @since 1.0.0
 */
public class ScheduledAgentManagerFactory {

	private static final Logger log = LoggerFactory.getLogger(ScheduledAgentManagerFactory.class);

	private static volatile ScheduledAgentManagerFactory instance;

	private volatile ScheduledAgentManager activeManager;

	private ScheduledAgentManagerFactory() {
		initializeDefaultProviders();
	}

	/**
	 * Get the singleton factory instance
	 */
	public static ScheduledAgentManagerFactory getInstance() {
		if (instance == null) {
			synchronized (ScheduledAgentManagerFactory.class) {
				if (instance == null) {
					instance = new ScheduledAgentManagerFactory();
				}
			}
		}
		return instance;
	}

	/**
	 * Get the current active manager
	 */
	public ScheduledAgentManager getManager() {
		return activeManager;
	}

	/**
	 * Register a simple custom manager provider (backward compatibility)
	 * @param provider the provider function
	 */
	public void registerProvider(Supplier<ScheduledAgentManager> provider) {
		activeManager = provider.get();
		log.info("Registered ScheduledAgentManager provider for type: {}", activeManager.getClass().getName());
	}

	private void initializeDefaultProviders() {
		registerProvider(DefaultScheduledAgentManager::getInstance);
	}

}
