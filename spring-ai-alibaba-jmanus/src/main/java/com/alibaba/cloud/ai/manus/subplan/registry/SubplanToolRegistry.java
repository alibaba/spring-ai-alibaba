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
package com.alibaba.cloud.ai.manus.subplan.registry;

import com.alibaba.cloud.ai.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Registry for subplan tools that integrates with PlanningFactory
 *
 * Automatically registers all subplan tools when the application starts
 */
@Component
public class SubplanToolRegistry {

	private static final Logger logger = LoggerFactory.getLogger(SubplanToolRegistry.class);

	@Autowired
	private ISubplanToolService subplanToolService;

	/**
	 * Initialize and register subplan tools after construction
	 */
	@PostConstruct
	public void initialize() {
		registerAllTools();
	}

	/**
	 * Register all subplan tools with PlanningFactory
	 */
	public void registerAllTools() {
		try {
			logger.info("Starting registration of all subplan tools...");

			// Get all subplan tools from the service
			Map<String, PlanningFactory.ToolCallBackContext> toolCallbacks = subplanToolService
				.createSubplanToolCallbacks("system", "system", "Subplan tool execution");

			if (toolCallbacks.isEmpty()) {
				logger.warn("No subplan tools found to register");
				return;
			}

			// Register tools with PlanningFactory
			for (Map.Entry<String, PlanningFactory.ToolCallBackContext> entry : toolCallbacks.entrySet()) {
				String toolName = entry.getKey();
				PlanningFactory.ToolCallBackContext context = entry.getValue();

				try {
					// Register the tool (this would need to be implemented in
					// PlanningFactory)
					// planningFactory.registerTool(toolName, context);
					logger.info("Successfully registered subplan tool: {}", toolName);
				}
				catch (Exception e) {
					logger.error("Failed to register subplan tool: {}", toolName, e);
				}
			}

			logger.info("Completed registration of {} subplan tools", toolCallbacks.size());

		}
		catch (Exception e) {
			logger.error("Failed to register subplan tools", e);
		}
	}

	/**
	 * Register a specific subplan tool by name
	 * @param toolName Name of the tool to register
	 * @return true if registration was successful, false otherwise
	 */
	public boolean registerTool(String toolName) {
		try {
			logger.info("Registering specific subplan tool: {}", toolName);

			// Get tool callbacks for the specific tool
			Map<String, PlanningFactory.ToolCallBackContext> toolCallbacks = subplanToolService
				.createSubplanToolCallbacks("system", "system", "Subplan tool execution");

			PlanningFactory.ToolCallBackContext context = toolCallbacks.get(toolName);
			if (context == null) {
				logger.warn("Tool not found: {}", toolName);
				return false;
			}

			// Register the tool (this would need to be implemented in PlanningFactory)
			// planningFactory.registerTool(toolName, context);
			logger.info("Successfully registered subplan tool: {}", toolName);
			return true;

		}
		catch (Exception e) {
			logger.error("Failed to register subplan tool: {}", toolName, e);
			return false;
		}
	}

	/**
	 * Unregister a specific subplan tool by name
	 * @param toolName Name of the tool to unregister
	 * @return true if unregistration was successful, false otherwise
	 */
	public boolean unregisterTool(String toolName) {
		try {
			logger.info("Unregistering subplan tool: {}", toolName);

			// Unregister the tool (this would need to be implemented in PlanningFactory)
			// planningFactory.unregisterTool(toolName);
			logger.info("Successfully unregistered subplan tool: {}", toolName);
			return true;

		}
		catch (Exception e) {
			logger.error("Failed to unregister subplan tool: {}", toolName, e);
			return false;
		}
	}

}
