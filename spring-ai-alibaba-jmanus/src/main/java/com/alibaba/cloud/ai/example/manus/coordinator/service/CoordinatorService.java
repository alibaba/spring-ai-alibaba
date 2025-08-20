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

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorConfigParser;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * Coordinator Service
 *
 * Unified external interface layer, providing concise APIs
 */
@Service
public class CoordinatorService {

	@Autowired
	private CoordinatorToolRegistry registry;

	@Autowired
	private CoordinatorToolExecutor executor;

	@Autowired
	private CoordinatorConfigParser configParser;

	// ==================== Tool Management ====================

	/**
	 * Load published tools
	 * @return Tools grouped by endpoint Map
	 */
	public Map<String, List<CoordinatorTool>> loadTools() {
		return registry.loadPublishedTools();
	}

	/**
	 * Publish tool
	 * @param tool Tool to publish
	 * @return Whether publication was successful
	 */
	public boolean publish(CoordinatorTool tool) {
		return registry.publish(tool);
	}

	/**
	 * Publish tool (with result wrapper)
	 * @param tool Tool to publish
	 * @return Publication result
	 */
	public Result<Boolean> publishWithResult(CoordinatorTool tool) {
		return registry.publishWithResult(tool);
	}

	/**
	 * Publish tool entity
	 * @param entity Tool entity to publish
	 * @return Whether publication was successful
	 */
	public boolean publish(CoordinatorToolEntity entity) {
		return registry.publish(entity);
	}

	/**
	 * Publish tool entity (with result wrapper)
	 * @param entity Tool entity to publish
	 * @return Publication result
	 */
	public Result<Boolean> publishWithResult(CoordinatorToolEntity entity) {
		return registry.publishWithResult(entity);
	}

	/**
	 * Batch publish tools
	 * @param tools List of tools to publish
	 * @return Number of successfully published tools
	 */
	public int publishAll(List<CoordinatorTool> tools) {
		return registry.publishAll(tools);
	}

	/**
	 * Batch publish tool entities
	 * @param entities List of tool entities to publish
	 * @return Number of successfully published tools
	 */
	public int publishAllEntities(List<CoordinatorToolEntity> entities) {
		return registry.publishAllEntities(entities);
	}

	/**
	 * Unpublish tool
	 * @param toolName Tool name
	 * @param endpoint Endpoint address
	 * @return Whether unpublishing was successful
	 */
	public boolean unpublish(String toolName, String endpoint) {
		return registry.unpublish(toolName, endpoint);
	}

	/**
	 * Unpublish tool (with result wrapper)
	 * @param toolName Tool name
	 * @param endpoint Endpoint address
	 * @return Unpublishing result
	 */
	public Result<Boolean> unpublishWithResult(String toolName, String endpoint) {
		return registry.unpublishWithResult(toolName, endpoint);
	}

	/**
	 * Unpublish tool
	 * @param tool Tool to unpublish
	 * @return Whether unpublishing was successful
	 */
	public boolean unpublish(CoordinatorTool tool) {
		return registry.unpublish(tool);
	}

	/**
	 * Unpublish tool entity
	 * @param entity Tool entity to unpublish
	 * @return Whether unpublishing was successful
	 */
	public boolean unpublish(CoordinatorToolEntity entity) {
		return registry.unpublish(entity);
	}

	/**
	 * Unpublish tool entity (with result wrapper)
	 * @param entity Tool entity to unpublish
	 * @return Unpublishing result
	 */
	public Result<Boolean> unpublishWithResult(CoordinatorToolEntity entity) {
		return registry.unpublishWithResult(entity);
	}

	// ==================== Tool Execution ====================

	/**
	 * Create tool specification
	 * @param tool Coordinator tool
	 * @return Tool specification
	 */
	public McpServerFeatures.SyncToolSpecification createSpec(CoordinatorTool tool) {
		return executor.createSpec(tool);
	}

	/**
	 * Execute tool call
	 * @param request Tool call request
	 * @return Tool call result
	 */
	public CallToolResult execute(CallToolRequest request) {
		return executor.execute(request);
	}

	/**
	 * Get plan execution result
	 * @param planId Plan ID
	 * @return Execution result string
	 */
	public String getResult(String planId) {
		return executor.pollPlanResult(planId);
	}

	/**
	 * Get result output
	 * @param record Plan execution record
	 * @return Result output string
	 */
	public String getResultOutput(PlanExecutionRecord record) {
		return executor.extractFinalResult(record);
	}

	// ==================== Configuration Conversion ====================

	/**
	 * Convert configuration to tool
	 * @param config Configuration object
	 * @return Tool object
	 */
	public CoordinatorTool convert(CoordinatorConfigVO config) {
		return configParser.convertToCoordinatorTool(config);
	}

	/**
	 * Batch convert configurations to tools
	 * @param configs Configuration list
	 * @return Tool list
	 */
	public List<CoordinatorTool> convert(List<CoordinatorConfigVO> configs) {
		return configParser.convertToCoordinatorTools(configs);
	}

	// ==================== Compatibility Methods (Deprecated) ====================

	/**
	 * @deprecated Use {@link #loadTools()} instead
	 */
	@Deprecated
	public Map<String, List<CoordinatorTool>> loadCoordinatorTools() {
		return loadTools();
	}

	/**
	 * @deprecated Use {@link #createSpec(CoordinatorTool)} instead
	 */
	@Deprecated
	public McpServerFeatures.SyncToolSpecification createToolSpecification(CoordinatorTool tool) {
		return createSpec(tool);
	}

	/**
	 * @deprecated Use {@link #execute(CallToolRequest)} instead
	 */
	@Deprecated
	public CallToolResult invokeTool(CallToolRequest request) {
		return execute(request);
	}

	/**
	 * @deprecated Use {@link #getResult(String)} instead
	 */
	@Deprecated
	public String pollPlanExecutionStatus(String planId) {
		return getResult(planId);
	}

	/**
	 * @deprecated Use {@link #convert(CoordinatorConfigVO)} instead
	 */
	@Deprecated
	public CoordinatorTool convertToCoordinatorTool(CoordinatorConfigVO config) {
		return convert(config);
	}

	/**
	 * @deprecated Use {@link #convert(List)} instead
	 */
	@Deprecated
	public List<CoordinatorTool> convertToCoordinatorTools(List<CoordinatorConfigVO> configs) {
		return convert(configs);
	}

	/**
	 * @deprecated Use {@link #publish(CoordinatorTool)} instead
	 */
	@Deprecated
	public boolean publishCoordinatorTool(CoordinatorTool tool) {
		return publish(tool);
	}

	/**
	 * @deprecated Use {@link #publish(CoordinatorToolEntity)} instead
	 */
	@Deprecated
	public boolean publishCoordinatorTool(CoordinatorToolEntity entity) {
		return publish(entity);
	}

	/**
	 * @deprecated Use {@link #publishAll(List)} instead
	 */
	@Deprecated
	public int publishCoordinatorTools(List<CoordinatorTool> tools) {
		return publishAll(tools);
	}

	/**
	 * @deprecated Use {@link #publishAllEntities(List)} instead
	 */
	@Deprecated
	public int publishCoordinatorToolEntities(List<CoordinatorToolEntity> entities) {
		return publishAllEntities(entities);
	}

	/**
	 * @deprecated Use {@link #unpublish(String, String)} instead
	 */
	@Deprecated
	public boolean unpublishCoordinatorTool(String toolName, String endpoint) {
		return unpublish(toolName, endpoint);
	}

	/**
	 * @deprecated Use {@link #unpublish(CoordinatorToolEntity)} instead
	 */
	@Deprecated
	public boolean unpublishCoordinatorTool(CoordinatorToolEntity entity) {
		return unpublish(entity);
	}

}
