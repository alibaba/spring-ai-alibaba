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

package com.alibaba.cloud.ai.example.manus.coordinator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.coordinator.server.CoordinatorMCPServer;
import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;
import com.alibaba.cloud.ai.example.manus.coordinator.repository.CoordinatorToolRepository;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * Coordinator Service
 *
 * Responsible for loading and managing coordinator tools, handling business logic
 */
@Service
public class CoordinatorService {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorService.class);

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private CoordinatorMCPServer mcpServer;

	@Autowired
	private CoordinatorToolRepository coordinatorToolRepository;

	private final ObjectMapper objectMapper;

	public CoordinatorService() {
		this.objectMapper = new ObjectMapper();
		// Register JSR310 module to support LocalDateTime and other Java 8 time types
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	/**
	 * Load coordinator tools
	 * @return Map of coordinator tools grouped by endpoint
	 */
	public Map<String, List<CoordinatorTool>> loadCoordinatorTools() {
		log.info("Starting to load coordinator tools");

		try {
			// Query published tools from database
			List<CoordinatorToolEntity> publishedEntities = coordinatorToolRepository
				.findByPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);

			log.info("Found {} published tools from database", publishedEntities.size());

			// Convert to CoordinatorTool objects
			List<CoordinatorTool> coordinatorTools = new ArrayList<>();
			for (CoordinatorToolEntity entity : publishedEntities) {
				CoordinatorTool tool = new CoordinatorTool();
				tool.setToolName(entity.getToolName());
				tool.setToolDescription(entity.getToolDescription());
				tool.setToolSchema(entity.getMcpSchema());
				tool.setEndpoint(entity.getEndpoint());
				coordinatorTools.add(tool);
			}

			log.info("Successfully converted {} coordinator tools", coordinatorTools.size());

			// Group by endpoint
			Map<String, List<CoordinatorTool>> groupedTools = coordinatorTools.stream()
				.collect(Collectors.groupingBy(CoordinatorTool::getEndpoint));

			log.info("Successfully loaded coordinator tools, total {} tools, grouped into {} endpoints", coordinatorTools.size(), groupedTools.size());

			// Output tool information for each endpoint
			for (Map.Entry<String, List<CoordinatorTool>> entry : groupedTools.entrySet()) {
				log.info("Endpoint: {}, tool count: {}", entry.getKey(), entry.getValue().size());
				for (CoordinatorTool tool : entry.getValue()) {
					log.info("  - Tool: {} (description: {})", tool.getToolName(), tool.getToolDescription());
				}
			}

			return groupedTools;

		}
		catch (Exception e) {
			log.error("Failed to load coordinator tools: {}", e.getMessage(), e);
			return Map.of();
		}
	}

	/**
	 * Create tool specification for coordinator tool
	 * @param tool Coordinator tool
	 * @return Tool specification
	 */
	public McpServerFeatures.SyncToolSpecification createToolSpecification(CoordinatorTool tool) {
		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(io.modelcontextprotocol.spec.McpSchema.Tool.builder()
				.name(tool.getToolName())
				.description(tool.getToolDescription())
				.inputSchema(tool.getToolSchema())
				.build())
			.callHandler((exchange, request) -> invokeTool(request))
			.build();
	}

	/**
	 * Invoke coordinator tool
	 * @param request Tool invocation request
	 * @return Tool invocation result
	 */
	public CallToolResult invokeTool(CallToolRequest request) {
		try {
			log.debug("Invoking plan coordinator tool, parameters: {}", request.arguments());
			String resultString = null;
			String toolName = request.name();

			// Convert parameters to JSON string
			String rawParam = objectMapper.writeValueAsString(request.arguments());

			log.info("Executing plan template: {}, parameters: {}", toolName, rawParam);

			// Call plan template service
			ResponseEntity<Map<String, Object>> responseEntity = planTemplateService
				.executePlanByTemplateIdInternal(toolName, rawParam);

			// Create simplified response result
			CoordinatorResult response;

			// Process service response result
			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				Map<String, Object> executionResult = responseEntity.getBody();

				// Get planId and status
				String planId = (String) executionResult.get("planId");
				String status = (String) executionResult.get("status");
				String message = (String) executionResult.get("message");
				resultString = "Plan execution completed, planId: " + planId;
				response = new CoordinatorResult(planId, request.arguments(), 2, "success", resultString);

			}
			else {
				response = new CoordinatorResult(null, request.arguments(), 2, "failed", resultString);
			}

			// Convert to JSON string
			String resultJson = objectMapper.writeValueAsString(response);

			log.info("Plan template execution completed: {}, result: {}", toolName, resultJson);

			// Directly return result string, consistent with other tools
			return new CallToolResult(List.of(new McpSchema.TextContent(resultJson)), null);

		}
		catch (Exception e) {
			log.error("Plan coordinator tool invocation failed: {}", e.getMessage(), e);

			// Create simplified error response
			CoordinatorResult errorResponse = new CoordinatorResult(null, request.arguments(), 2, "error", null);
			try {
				String errorJson = objectMapper.writeValueAsString(errorResponse);
				return new CallToolResult(List.of(new McpSchema.TextContent(errorJson)), null);
			}
			catch (Exception jsonError) {
				return new CallToolResult(List.of(new McpSchema.TextContent("Plan coordinator tool invocation failed: " + e.getMessage())), null);
			}
		}
	}

	/**
	 * Publish CoordinatorTool to MCP server
	 * @param tool Coordinator tool to publish
	 * @return Whether publishing was successful
	 */
	public boolean publishCoordinatorTool(CoordinatorTool tool) {
		if (tool == null) {
			log.warn("CoordinatorTool is null, cannot publish");
			return false;
		}

		try {
			log.info("Starting to publish CoordinatorTool: {} to endpoint: {}", tool.getToolName(), tool.getEndpoint());

			// Call MCP server for dynamic registration
			boolean success = mcpServer.registerCoordinatorTool(tool);

			if (success) {
				log.info("Successfully published CoordinatorTool: {} to endpoint: {}", tool.getToolName(), tool.getEndpoint());
			}
			else {
				log.error("Failed to publish CoordinatorTool: {} to endpoint: {}", tool.getToolName(), tool.getEndpoint());
			}

			return success;
		}
		catch (Exception e) {
			log.error("Exception occurred while publishing CoordinatorTool: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Publish CoordinatorToolEntity to MCP server
	 * @param entity Coordinator tool entity to publish
	 * @return Whether publishing was successful
	 */
	public boolean publishCoordinatorTool(CoordinatorToolEntity entity) {
		if (entity == null) {
			log.warn("CoordinatorToolEntity is null, cannot publish");
			return false;
		}

		try {
			log.info("Starting to publish CoordinatorToolEntity: {} to endpoint: {}", entity.getToolName(), entity.getEndpoint());

			CoordinatorTool tool = new CoordinatorTool();
			tool.setToolName(entity.getToolName());
			tool.setToolDescription(entity.getToolDescription());
			tool.setEndpoint(entity.getEndpoint());
			tool.setToolSchema(entity.getMcpSchema());

			// First try to refresh existing tool
			boolean refreshSuccess = mcpServer.refreshTool(entity.getToolName(), tool);
			if (refreshSuccess) {
				log.info("Successfully refreshed tool: {} in MCP server", entity.getToolName());
				return true;
			}

			// If refresh fails, try to register new tool
			log.info("Tool: {} doesn't exist, trying to register new tool", entity.getToolName());
			boolean success = publishCoordinatorTool(tool);

			if (success) {
				log.info("Successfully published CoordinatorToolEntity: {} to endpoint: {}", entity.getToolName(), entity.getEndpoint());
			}
			else {
				log.error("Failed to publish CoordinatorToolEntity: {} to endpoint: {}", entity.getToolName(), entity.getEndpoint());
			}

			return success;
		}
		catch (Exception e) {
			log.error("Exception occurred while publishing CoordinatorToolEntity: {}", e.getMessage(), e);
			return false;
		}
	}

}
