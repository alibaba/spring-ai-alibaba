/*
* Copyright 2025 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.alibaba.cloud.ai.manus.coordinator.controller;

import com.alibaba.cloud.ai.manus.coordinator.entity.vo.CoordinatorToolVO;
import com.alibaba.cloud.ai.manus.coordinator.exception.CoordinatorToolException;
import com.alibaba.cloud.ai.manus.coordinator.service.ICoordinatorToolService;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;

@RestController
@RequestMapping("/api/coordinator-tools")
@CrossOrigin(origins = "*")
public class CoordinatorToolController {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorToolController.class);

	@Autowired
	private ICoordinatorToolService coordinatorToolService;

	@Autowired
	private ISubplanToolService subplanToolService;

	/**
	 * Create coordinator tool
	 */
	@PostMapping
	public ResponseEntity<CoordinatorToolVO> createCoordinatorTool(@RequestBody CoordinatorToolVO toolVO) {
		try {
			log.info("Creating coordinator tool: {}", toolVO);
			CoordinatorToolVO result = coordinatorToolService.createCoordinatorTool(toolVO);
			return ResponseEntity.ok(result);
		}
		catch (CoordinatorToolException e) {
			// Re-throw custom exceptions - they will be handled by @ControllerAdvice
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error creating coordinator tool: {}", e.getMessage(), e);
			throw new CoordinatorToolException("INTERNAL_ERROR",
					"An unexpected error occurred while creating coordinator tool");
		}
	}

	/**
	 * Update coordinator tool
	 */
	@PutMapping("/{id}")
	public ResponseEntity<CoordinatorToolVO> updateCoordinatorTool(@PathVariable("id") Long id,
			@RequestBody CoordinatorToolVO toolVO) {
		try {
			log.info("Updating coordinator tool with ID: {}", id);
			CoordinatorToolVO result = coordinatorToolService.updateCoordinatorTool(id, toolVO);
			return ResponseEntity.ok(result);
		}
		catch (CoordinatorToolException e) {
			// Re-throw custom exceptions - they will be handled by @ControllerAdvice
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error updating coordinator tool: {}", e.getMessage(), e);
			throw new CoordinatorToolException("INTERNAL_ERROR",
					"An unexpected error occurred while updating coordinator tool");
		}
	}

	/**
	 * Get coordinator tool by plan template ID (only if exists)
	 */
	@GetMapping("/get-by-template/{planTemplateId}")
	public ResponseEntity<CoordinatorToolVO> getCoordinatorToolsByTemplate(
			@PathVariable("planTemplateId") String planTemplateId) {
		try {
			log.info("Getting coordinator tool for plan template: {}", planTemplateId);
			return coordinatorToolService.getCoordinatorToolByPlanTemplateId(planTemplateId)
				.map(tool -> ResponseEntity.ok(tool))
				.orElse(ResponseEntity.notFound().build());
		}
		catch (Exception e) {
			log.error("Error getting coordinator tool: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Get or create coordinator tool by plan template ID
	 */
	@GetMapping("/get-or-new-by-template/{planTemplateId}")
	public ResponseEntity<CoordinatorToolVO> getOrNewCoordinatorToolsByTemplate(
			@PathVariable("planTemplateId") String planTemplateId) {
		try {
			log.info("Getting or creating coordinator tool for plan template: {}", planTemplateId);
			CoordinatorToolVO tool = coordinatorToolService.getOrCreateCoordinatorToolByPlanTemplateId(planTemplateId);
			return ResponseEntity.ok(tool);
		}
		catch (Exception e) {
			log.error("Error getting or creating coordinator tool: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Get CoordinatorTool configuration information
	 */
	@GetMapping("/config")
	public ResponseEntity<Map<String, Object>> getCoordinatorToolConfig() {
		Map<String, Object> config = new HashMap<>();
		config.put("enabled", true);
		config.put("success", true);
		return ResponseEntity.ok(config);
	}

	/**
	 * Get all unique endpoints
	 */
	@GetMapping("/endpoints")
	public ResponseEntity<List<String>> getAllEndpoints() {
		try {
			// Get MCP endpoints from CoordinatorToolEntity
			List<String> mcpEndpoints = coordinatorToolService.getAllUniqueMcpEndpoints();

			// Get endpoints from SubplanToolDef (for backward compatibility)
			List<SubplanToolDef> allTools = subplanToolService.getAllSubplanTools();
			List<String> subplanEndpoints = allTools.stream()
				.map(SubplanToolDef::getEndpoint)
				.distinct()
				.collect(java.util.stream.Collectors.toList());

			// Combine all endpoints
			List<String> allEndpoints = new ArrayList<>();
			allEndpoints.addAll(mcpEndpoints);
			allEndpoints.addAll(subplanEndpoints);

			// Remove duplicates and null values
			List<String> uniqueEndpoints = allEndpoints.stream()
				.filter(Objects::nonNull)
				.distinct()
				.collect(java.util.stream.Collectors.toList());

			log.info("Found {} unique endpoints (MCP: {}, Subplan: {})", uniqueEndpoints.size(), mcpEndpoints.size(),
					subplanEndpoints.size());
			return ResponseEntity.ok(uniqueEndpoints);

		}
		catch (Exception e) {
			log.error("Error getting endpoints: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Delete coordinator tool
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteCoordinatorTool(@PathVariable("id") Long id) {
		Map<String, Object> result = new HashMap<>();

		try {
			log.info("Deleting coordinator tool with ID: {}", id);
			coordinatorToolService.deleteCoordinatorTool(id);

			result.put("success", true);
			result.put("message", "Coordinator tool deleted successfully");
			return ResponseEntity.ok(result);

		}
		catch (CoordinatorToolException e) {
			log.error("Error deleting coordinator tool: {}", e.getMessage(), e);
			result.put("success", false);
			result.put("message", e.getMessage());
			return ResponseEntity.status(400).body(result);
		}
		catch (Exception e) {
			log.error("Unexpected error deleting coordinator tool: {}", e.getMessage(), e);
			result.put("success", false);
			result.put("message", "An unexpected error occurred while deleting coordinator tool");
			return ResponseEntity.status(500).body(result);
		}
	}

}
