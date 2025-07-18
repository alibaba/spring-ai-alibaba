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

package com.alibaba.cloud.ai.example.manus.config.startUp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.IConfigService;
import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentScanner;

@Component
public class ConfigAppStartupListener implements ApplicationListener<ApplicationStartedEvent> {

	private static final Logger log = LoggerFactory.getLogger(ConfigAppStartupListener.class);

	@Autowired
	private IConfigService configService;

	@Autowired
	private DynamicAgentScanner dynamicAgentScanner;

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		initializeConfigs();
		initializeDynamicAgents();
	}

	private void initializeConfigs() {
		try {
			List<ConfigEntity> allConfigs = configService.getAllConfigs();

			// Count configurations by configuration group
			Map<String, Long> groupCounts = allConfigs.stream()
				.collect(Collectors.groupingBy(ConfigEntity::getConfigGroup, Collectors.counting()));

			// Log the startup status of the configuration system
			log.info("Configuration system initialized with {} total configs", allConfigs.size());
			groupCounts.forEach((group, count) -> log.info("Group '{}': {} configs", group, count));

			// Check if there are any custom value configurations
			long customizedCount = allConfigs.stream()
				.filter(config -> !config.getConfigValue().equals(config.getDefaultValue()))
				.count();

			log.info("{} configs are using custom values", customizedCount);

		}
		catch (Exception e) {
			log.error("Failed to verify configuration system state", e);
		}
	}

	private void initializeDynamicAgents() {
		try {
			log.info("Starting to initialize dynamic agents...");
			dynamicAgentScanner.scanAndSaveAgents();
			log.info("Dynamic agents initialization completed");
		}
		catch (Exception e) {
			log.error("Failed to initialize dynamic agents", e);
		}
	}

}
