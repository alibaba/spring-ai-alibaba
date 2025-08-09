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
package com.alibaba.cloud.ai.example.manus.coordinator.controller;

import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;
import com.alibaba.cloud.ai.example.manus.coordinator.repository.CoordinatorToolRepository;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.EndPointUtils;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorToolVO;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorConfigParser;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.example.manus.coordinator.service.CoordinatorService;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateRepository;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateVersionRepository;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplateVersion;
import com.alibaba.cloud.ai.example.manus.config.CoordinatorToolProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coordinator-tools")
@CrossOrigin(origins = "*")
public class CoordinatorToolController {

	@Autowired
	private CoordinatorToolRepository coordinatorToolRepository;

	@Autowired
	private PlanTemplateRepository planTemplateRepository;

	@Autowired
	private PlanTemplateVersionRepository planTemplateVersionRepository;

	@Autowired
	private CoordinatorConfigParser coordinatorConfigParser;

	@Autowired
	private CoordinatorService coordinatorService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CoordinatorToolProperties coordinatorToolProperties;

	/**
	 * Create coordinator tool
	 */
	@PostMapping
	public ResponseEntity<CoordinatorToolVO> createCoordinatorTool(@RequestBody CoordinatorToolVO toolVO) {
		try {
			System.out.println("Received toolVO: " + toolVO);

			// Validate required fields
			if (toolVO.getToolName() == null || toolVO.getToolName().trim().isEmpty()) {
				System.err.println("Tool name is required but was null or empty");
				return ResponseEntity.badRequest().build();
			}
			if (toolVO.getToolDescription() == null || toolVO.getToolDescription().trim().isEmpty()) {
				System.err.println("Tool description is required but was null or empty");
				return ResponseEntity.badRequest().build();
			}
			if (toolVO.getPlanTemplateId() == null || toolVO.getPlanTemplateId().trim().isEmpty()) {
				System.err.println("Plan template ID is required but was null or empty");
				return ResponseEntity.badRequest().build();
			}
			if (toolVO.getEndpoint() == null || toolVO.getEndpoint().trim().isEmpty()) {
				System.err.println("Endpoint is required but was null or empty");
				return ResponseEntity.badRequest().build();
			}

			CoordinatorToolEntity entity = toolVO.toEntity();
			entity.setId(null); // Ensure new creation

			// Set default values
			if (entity.getInputSchema() == null || entity.getInputSchema().trim().isEmpty()) {
				entity.setInputSchema("[]");
			}
			if (entity.getPublishStatus() == null) {
				entity.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
			}

			// Call CoordinatorConfigParser to generate MCP Schema
			String mcpSchema = coordinatorConfigParser.generateToolSchema(entity.getInputSchema());
			entity.setMcpSchema(mcpSchema);

			System.out.println("Entity to save: " + entity);
			CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(entity);
			System.out.println("Saved entity: " + savedEntity);
			return ResponseEntity.ok(CoordinatorToolVO.fromEntity(savedEntity));
		}
		catch (Exception e) {
			System.err.println("Error creating coordinator tool: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Update coordinator tool
	 */
	@PutMapping("/{id}")
	public ResponseEntity<CoordinatorToolVO> updateCoordinatorTool(@PathVariable("id") Long id,
			@RequestBody CoordinatorToolVO toolVO) {
		try {
			Optional<CoordinatorToolEntity> existingEntity = coordinatorToolRepository.findById(id);
			if (existingEntity.isPresent()) {
				CoordinatorToolEntity entity = toolVO.toEntity();
				entity.setId(id);

				// Call CoordinatorConfigParser to generate MCP Schema
				String mcpSchema = coordinatorConfigParser.generateToolSchema(entity.getInputSchema());
				entity.setMcpSchema(mcpSchema);

				CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(entity);
				return ResponseEntity.ok(CoordinatorToolVO.fromEntity(savedEntity));
			}
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Publish tool
	 */
	@PostMapping("/{id}/publish")
	public ResponseEntity<Map<String, Object>> publishCoordinatorTool(@PathVariable("id") Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
			if (entity.isPresent()) {
				CoordinatorToolEntity tool = entity.get();

				// Try to publish to MCP server
				boolean publishSuccess = coordinatorService.publishCoordinatorTool(tool);

				if (publishSuccess) {
					// MCP publish successful, update database status to published
					tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);
					coordinatorToolRepository.save(tool);

					response.put("success", true);
					response.put("message", "Tool has been published successfully to MCP server");
					return ResponseEntity.ok(response);
				}
				else {
					// MCP publish failed, ignore, do not update database status
					response.put("success", false);
					response.put("message", "Failed to publish tool to MCP server, status unchanged");
					return ResponseEntity.ok(response);
				}
			}
			response.put("success", false);
			response.put("message", "Tool not found");
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "Publish failed: " + e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Unpublish tool
	 */
	@PostMapping("/{id}/unpublish")
	public ResponseEntity<Map<String, Object>> unpublishCoordinatorTool(@PathVariable("id") Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
			if (entity.isPresent()) {
				CoordinatorToolEntity tool = entity.get();

				// Try to unpublish from coordinator server
				boolean unpublishSuccess = coordinatorService.unpublishCoordinatorTool(tool);

				if (unpublishSuccess) {
					// Unpublish successful, update database status to unpublished
					tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
					coordinatorToolRepository.save(tool);

					response.put("success", true);
					response.put("message", "Tool has been unpublished successfully from coordinator server");
					return ResponseEntity.ok(response);
				}
				else {
					// Unpublish failed, ignore, do not update database status
					response.put("success", false);
					response.put("message", "Failed to unpublish tool from coordinator server, status unchanged");
					return ResponseEntity.ok(response);
				}
			}
			response.put("success", false);
			response.put("message", "Tool not found");
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "Unpublish failed: " + e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Get or create coordinator tool by plan template ID
	 */
	@GetMapping("/get-or-new-by-template/{planTemplateId}")
	public ResponseEntity<Map<String, Object>> getOrNewCoordinatorToolsByTemplate(
			@PathVariable("planTemplateId") String planTemplateId) {
		Map<String, Object> result = new HashMap<>();

		try {
			// 1. First check if it already exists in coordinator_tools table
			List<CoordinatorToolEntity> existingTools = coordinatorToolRepository.findByPlanTemplateId(planTemplateId);

			if (!existingTools.isEmpty()) {
				// If it already exists, return directly
				List<CoordinatorToolVO> tools = existingTools.stream()
					.map(CoordinatorToolVO::fromEntity)
					.collect(Collectors.toList());

				result.put("success", true);
				result.put("message", "Found existing coordinator tools");
				result.put("data", tools);
				result.put("publishStatus", tools.get(0).getPublishStatus());
				result.put("endpointUrl", EndPointUtils.getUrl(tools.get(0).getEndpoint()));
				return ResponseEntity.ok(result);
			}

			// 2. If it doesn't exist, query plan_template table
			PlanTemplate planTemplate = planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
			if (planTemplate == null) {
				result.put("success", false);
				result.put("message", "Plan template not found: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}

			// 3. Query the latest version
			Integer maxVersionIndex = planTemplateVersionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
			if (maxVersionIndex == null) {
				result.put("success", false);
				result.put("message", "No version found for plan template: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}

			PlanTemplateVersion latestVersion = planTemplateVersionRepository
				.findByPlanTemplateIdAndVersionIndex(planTemplateId, maxVersionIndex);
			if (latestVersion == null) {
				result.put("success", false);
				result.put("message", "Latest version not found for plan template: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}

			// 4. Convert plan_json to CoordinatorConfigVO
			CoordinatorConfigVO mcpPlanConfig = coordinatorConfigParser.parser(latestVersion.getPlanJson());

			// 5. Create CoordinatorToolVO
			CoordinatorToolVO coordinatorToolVO = new CoordinatorToolVO();
			coordinatorToolVO.setToolName(mcpPlanConfig.getId()); // id = toolName
			coordinatorToolVO.setPlanTemplateId(planTemplateId);
			coordinatorToolVO.setToolDescription(mcpPlanConfig.getDescription()); // description
																					// =
																					// toolDescription

			// 6. Convert parameters to JSON as inputSchema
			try {
				String inputSchema = objectMapper.writeValueAsString(mcpPlanConfig.getParameters());
				coordinatorToolVO.setInputSchema(inputSchema);
			}
			catch (Exception e) {
				coordinatorToolVO.setInputSchema("[]");
			}

			// 7. Set default values
			coordinatorToolVO.setMcpSchema("{}");
			coordinatorToolVO.setEndpoint("jmanus");
			coordinatorToolVO.setPublishStatus("UNPUBLISHED");

			result.put("success", true);
			result.put("message", "Created new coordinator tool from plan template");
			result.put("data", coordinatorToolVO);
			return ResponseEntity.ok(result);

		}
		catch (Exception e) {
			result.put("success", false);
			result.put("message", "Error processing request: " + e.getMessage());
			return ResponseEntity.status(500).body(result);
		}
	}

	/**
	 * Get CoordinatorTool configuration information
	 */
	@GetMapping("/config")
	public ResponseEntity<Map<String, Object>> getCoordinatorToolConfig() {
		Map<String, Object> config = new HashMap<>();
		try {
			config.put("enabled", coordinatorToolProperties.isEnabled());
			config.put("showPublishButton", coordinatorToolProperties.isShowPublishButton());
			config.put("success", true);
			return ResponseEntity.ok(config);
		}
		catch (Exception e) {
			config.put("success", false);
			config.put("message", "Failed to get config: " + e.getMessage());
			return ResponseEntity.status(500).body(config);
		}
	}

}