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
// package com.alibaba.cloud.ai.manus.coordinator.controller;

// import com.alibaba.cloud.ai.manus.coordinator.entity.CoordinatorToolEntity;
// import com.alibaba.cloud.ai.manus.coordinator.repository.CoordinatorToolRepository;
// import com.alibaba.cloud.ai.manus.coordinator.tool.EndPointUtils;
// import com.alibaba.cloud.ai.manus.coordinator.vo.CoordinatorToolVO;
// import com.alibaba.cloud.ai.manus.coordinator.tool.CoordinatorConfigParser;
// import com.alibaba.cloud.ai.manus.coordinator.vo.CoordinatorConfigVO;
// import com.alibaba.cloud.ai.manus.coordinator.service.CoordinatorService;
// import com.alibaba.cloud.ai.manus.coordinator.service.Result;
// import com.alibaba.cloud.ai.manus.planning.repository.PlanTemplateRepository;
// import com.alibaba.cloud.ai.manus.planning.repository.PlanTemplateVersionRepository;
// import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplate;
// import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplateVersion;
// import com.alibaba.cloud.ai.manus.config.CoordinatorProperties;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.stream.Collectors;

// @RestController
// @RequestMapping("/api/coordinator-tools")
// @CrossOrigin(origins = "*")
// public class CoordinatorToolController {

// private static final Logger log =
// LoggerFactory.getLogger(CoordinatorToolController.class);

// @Autowired
// private CoordinatorToolRepository coordinatorToolRepository;

// @Autowired
// private PlanTemplateRepository planTemplateRepository;

// @Autowired
// private PlanTemplateVersionRepository planTemplateVersionRepository;

// @Autowired
// private CoordinatorConfigParser coordinatorConfigParser;

// @Autowired
// private CoordinatorService coordinatorService;

// @Autowired
// private ObjectMapper objectMapper;

// @Autowired
// private CoordinatorProperties coordinatorProperties;

// /**
// * Create coordinator tool
// */
// @PostMapping
// public ResponseEntity<CoordinatorToolVO> createCoordinatorTool(@RequestBody
// CoordinatorToolVO toolVO) {
// try {
// log.info("Received toolVO: {}", toolVO);

// // Validate required fields
// if (toolVO.getToolName() == null || toolVO.getToolName().trim().isEmpty()) {
// log.error("Tool name is required but was null or empty");
// return ResponseEntity.badRequest().build();
// }
// if (toolVO.getToolDescription() == null ||
// toolVO.getToolDescription().trim().isEmpty()) {
// log.error("Tool description is required but was null or empty");
// return ResponseEntity.badRequest().build();
// }
// if (toolVO.getPlanTemplateId() == null || toolVO.getPlanTemplateId().trim().isEmpty())
// {
// log.error("Plan template ID is required but was null or empty");
// return ResponseEntity.badRequest().build();
// }
// if (toolVO.getEndpoint() == null || toolVO.getEndpoint().trim().isEmpty()) {
// log.error("Endpoint is required but was null or empty");
// return ResponseEntity.badRequest().build();
// }

// CoordinatorToolEntity entity = toolVO.toEntity();
// entity.setId(null); // Ensure new creation

// // Set default values
// if (entity.getInputSchema() == null || entity.getInputSchema().trim().isEmpty()) {
// entity.setInputSchema("[]");
// }
// if (entity.getPublishStatus() == null) {
// entity.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
// }

// // Call CoordinatorConfigParser to generate MCP Schema
// String mcpSchema = coordinatorConfigParser.generateToolSchema(entity.getInputSchema());
// entity.setMcpSchema(mcpSchema);

// log.info("Entity to save: {}", entity);
// CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(entity);
// log.info("Saved entity: {}", savedEntity);
// return ResponseEntity.ok(CoordinatorToolVO.fromEntity(savedEntity));
// }
// catch (Exception e) {
// log.error("Error creating coordinator tool: {}", e.getMessage(), e);
// return ResponseEntity.badRequest().build();
// }
// }

// /**
// * Update coordinator tool
// */
// @PutMapping("/{id}")
// public ResponseEntity<CoordinatorToolVO> updateCoordinatorTool(@PathVariable("id") Long
// id,
// @RequestBody CoordinatorToolVO toolVO) {
// try {
// Optional<CoordinatorToolEntity> existingEntity =
// coordinatorToolRepository.findById(id);
// if (existingEntity.isPresent()) {
// CoordinatorToolEntity entity = toolVO.toEntity();
// entity.setId(id);

// // Call CoordinatorConfigParser to generate MCP Schema
// String mcpSchema = coordinatorConfigParser.generateToolSchema(entity.getInputSchema());
// entity.setMcpSchema(mcpSchema);

// CoordinatorToolEntity savedEntity = coordinatorToolRepository.save(entity);
// return ResponseEntity.ok(CoordinatorToolVO.fromEntity(savedEntity));
// }
// return ResponseEntity.notFound().build();
// }
// catch (Exception e) {
// return ResponseEntity.badRequest().build();
// }
// }

// /**
// * Publish tool
// */
// @PostMapping("/{id}/publish")
// public ResponseEntity<Map<String, Object>> publishCoordinatorTool(@PathVariable("id")
// Long id) {
// try {
// Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
// if (entity.isPresent()) {
// CoordinatorToolEntity tool = entity.get();

// // Try to publish to MCP server with detailed result
// Result<Boolean> publishResult = coordinatorService.publishWithResult(tool);

// if (publishResult.isSuccess()) {
// // MCP publish successful, update database status to published
// tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);
// coordinatorToolRepository.save(tool);

// Map<String, Object> response = new HashMap<>();
// response.put("success", true);
// response.put("message", publishResult.getMessage());
// response.put("errorCode", publishResult.getErrorCode());
// response.put("timestamp", publishResult.getTimestamp());
// response.put("endpointUrl", EndPointUtils.getUrl(tool.getEndpoint()));
// return ResponseEntity.ok(response);
// }
// else {
// // MCP publish failed, return detailed error information
// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", publishResult.getMessage());
// response.put("errorCode", publishResult.getErrorCode());
// response.put("timestamp", publishResult.getTimestamp());
// return ResponseEntity.badRequest().body(response);
// }
// }

// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", "Tool not found");
// response.put("errorCode", "TOOL_NOT_FOUND");
// return ResponseEntity.notFound().build();
// }
// catch (Exception e) {
// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", "Publish failed: " + e.getMessage());
// response.put("errorCode", "SYSTEM_ERROR");
// return ResponseEntity.status(500).body(response);
// }
// }

// /**
// * Unpublish tool
// */
// @PostMapping("/{id}/unpublish")
// public ResponseEntity<Map<String, Object>> unpublishCoordinatorTool(@PathVariable("id")
// Long id) {
// try {
// Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
// if (entity.isPresent()) {
// CoordinatorToolEntity tool = entity.get();

// // Try to unpublish from coordinator server with detailed result
// Result<Boolean> unpublishResult = coordinatorService.unpublishWithResult(tool);

// if (unpublishResult.isSuccess()) {
// // Unpublish successful, update database status to unpublished
// tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
// coordinatorToolRepository.save(tool);

// Map<String, Object> response = new HashMap<>();
// response.put("success", true);
// response.put("message", unpublishResult.getMessage());
// response.put("errorCode", unpublishResult.getErrorCode());
// response.put("timestamp", unpublishResult.getTimestamp());
// return ResponseEntity.ok(response);
// }
// else {
// // Unpublish failed, return detailed error information
// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", unpublishResult.getMessage());
// response.put("errorCode", unpublishResult.getErrorCode());
// response.put("timestamp", unpublishResult.getTimestamp());
// return ResponseEntity.badRequest().body(response);
// }
// }

// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", "Tool not found");
// response.put("errorCode", "TOOL_NOT_FOUND");
// return ResponseEntity.notFound().build();
// }
// catch (Exception e) {
// Map<String, Object> response = new HashMap<>();
// response.put("success", false);
// response.put("message", "Unpublish failed: " + e.getMessage());
// response.put("errorCode", "SYSTEM_ERROR");
// return ResponseEntity.status(500).body(response);
// }
// }

// /**
// * Get or create coordinator tool by plan template ID
// */
// @GetMapping("/get-or-new-by-template/{planTemplateId}")
// public ResponseEntity<Map<String, Object>> getOrNewCoordinatorToolsByTemplate(
// @PathVariable("planTemplateId") String planTemplateId) {
// Map<String, Object> result = new HashMap<>();

// try {
// // 1. First check if it already exists in coordinator_tools table
// List<CoordinatorToolEntity> existingTools =
// coordinatorToolRepository.findByPlanTemplateId(planTemplateId);

// if (!existingTools.isEmpty()) {
// // If it already exists, return directly
// List<CoordinatorToolVO> tools = existingTools.stream()
// .map(CoordinatorToolVO::fromEntity)
// .collect(Collectors.toList());

// result.put("success", true);
// result.put("message", "Found existing coordinator tools");
// result.put("data", tools);
// result.put("publishStatus", tools.get(0).getPublishStatus());
// result.put("endpointUrl", EndPointUtils.getUrl(tools.get(0).getEndpoint()));
// return ResponseEntity.ok(result);
// }

// // 2. If it doesn't exist, query plan_template table
// PlanTemplate planTemplate =
// planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
// if (planTemplate == null) {
// result.put("success", false);
// result.put("message", "Plan template not found: " + planTemplateId);
// return ResponseEntity.notFound().build();
// }

// // 3. Query the latest version
// Integer maxVersionIndex =
// planTemplateVersionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
// if (maxVersionIndex == null) {
// result.put("success", false);
// result.put("message", "No version found for plan template: " + planTemplateId);
// return ResponseEntity.notFound().build();
// }

// PlanTemplateVersion latestVersion = planTemplateVersionRepository
// .findByPlanTemplateIdAndVersionIndex(planTemplateId, maxVersionIndex);
// if (latestVersion == null) {
// result.put("success", false);
// result.put("message", "Latest version not found for plan template: " + planTemplateId);
// return ResponseEntity.notFound().build();
// }

// // 4. Convert plan_json to CoordinatorConfigVO
// CoordinatorConfigVO mcpPlanConfig =
// coordinatorConfigParser.parse(latestVersion.getPlanJson());

// // 5. Create CoordinatorToolVO
// CoordinatorToolVO coordinatorToolVO = new CoordinatorToolVO();
// coordinatorToolVO.setToolName(mcpPlanConfig.getId()); // id = toolName
// coordinatorToolVO.setPlanTemplateId(planTemplateId);
// coordinatorToolVO.setToolDescription(mcpPlanConfig.getDescription()); // description
// // =
// // toolDescription

// // 6. Convert parameters to JSON as inputSchema
// try {
// String inputSchema = objectMapper.writeValueAsString(mcpPlanConfig.getParameters());
// coordinatorToolVO.setInputSchema(inputSchema);
// }
// catch (Exception e) {
// coordinatorToolVO.setInputSchema("[]");
// }

// // 7. Set default values
// coordinatorToolVO.setMcpSchema("{}");
// coordinatorToolVO.setEndpoint("jmanus");
// coordinatorToolVO.setPublishStatus("UNPUBLISHED");

// result.put("success", true);
// result.put("message", "Created new coordinator tool from plan template");
// result.put("data", coordinatorToolVO);
// return ResponseEntity.ok(result);

// }
// catch (Exception e) {
// result.put("success", false);
// result.put("message", "Error processing request: " + e.getMessage());
// return ResponseEntity.status(500).body(result);
// }
// }

// /**
// * Get CoordinatorTool configuration information
// */
// @GetMapping("/config")
// public ResponseEntity<Map<String, Object>> getCoordinatorToolConfig() {
// Map<String, Object> config = new HashMap<>();
// try {
// config.put("enabled", coordinatorProperties.isEnabled());
// config.put("success", true);
// return ResponseEntity.ok(config);
// }
// catch (Exception e) {
// config.put("success", false);
// config.put("message", "Failed to get config: " + e.getMessage());
// return ResponseEntity.status(500).body(config);
// }
// }

// /**
// * Get all unique endpoints
// */
// @GetMapping("/endpoints")
// public ResponseEntity<List<String>> getAllEndpoints() {
// try {
// List<String> endpoints = coordinatorToolRepository.findAllUniqueEndpoints();
// return ResponseEntity.ok(endpoints);
// }
// catch (Exception e) {
// log.error("Error getting endpoints: {}", e.getMessage(), e);
// return ResponseEntity.status(500).build();
// }
// }

// /**
// * Delete coordinator tool
// */
// @DeleteMapping("/{id}")
// public ResponseEntity<Map<String, Object>> deleteCoordinatorTool(@PathVariable("id")
// Long id) {
// Map<String, Object> result = new HashMap<>();

// try {
// log.info("Deleting coordinator tool with ID: {}", id);

// // 1. First check if tool exists
// Optional<CoordinatorToolEntity> toolOptional = coordinatorToolRepository.findById(id);
// if (!toolOptional.isPresent()) {
// result.put("success", false);
// result.put("message", "Coordinator tool not found with ID: " + id);
// return ResponseEntity.notFound().build();
// }

// CoordinatorToolEntity tool = toolOptional.get();

// // 2. If tool is published, unpublish first
// if (CoordinatorToolEntity.PublishStatus.PUBLISHED.equals(tool.getPublishStatus())) {
// log.info("Tool is published, unpublishing first...");
// // Call unpublish logic
// boolean unpublishSuccess = coordinatorService.unpublish(tool.getToolName(),
// tool.getEndpoint());
// if (!unpublishSuccess) {
// result.put("success", false);
// result.put("message", "Failed to unpublish tool before deletion");
// return ResponseEntity.status(500).body(result);
// }
// log.info("Tool unpublished successfully");
// }

// // 3. Delete database record
// coordinatorToolRepository.deleteById(id);
// log.info("Coordinator tool deleted from database");

// result.put("success", true);
// result.put("message", "Coordinator tool deleted successfully");
// return ResponseEntity.ok(result);

// }
// catch (Exception e) {
// log.error("Error deleting coordinator tool: {}", e.getMessage(), e);
// result.put("success", false);
// result.put("message", "Error deleting coordinator tool: " + e.getMessage());
// return ResponseEntity.status(500).body(result);
// }
// }

// }
