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

import com.alibaba.cloud.ai.studio.core.base.entity.AgentSchemaEntity;
import com.alibaba.cloud.ai.studio.core.base.service.AgentSchemaService;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing agent schemas. Handles CRUD operations for agent
 * configurations.
 *
 * @since 1.0.0.3
 */
@RestController
@RequestMapping("/console/v1/agent-schemas")
public class AgentSchemaController {

	private final AgentSchemaService agentSchemaService;

	public AgentSchemaController(AgentSchemaService agentSchemaService) {
		this.agentSchemaService = agentSchemaService;
	}

	/**
	 * Create a new agent schema
	 */
	@PostMapping
	public Result<AgentSchemaEntity> createAgentSchema(@RequestBody AgentSchemaEntity agentSchemaEntity) {
		AgentSchemaEntity created = agentSchemaService.createAgentSchema(agentSchemaEntity);
		return Result.success(created);
	}

	/**
	 * Update an existing agent schema
	 */
	@PutMapping("/{id}")
	public Result<AgentSchemaEntity> updateAgentSchema(@PathVariable Long id,
			@RequestBody AgentSchemaEntity agentSchemaEntity) {
		agentSchemaEntity.setId(id);
		AgentSchemaEntity updated = agentSchemaService.updateAgentSchema(agentSchemaEntity);
		return Result.success(updated);
	}

	/**
	 * Delete an agent schema
	 */
	@DeleteMapping("/{id}")
	public Result<Void> deleteAgentSchema(@PathVariable Long id) {
		agentSchemaService.deleteAgentSchema(id);
		return Result.success(null);
	}

	/**
	 * Get an agent schema by ID
	 */
	@GetMapping("/{id}")
	public Result<AgentSchemaEntity> getAgentSchema(@PathVariable Long id) {
		AgentSchemaEntity agentSchema = agentSchemaService.getAgentSchemaById(id);
		return Result.success(agentSchema);
	}

	/**
	 * Get all agent schemas for the current workspace
	 */
	@GetMapping
	public Result<List<AgentSchemaEntity>> getAgentSchemas() {
		String workspaceId = "1"; // Use default workspace ID that matches database
		List<AgentSchemaEntity> agentSchemas = agentSchemaService.getAgentSchemasByWorkspaceId(workspaceId);
		return Result.success(agentSchemas);
	}

	/**
	 * Get agent schemas with pagination
	 */
	@GetMapping("/page")
	public Result<PagingList<AgentSchemaEntity>> getAgentSchemasByPage(@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		String workspaceId = "1"; // Use default workspace ID that matches database
		Page<AgentSchemaEntity> page = new Page<>(current, size);
		PagingList<AgentSchemaEntity> result = agentSchemaService.getAgentSchemasByWorkspaceId(workspaceId, page);
		return Result.success(result);
	}

	/**
	 * Search agent schemas by name
	 */
	@GetMapping("/search")
	public Result<List<AgentSchemaEntity>> searchAgentSchemas(@RequestParam String name) {
		String workspaceId = "1"; // Use default workspace ID that matches database
		List<AgentSchemaEntity> agentSchemas = agentSchemaService.getAgentSchemasByName(name, workspaceId);
		return Result.success(agentSchemas);
	}

	/**
	 * Enable or disable an agent schema
	 */
	@PatchMapping("/{id}/enabled")
	public Result<Void> setAgentSchemaEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
		agentSchemaService.setAgentSchemaEnabled(id, enabled);
		return Result.success(null);
	}

}
