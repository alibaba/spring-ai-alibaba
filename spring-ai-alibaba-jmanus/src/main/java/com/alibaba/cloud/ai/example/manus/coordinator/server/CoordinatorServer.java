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

package com.alibaba.cloud.ai.example.manus.coordinator.server;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.config.CoordinatorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Coordinator Server Application
 *
 * Supports multi-endpoint coordinator server, each endpoint corresponds to a group of tools
 * Reference WebFluxStreamableServerApplication's multi-endpoint logic
 */
@Component
public class CoordinatorServer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorServer.class);

	@Autowired
	private CoordinatorToolProperties coordinatorToolProperties;

	@Autowired
	private ServerLifecycle serverManager;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
			return;
		}

		// Delay coordinator server startup to ensure all Beans are initialized
		try {
			Thread.sleep(1000); // Wait 1 second to ensure all Beans are initialized
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		// Start the server using the server manager
		serverManager.start();
	}

	/**
	 * Register CoordinatorTool to coordinator server
	 * @param tool Coordinator tool to register
	 * @return Whether registration was successful
	 */
	public boolean registerCoordinatorTool(CoordinatorTool tool) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool registration");
			return false;
		}

		if (tool == null) {
			log.warn("CoordinatorTool is null, cannot register");
			return false;
		}

		String endpoint = tool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("CoordinatorTool's endpoint is empty, cannot register");
			return false;
		}

		try {
			log.info("Starting to register CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);

			// Use the server manager to register the tool
			boolean success = ((CoordinatorServerManager) serverManager).registerTool(tool);
			
			if (success) {
				log.info("Successfully registered CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);
				log.info("Coordinator Service Access Information:");
				log.info("  Full URL: {}", com.alibaba.cloud.ai.example.manus.coordinator.tool.EndPointUtils.getUrl(endpoint));
			}

			return success;

		}
		catch (Exception e) {
			log.error("Exception occurred while registering CoordinatorTool: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Unregister CoordinatorTool from coordinator server
	 * @param toolName Tool name to unregister
	 * @param endpoint Endpoint address
	 * @return Whether unregistration was successful
	 */
	public boolean unregisterCoordinatorTool(String toolName, String endpoint) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool unregistration");
			return false;
		}

		if (toolName == null || toolName.trim().isEmpty()) {
			log.warn("Tool name is empty, cannot unregister");
			return false;
		}

		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("Endpoint is empty, cannot unregister");
			return false;
		}

		try {
			log.info("Starting to unregister CoordinatorTool: {} from endpoint: {}", toolName, endpoint);

			// Use the server manager to unregister the tool
			boolean success = ((CoordinatorServerManager) serverManager).unregisterTool(toolName, endpoint);
			
			if (success) {
				log.info("Successfully unregistered CoordinatorTool: {} from endpoint: {}", toolName, endpoint);
			}

			return success;

		}
		catch (Exception e) {
			log.error("Exception occurred while unregistering CoordinatorTool: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Forcefully refresh a specific tool
	 * @param toolName Tool name
	 * @param updatedTool Updated tool
	 * @return Whether refresh was successful
	 */
	public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool refresh");
			return false;
		}

		if (updatedTool == null || toolName == null) {
			log.warn("Tool or tool name is empty, cannot refresh");
			return false;
		}

		String endpoint = updatedTool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("Tool's endpoint is empty, cannot refresh");
			return false;
		}

		try {
			log.info("Starting to force refresh tool: {} in endpoint: {}", toolName, endpoint);

			// Use the server manager to refresh the tool
			boolean success = ((CoordinatorServerManager) serverManager).refreshTool(toolName, updatedTool);
			
			if (success) {
				log.info("Successfully refreshed tool: {} in endpoint: {}", toolName, endpoint);
			}

			return success;

		}
		catch (Exception e) {
			log.error("Exception occurred while refreshing tool: {}", e.getMessage(), e);
			return false;
		}
	}

}
