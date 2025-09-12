// /*
// * Copyright 2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */

// package com.alibaba.cloud.ai.manus.coordinator.service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Lazy;
// import org.springframework.stereotype.Service;

// import com.alibaba.cloud.ai.manus.coordinator.tool.CoordinatorTool;
// import com.alibaba.cloud.ai.manus.coordinator.server.CoordinatorServer;
// import com.alibaba.cloud.ai.manus.coordinator.entity.CoordinatorToolEntity;
// import com.alibaba.cloud.ai.manus.coordinator.repository.CoordinatorToolRepository;

// /**
// * Coordinator Tool Registry Management Service
// *
// * Responsible for tool publishing, unpublishing and other management operations
// */
// @Service
// public class CoordinatorToolRegistry {

// private static final Logger log =
// LoggerFactory.getLogger(CoordinatorToolRegistry.class);

// @Autowired
// @Lazy
// private CoordinatorServer mcpServer;

// @Autowired
// private CoordinatorToolRepository coordinatorToolRepository;

// /**
// * Load published tools
// * @return Tools grouped by endpoint Map
// */
// public Map<String, List<CoordinatorTool>> loadPublishedTools() {
// Result<Map<String, List<CoordinatorTool>>> result = loadPublishedToolsWithResult();
// return result.isSuccess() ? result.getData() : Map.of();
// }

// /**
// * Load published tools (with result wrapper)
// * @return Result wrapped tool Map
// */
// public Result<Map<String, List<CoordinatorTool>>> loadPublishedToolsWithResult() {
// log.info("Starting to load published tools");

// try {
// // Query published tools from database
// List<CoordinatorToolEntity> publishedEntities = coordinatorToolRepository
// .findByPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);

// log.info("Found {} published tools from database", publishedEntities.size());

// // Convert to CoordinatorTool objects
// List<CoordinatorTool> coordinatorTools = new ArrayList<>();
// for (CoordinatorToolEntity entity : publishedEntities) {
// CoordinatorTool tool = new CoordinatorTool();
// tool.setToolName(entity.getToolName());
// tool.setToolDescription(entity.getToolDescription());
// tool.setToolSchema(entity.getMcpSchema());
// tool.setEndpoint(entity.getEndpoint());
// coordinatorTools.add(tool);
// }

// log.info("Successfully converted {} coordinator tools", coordinatorTools.size());

// // Group by endpoint
// Map<String, List<CoordinatorTool>> groupedTools = coordinatorTools.stream()
// .collect(Collectors.groupingBy(CoordinatorTool::getEndpoint));

// log.info("Successfully loaded coordinator tools, total {} tools, grouped into {}
// endpoints",
// coordinatorTools.size(), groupedTools.size());

// return Result.success(groupedTools, "Successfully loaded " + coordinatorTools.size() +
// " tools");
// }
// catch (Exception e) {
// log.error("Failed to load coordinator tools: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.LOAD_FAILED.getCode(), "Failed to load tools: " +
// e.getMessage());
// }
// }

// /**
// * Publish tool
// * @param tool Tool to publish
// * @return Whether publication was successful
// */
// public boolean publish(CoordinatorTool tool) {
// Result<Boolean> result = publishWithResult(tool);
// return result.isSuccess();
// }

// /**
// * Publish tool (with result wrapper)
// * @param tool Tool to publish
// * @return Publication result
// */
// public Result<Boolean> publishWithResult(CoordinatorTool tool) {
// if (tool == null) {
// log.warn("CoordinatorTool is null, cannot publish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool object cannot be
// empty");
// }

// try {
// log.info("Starting to publish tool: {} to endpoint: {}", tool.getToolName(),
// tool.getEndpoint());

// // Call MCP server for dynamic registration
// boolean success = mcpServer.registerCoordinatorTool(tool);

// if (success) {
// log.info("Successfully published tool: {} to endpoint: {}", tool.getToolName(),
// tool.getEndpoint());
// return Result.success(true, "Tool published successfully");
// }
// else {
// log.error("Failed to publish tool: {} to endpoint: {}", tool.getToolName(),
// tool.getEndpoint());
// return Result.failure(ToolErrorCode.PUBLISH_FAILED.getCode(), "MCP server registration
// failed");
// }
// }
// catch (Exception e) {
// log.error("Exception occurred while publishing tool: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.SERVER_ERROR.getCode(),
// "Error occurred while publishing tool: " + e.getMessage());
// }
// }

// /**
// * Publish tool entity
// * @param entity Tool entity to publish
// * @return Whether publication was successful
// */
// public boolean publish(CoordinatorToolEntity entity) {
// Result<Boolean> result = publishWithResult(entity);
// return result.isSuccess();
// }

// /**
// * Publish tool entity (with result wrapper)
// * @param entity Tool entity to publish
// * @return Publication result
// */
// public Result<Boolean> publishWithResult(CoordinatorToolEntity entity) {
// if (entity == null) {
// log.warn("CoordinatorToolEntity is null, cannot publish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool entity object
// cannot be empty");
// }

// try {
// log.info("Starting to publish tool entity: {} to endpoint: {}", entity.getToolName(),
// entity.getEndpoint());

// CoordinatorTool tool = new CoordinatorTool();
// tool.setToolName(entity.getToolName());
// tool.setToolDescription(entity.getToolDescription());
// tool.setEndpoint(entity.getEndpoint());
// tool.setToolSchema(entity.getMcpSchema());

// // First try to refresh existing tool
// boolean refreshSuccess = mcpServer.refreshTool(entity.getToolName(), tool);
// if (refreshSuccess) {
// log.info("Successfully refreshed tool: {} in MCP server", entity.getToolName());
// return Result.success(true, "Tool refreshed successfully");
// }

// // If refresh failed, try to register new tool
// log.info("Tool: {} does not exist, trying to register new tool", entity.getToolName());
// Result<Boolean> publishResult = publishWithResult(tool);

// if (publishResult.isSuccess()) {
// log.info("Successfully published tool entity: {} to endpoint: {}",
// entity.getToolName(),
// entity.getEndpoint());
// return Result.success(true, "Tool published successfully");
// }
// else {
// log.error("Failed to publish tool entity: {} to endpoint: {}", entity.getToolName(),
// entity.getEndpoint());
// return publishResult;
// }
// }
// catch (Exception e) {
// log.error("Exception occurred while publishing tool entity: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.SERVER_ERROR.getCode(),
// "Error occurred while publishing tool entity: " + e.getMessage());
// }
// }

// /**
// * Batch publish tools
// * @param tools List of tools to publish
// * @return Number of successfully published tools
// */
// public int publishAll(List<CoordinatorTool> tools) {
// Result<Integer> result = publishAllWithResult(tools);
// return result.isSuccess() ? result.getData() : 0;
// }

// /**
// * Batch publish tools (with result wrapper)
// * @param tools List of tools to publish
// * @return Publication result
// */
// public Result<Integer> publishAllWithResult(List<CoordinatorTool> tools) {
// if (tools == null || tools.isEmpty()) {
// log.warn("Tool list is empty, cannot publish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool list cannot be
// empty");
// }

// int successCount = 0;
// int totalCount = tools.size();
// StringBuilder failedTools = new StringBuilder();

// for (CoordinatorTool tool : tools) {
// Result<Boolean> result = publishWithResult(tool);
// if (result.isSuccess()) {
// successCount++;
// }
// else {
// if (failedTools.length() > 0) {
// failedTools.append(", ");
// }
// failedTools.append(tool.getToolName()).append("(").append(result.getMessage()).append(")");
// }
// }

// log.info("Batch publishing completed, successfully published {} tools out of {} total",
// successCount,
// totalCount);

// if (successCount == totalCount) {
// return Result.success(successCount, "All tools published successfully");
// }
// else if (successCount > 0) {
// return Result.success(successCount,
// String.format("Partially successful tool publishing, %d successful, %d failed. Failed
// tools: %s",
// successCount, totalCount - successCount, failedTools.toString()));
// }
// else {
// return Result.failure(ToolErrorCode.PUBLISH_FAILED.getCode(),
// "All tools failed to publish: " + failedTools.toString());
// }
// }

// /**
// * Batch publish tool entities
// * @param entities List of tool entities to publish
// * @return Number of successfully published tools
// */
// public int publishAllEntities(List<CoordinatorToolEntity> entities) {
// Result<Integer> result = publishAllEntitiesWithResult(entities);
// return result.isSuccess() ? result.getData() : 0;
// }

// /**
// * Batch publish tool entities (with result wrapper)
// * @param entities List of tool entities to publish
// * @return Publication result
// */
// public Result<Integer> publishAllEntitiesWithResult(List<CoordinatorToolEntity>
// entities) {
// if (entities == null || entities.isEmpty()) {
// log.warn("Tool entity list is empty, cannot publish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool entity list
// cannot be empty");
// }

// int successCount = 0;
// int totalCount = entities.size();
// StringBuilder failedEntities = new StringBuilder();

// for (CoordinatorToolEntity entity : entities) {
// Result<Boolean> result = publishWithResult(entity);
// if (result.isSuccess()) {
// successCount++;
// }
// else {
// if (failedEntities.length() > 0) {
// failedEntities.append(", ");
// }
// failedEntities.append(entity.getToolName()).append("(").append(result.getMessage()).append(")");
// }
// }

// log.info("Batch publishing completed, successfully published {} tool entities out of {}
// total", successCount,
// totalCount);

// if (successCount == totalCount) {
// return Result.success(successCount, "All tool entities published successfully");
// }
// else if (successCount > 0) {
// return Result.success(successCount, String.format(
// "Partially successful tool entity publishing, %d successful, %d failed. Failed
// entities: %s",
// successCount, totalCount - successCount, failedEntities.toString()));
// }
// else {
// return Result.failure(ToolErrorCode.PUBLISH_FAILED.getCode(),
// "All tool entities failed to publish: " + failedEntities.toString());
// }
// }

// /**
// * Unpublish tool
// * @param toolName Tool name
// * @param endpoint Endpoint address
// * @return Whether unpublishing was successful
// */
// public boolean unpublish(String toolName, String endpoint) {
// Result<Boolean> result = unpublishWithResult(toolName, endpoint);
// return result.isSuccess();
// }

// /**
// * Unpublish tool (with result wrapper)
// * @param toolName Tool name
// * @param endpoint Endpoint address
// * @return Unpublishing result
// */
// public Result<Boolean> unpublishWithResult(String toolName, String endpoint) {
// if (toolName == null || toolName.trim().isEmpty()) {
// log.warn("Tool name is empty, cannot unpublish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool name cannot be
// empty");
// }

// if (endpoint == null || endpoint.trim().isEmpty()) {
// log.warn("Endpoint address is empty, cannot unpublish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Endpoint address
// cannot be empty");
// }

// try {
// log.info("Starting to unpublish tool: {} from endpoint: {}", toolName, endpoint);

// // Call coordinator server for dynamic unregistration
// boolean success = mcpServer.unregisterCoordinatorTool(toolName, endpoint);

// if (success) {
// log.info("Successfully unpublished tool: {} from endpoint: {}", toolName, endpoint);
// return Result.success(true, "Tool unpublished successfully");
// }
// else {
// log.error("Failed to unpublish tool: {} from endpoint: {}", toolName, endpoint);
// return Result.failure(ToolErrorCode.UNPUBLISH_FAILED.getCode(), "MCP server
// unregistration failed");
// }
// }
// catch (Exception e) {
// log.error("Exception occurred while unpublishing tool: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.SERVER_ERROR.getCode(),
// "Error occurred while unpublishing tool: " + e.getMessage());
// }
// }

// /**
// * Unpublish tool
// * @param tool Tool to unpublish
// * @return Whether unpublishing was successful
// */
// public boolean unpublish(CoordinatorTool tool) {
// Result<Boolean> result = unpublishWithResult(tool);
// return result.isSuccess();
// }

// /**
// * Unpublish tool (with result wrapper)
// * @param tool Tool to unpublish
// * @return Unpublishing result
// */
// public Result<Boolean> unpublishWithResult(CoordinatorTool tool) {
// if (tool == null) {
// log.warn("CoordinatorTool is null, cannot unpublish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool object cannot be
// empty");
// }

// try {
// log.info("Starting to unpublish tool: {} from endpoint: {}", tool.getToolName(),
// tool.getEndpoint());

// Result<Boolean> result = unpublishWithResult(tool.getToolName(), tool.getEndpoint());

// if (result.isSuccess()) {
// log.info("Successfully unpublished tool: {} from endpoint: {}", tool.getToolName(),
// tool.getEndpoint());
// return Result.success(true, "Tool unpublished successfully");
// }
// else {
// log.error("Failed to unpublish tool: {} from endpoint: {}", tool.getToolName(),
// tool.getEndpoint());
// return result;
// }
// }
// catch (Exception e) {
// log.error("Exception occurred while unpublishing tool: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.SERVER_ERROR.getCode(),
// "Error occurred while unpublishing tool: " + e.getMessage());
// }
// }

// /**
// * Unpublish tool entity
// * @param entity Tool entity to unpublish
// * @return Whether unpublishing was successful
// */
// public boolean unpublish(CoordinatorToolEntity entity) {
// Result<Boolean> result = unpublishWithResult(entity);
// return result.isSuccess();
// }

// /**
// * Unpublish tool entity (with result wrapper)
// * @param entity Tool entity to unpublish
// * @return Unpublishing result
// */
// public Result<Boolean> unpublishWithResult(CoordinatorToolEntity entity) {
// if (entity == null) {
// log.warn("CoordinatorToolEntity is null, cannot unpublish");
// return Result.failure(ToolErrorCode.INVALID_PARAMETER.getCode(), "Tool entity object
// cannot be empty");
// }

// try {
// log.info("Starting to unpublish tool entity: {} from endpoint: {}",
// entity.getToolName(),
// entity.getEndpoint());

// Result<Boolean> result = unpublishWithResult(entity.getToolName(),
// entity.getEndpoint());

// if (result.isSuccess()) {
// log.info("Successfully unpublished tool entity: {} from endpoint: {}",
// entity.getToolName(),
// entity.getEndpoint());
// return Result.success(true, "Tool entity unpublished successfully");
// }
// else {
// log.error("Failed to unpublish tool entity: {} from endpoint: {}",
// entity.getToolName(),
// entity.getEndpoint());
// return result;
// }
// }
// catch (Exception e) {
// log.error("Exception occurred while unpublishing tool entity: {}", e.getMessage(), e);
// return Result.failure(ToolErrorCode.SERVER_ERROR.getCode(),
// "Error occurred while unpublishing tool entity: " + e.getMessage());
// }
// }

// }
