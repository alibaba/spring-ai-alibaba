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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.Agent;
import com.alibaba.cloud.ai.entity.AgentDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AgentStartupInitializationService implements ApplicationRunner, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(AgentStartupInitializationService.class);

	@Autowired
	private AgentService agentService;

	@Autowired
	private AgentVectorService agentVectorService;

	@Autowired
	private DatasourceService datasourceService;

	private final ExecutorService executorService = Executors.newFixedThreadPool(3);

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting automatic initialization of published agents...");

		try {
			CompletableFuture.runAsync(this::initializePublishedAgents, executorService).exceptionally(throwable -> {
				log.error("Error during agent initialization", throwable);
				return null;
			});

		}
		catch (Exception e) {
			log.error("Failed to start agent initialization process", e);
		}
	}

	/**
	 * Initialize all published agents
	 */
	private void initializePublishedAgents() {
		try {
			List<Agent> publishedAgents = agentService.findByStatus("published");

			if (publishedAgents.isEmpty()) {
				log.info("No published agents found, skipping initialization");
				return;
			}

			log.info("Found {} published agents, starting initialization...", publishedAgents.size());

			int successCount = 0;
			int failureCount = 0;

			for (Agent agent : publishedAgents) {
				try {
					boolean initialized = initializeAgentDataSource(agent);
					if (initialized) {
						successCount++;
						log.info("Successfully initialized agent: {} (ID: {})", agent.getName(), agent.getId());
					}
					else {
						failureCount++;
						log.warn("Failed to initialize agent: {} (ID: {}) - no active datasource or tables",
								agent.getName(), agent.getId());
					}
				}
				catch (Exception e) {
					failureCount++;
					log.error("Error initializing agent: {} (ID: {})", agent.getName(), agent.getId(), e);
				}

				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			log.info("Agent initialization completed. Success: {}, Failed: {}, Total: {}", successCount, failureCount,
					publishedAgents.size());

		}
		catch (Exception e) {
			log.error("Error during published agents initialization", e);
		}
	}

	/**
	 * Initialize the data source for a single agent
	 * @param agent The agent
	 * @return Whether the initialization was successful
	 */
	private boolean initializeAgentDataSource(Agent agent) {
		try {
			Long agentId = agent.getId();

			Map<String, Object> statistics = agentVectorService.getVectorStatistics(agentId);
			boolean hasData = (Boolean) statistics.getOrDefault("hasData", false);
			int documentCount = (Integer) statistics.getOrDefault("documentCount", 0);

			if (hasData && documentCount > 0) {
				log.info("Agent {} already has vector data (documents: {}), skipping initialization", agentId,
						documentCount);
				return true;
			}

			List<AgentDatasource> agentDatasources = datasourceService.getAgentDatasources(agentId.intValue());

			if (agentDatasources.isEmpty()) {
				log.warn("Agent {} has no associated datasources", agentId);
				return false;
			}

			AgentDatasource activeDatasource = null;
			for (AgentDatasource agentDatasource : agentDatasources) {
				if (agentDatasource.getIsActive() != null && agentDatasource.getIsActive() == 1) {
					activeDatasource = agentDatasource;
					break;
				}
			}

			if (activeDatasource == null) {
				log.warn("Agent {} has no active datasource", agentId);
				return false;
			}

			Integer datasourceId = activeDatasource.getDatasourceId();

			List<String> tables = agentVectorService.getDatasourceTables(datasourceId);

			if (tables.isEmpty()) {
				log.warn("Datasource {} has no tables available for agent {}", datasourceId, agentId);
				return false;
			}

			log.info("Initializing agent {} with datasource {} and {} tables", agentId, datasourceId, tables.size());

			Boolean result = agentVectorService.initializeSchemaForAgentWithDatasource(agentId, datasourceId, tables);

			if (result) {
				log.info("Successfully initialized datasource for agent {} with {} tables", agentId, tables.size());
				return true;
			}
			else {
				log.error("Failed to initialize datasource for agent {}", agentId);
				return false;
			}

		}
		catch (Exception e) {
			log.error("Error initializing datasource for agent {}", agent.getId(), e);
			return false;
		}
	}

	/**
	 * Clean up resources when the application shuts down. Implement the destroy method of
	 * the DisposableBean interface
	 */
	@Override
	public void destroy() throws Exception {
		if (executorService != null && !executorService.isShutdown()) {
			log.info("Shutting down agent initialization executor service");
			executorService.shutdown();
		}
	}

}
