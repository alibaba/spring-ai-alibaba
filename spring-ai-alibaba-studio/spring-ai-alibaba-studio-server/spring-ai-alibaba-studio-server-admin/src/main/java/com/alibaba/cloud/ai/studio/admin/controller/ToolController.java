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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.core.base.entity.ToolEntity;
import com.alibaba.cloud.ai.studio.core.base.service.ToolService;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing tools. Handles CRUD operations for tool configurations.
 *
 * @since 1.0.0.3
 */
@RestController
@RequestMapping("/console/v1/tools")
public class ToolController {

	private final ToolService toolService;

	public ToolController(ToolService toolService) {
		this.toolService = toolService;
	}

	/**
	 * Create a new tool
	 */
	@PostMapping
	public Result<ToolEntity> createTool(@RequestBody ToolEntity toolEntity) {
		ToolEntity created = toolService.createTool(toolEntity);
		return Result.success(created);
	}

	/**
	 * Update an existing tool
	 */
	@PutMapping("/{id}")
	public Result<ToolEntity> updateTool(@PathVariable Long id, @RequestBody ToolEntity toolEntity) {
		toolEntity.setId(id);
		ToolEntity updated = toolService.updateTool(toolEntity);
		return Result.success(updated);
	}

	/**
	 * Delete a tool
	 */
	@DeleteMapping("/{id}")
	public Result<Void> deleteTool(@PathVariable Long id) {
		toolService.deleteTool(id);
		return Result.success(null);
	}

	/**
	 * Get a tool by ID
	 */
	@GetMapping("/{id}")
	public Result<ToolEntity> getTool(@PathVariable Long id) {
		ToolEntity tool = toolService.getToolById(id);
		return Result.success(tool);
	}

	/**
	 * Get all tools for the current workspace
	 */
	@GetMapping
	public Result<List<ToolEntity>> getTools() {
		String workspaceId = "default"; // TODO: Get from context
		List<ToolEntity> tools = toolService.getToolsByWorkspaceId(workspaceId);
		return Result.success(tools);
	}

	/**
	 * Get tools with pagination
	 */
	@GetMapping("/page")
	public Result<PagingList<ToolEntity>> getToolsByPage(@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		String workspaceId = "default"; // TODO: Get from context
		Page<ToolEntity> page = new Page<>(current, size);
		PagingList<ToolEntity> result = toolService.getToolsByWorkspaceId(workspaceId, page);
		return Result.success(result);
	}

	/**
	 * Search tools by name
	 */
	@GetMapping("/search")
	public Result<List<ToolEntity>> searchTools(@RequestParam String name) {
		String workspaceId = "default"; // TODO: Get from context
		List<ToolEntity> tools = toolService.getToolsByName(name, workspaceId);
		return Result.success(tools);
	}

	/**
	 * Get tools by plugin ID
	 */
	@GetMapping("/plugin/{pluginId}")
	public Result<List<ToolEntity>> getToolsByPlugin(@PathVariable String pluginId) {
		String workspaceId = "default"; // TODO: Get from context
		List<ToolEntity> tools = toolService.getToolsByPluginId(pluginId, workspaceId);
		return Result.success(tools);
	}

	/**
	 * Enable or disable a tool
	 */
	@PatchMapping("/{id}/enabled")
	public Result<Void> setToolEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
		toolService.setToolEnabled(id, enabled);
		return Result.success(null);
	}

}
