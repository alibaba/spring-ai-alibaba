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
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanParamDef;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/coordinator-tools")
@CrossOrigin(origins = "*")
public class CoordinatorToolController {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorToolController.class);

    @Autowired
    private ISubplanToolService subplanToolService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Create coordinator tool
     */
    @PostMapping
    public ResponseEntity<CoordinatorToolVO> createCoordinatorTool(@RequestBody CoordinatorToolVO toolVO) {
        try {
            log.info("Creating coordinator tool: {}", toolVO);

            // Validate required fields
            if (toolVO.getToolName() == null || toolVO.getToolName().trim().isEmpty()) {
                log.error("Tool name is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getToolDescription() == null || toolVO.getToolDescription().trim().isEmpty()) {
                log.error("Tool description is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getPlanTemplateId() == null || toolVO.getPlanTemplateId().trim().isEmpty()) {
                log.error("Plan template ID is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getEndpoint() == null || toolVO.getEndpoint().trim().isEmpty()) {
                log.error("Endpoint is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }

            // Set default values
            if (toolVO.getInputSchema() == null || toolVO.getInputSchema().trim().isEmpty()) {
                toolVO.setInputSchema("[]");
            }
            if (toolVO.getPublishStatus() == null) {
                toolVO.setPublishStatus("UNPUBLISHED");
            }

            // Generate MCP Schema from input schema
            String mcpSchema = generateMcpSchema(toolVO.getInputSchema());
            toolVO.setMcpSchema(mcpSchema);

            // Create SubplanToolDef for tool call registration
            SubplanToolDef subplanToolDef = createSubplanToolDefFromVO(toolVO);
            
            // Register the tool in subplan tool service
            SubplanToolDef registeredTool = subplanToolService.registerSubplanTool(subplanToolDef);
            log.info("Successfully registered subplan tool: {} with ID: {}", registeredTool.getToolName(), registeredTool.getId());

            // Convert SubplanToolDef back to CoordinatorToolVO and return
            CoordinatorToolVO resultVO = convertSubplanToolDefToVO(registeredTool);
            return ResponseEntity.ok(resultVO);

        } catch (Exception e) {
            log.error("Error creating coordinator tool: {}", e.getMessage(), e);
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
            log.info("Updating coordinator tool with ID: {}", id);

            // Validate required fields
            if (toolVO.getToolName() == null || toolVO.getToolName().trim().isEmpty()) {
                log.error("Tool name is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getToolDescription() == null || toolVO.getToolDescription().trim().isEmpty()) {
                log.error("Tool description is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getPlanTemplateId() == null || toolVO.getPlanTemplateId().trim().isEmpty()) {
                log.error("Plan template ID is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }
            if (toolVO.getEndpoint() == null || toolVO.getEndpoint().trim().isEmpty()) {
                log.error("Endpoint is required but was null or empty");
                return ResponseEntity.badRequest().build();
            }

            // Check if tool exists
            SubplanToolDef existingTool = subplanToolService.getByToolName(toolVO.getToolName());
            if (existingTool == null || !existingTool.getId().equals(id)) {
                log.error("Tool not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Set default values
            if (toolVO.getInputSchema() == null || toolVO.getInputSchema().trim().isEmpty()) {
                toolVO.setInputSchema("[]");
            }
            if (toolVO.getPublishStatus() == null) {
                toolVO.setPublishStatus("UNPUBLISHED");
            }

            // Generate MCP Schema from input schema
            String mcpSchema = generateMcpSchema(toolVO.getInputSchema());
            toolVO.setMcpSchema(mcpSchema);

            // Create updated SubplanToolDef
            SubplanToolDef updatedToolDef = createSubplanToolDefFromVO(toolVO);
            updatedToolDef.setId(id); // Set the ID for update

            // Update the tool in subplan tool service
            SubplanToolDef savedTool = subplanToolService.updateSubplanTool(updatedToolDef);
            log.info("Successfully updated subplan tool: {} with ID: {}", savedTool.getToolName(), savedTool.getId());

            // Convert back to VO and return
            CoordinatorToolVO resultVO = convertSubplanToolDefToVO(savedTool);
            return ResponseEntity.ok(resultVO);

        } catch (Exception e) {
            log.error("Error updating coordinator tool: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Publish tool
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishCoordinatorTool(@PathVariable("id") Long id) {
        return null;
    }

    /**
     * Unpublish tool
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Map<String, Object>> unpublishCoordinatorTool(@PathVariable("id") Long id) {
        return null;
    }

    /**
     * Get or create coordinator tool by plan template ID
     */
    @GetMapping("/get-or-new-by-template/{planTemplateId}")
    public ResponseEntity<Map<String, Object>> getOrNewCoordinatorToolsByTemplate(
            @PathVariable("planTemplateId") String planTemplateId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Getting or creating coordinator tool for plan template: {}", planTemplateId);
            
            // Check if tool already exists in SubplanToolDef
            List<SubplanToolDef> existingTools = subplanToolService.getSubplanToolsByTemplate(planTemplateId);
            
            if (!existingTools.isEmpty()) {
                // Tool already exists, return it
                SubplanToolDef existingTool = existingTools.get(0);
                CoordinatorToolVO toolVO = convertSubplanToolDefToVO(existingTool);
                
                result.put("success", true);
                result.put("message", "Found existing coordinator tool");
                result.put("data", toolVO);
                result.put("publishStatus", toolVO.getPublishStatus());
                
                log.info("Found existing tool: {}", existingTool.getToolName());
                return ResponseEntity.ok(result);
            }
            
            // Tool doesn't exist, create a new one with default values
            CoordinatorToolVO newToolVO = createDefaultToolVO(planTemplateId);
            
            result.put("success", true);
            result.put("message", "Created new coordinator tool from plan template");
            result.put("data", newToolVO);
            result.put("publishStatus", newToolVO.getPublishStatus());
            
            log.info("Created new tool for plan template: {}", planTemplateId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting or creating coordinator tool: {}", e.getMessage(), e);
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
            List<SubplanToolDef> allTools = subplanToolService.getAllSubplanTools();
            List<String> endpoints = allTools.stream()
                    .map(SubplanToolDef::getEndpoint)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Found {} unique endpoints", endpoints.size());
            return ResponseEntity.ok(endpoints);
            
        } catch (Exception e) {
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
            
            // Check if tool exists by getting all tools and finding the one with matching ID
            List<SubplanToolDef> allTools = subplanToolService.getAllSubplanTools();
            SubplanToolDef toolToDelete = allTools.stream()
                    .filter(tool -> tool.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            
            if (toolToDelete == null) {
                result.put("success", false);
                result.put("message", "Coordinator tool not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            
            // Delete the tool from subplan tool service
            subplanToolService.deleteSubplanTool(id);
            log.info("Successfully deleted coordinator tool: {} with ID: {}", toolToDelete.getToolName(), id);
            
            result.put("success", true);
            result.put("message", "Coordinator tool deleted successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error deleting coordinator tool: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error deleting coordinator tool: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Generate MCP Schema from input schema
     * @param inputSchema JSON string of input parameters
     * @return MCP Schema JSON string
     */
    private String generateMcpSchema(String inputSchema) {
        try {
            // Parse input schema to extract parameters
            JsonNode inputParams = objectMapper.readTree(inputSchema);
            
            // Create MCP schema structure
            Map<String, Object> mcpSchema = new HashMap<>();
            mcpSchema.put("name", "coordinator_tool");
            mcpSchema.put("description", "Coordinator tool for plan execution");
            
            Map<String, Object> inputSchemaMap = new HashMap<>();
            inputSchemaMap.put("type", "object");
            
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            
            if (inputParams.isArray()) {
                for (JsonNode param : inputParams) {
                    String paramName = param.get("name").asText();
                    String paramType = param.get("type").asText();
                    String paramDescription = param.get("description").asText();
                    
                    Map<String, Object> paramSchema = new HashMap<>();
                    paramSchema.put("type", mapTypeToMcpType(paramType));
                    paramSchema.put("description", paramDescription);
                    
                    properties.put(paramName, paramSchema);
                    
                    if (param.has("required") && param.get("required").asBoolean()) {
                        required.add(paramName);
                    }
                }
            }
            
            inputSchemaMap.put("properties", properties);
            inputSchemaMap.put("required", required);
            mcpSchema.put("inputSchema", inputSchemaMap);
            
            return objectMapper.writeValueAsString(mcpSchema);
            
        } catch (Exception e) {
            log.warn("Failed to generate MCP schema, using default: {}", e.getMessage());
            return "{\"name\":\"coordinator_tool\",\"description\":\"Coordinator tool for plan execution\",\"inputSchema\":{\"type\":\"object\",\"properties\":{},\"required\":[]}}";
        }
    }

    /**
     * Map parameter type to MCP type
     * @param type Parameter type
     * @return MCP type
     */
    private String mapTypeToMcpType(String type) {
        if (type == null) return "string";
        
        switch (type.toLowerCase()) {
            case "string":
                return "string";
            case "integer":
            case "int":
                return "number";
            case "number":
            case "float":
            case "double":
                return "number";
            case "boolean":
                return "boolean";
            case "array":
                return "array";
            case "object":
                return "object";
            default:
                return "string";
        }
    }

    /**
     * Create SubplanToolDef from CoordinatorToolVO
     * @param toolVO CoordinatorToolVO
     * @return SubplanToolDef
     */
    private SubplanToolDef createSubplanToolDefFromVO(CoordinatorToolVO toolVO) {
        SubplanToolDef toolDef = new SubplanToolDef();
        toolDef.setToolName(toolVO.getToolName());
        toolDef.setToolDescription(toolVO.getToolDescription());
        toolDef.setPlanTemplateId(toolVO.getPlanTemplateId());
        toolDef.setEndpoint(toolVO.getEndpoint());
        toolDef.setServiceGroup("coordinator-tools");

        // Parse input schema and create parameters
        try {
            JsonNode inputParams = objectMapper.readTree(toolVO.getInputSchema());
            List<SubplanParamDef> parameters = new ArrayList<>();
            
            if (inputParams.isArray()) {
                for (JsonNode param : inputParams) {
                    String paramName = param.get("name").asText();
                    String paramType = param.get("type").asText();
                    String paramDescription = param.get("description").asText();
                    boolean required = param.has("required") ? param.get("required").asBoolean() : true;
                    
                    SubplanParamDef paramDef = new SubplanParamDef(paramName, paramType, paramDescription, required);
                    parameters.add(paramDef);
                }
            }
            
            toolDef.setInputSchema(parameters);
            
        } catch (Exception e) {
            log.warn("Failed to parse input schema for subplan tool: {}", e.getMessage());
            toolDef.setInputSchema(new ArrayList<>());
        }

        return toolDef;
    }

    /**
     * Convert SubplanToolDef to CoordinatorToolVO
     * @param toolDef SubplanToolDef
     * @return CoordinatorToolVO
     */
    private CoordinatorToolVO convertSubplanToolDefToVO(SubplanToolDef toolDef) {
        CoordinatorToolVO vo = new CoordinatorToolVO();
        vo.setId(toolDef.getId());
        vo.setToolName(toolDef.getToolName());
        vo.setToolDescription(toolDef.getToolDescription());
        vo.setPlanTemplateId(toolDef.getPlanTemplateId());
        vo.setEndpoint(toolDef.getEndpoint());
        vo.setPublishStatus("UNPUBLISHED"); // Default status
        
        // Convert parameters back to JSON string
        try {
            List<Map<String, Object>> paramList = new ArrayList<>();
            for (SubplanParamDef param : toolDef.getInputSchema()) {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("name", param.getName());
                paramMap.put("type", param.getType());
                paramMap.put("description", param.getDescription());
                paramMap.put("required", param.isRequired());
                paramList.add(paramMap);
            }
            vo.setInputSchema(objectMapper.writeValueAsString(paramList));
        } catch (Exception e) {
            log.warn("Failed to convert parameters to JSON: {}", e.getMessage());
            vo.setInputSchema("[]");
        }
        
        // Generate MCP Schema
        vo.setMcpSchema(generateMcpSchema(vo.getInputSchema()));
        
        return vo;
    }

    /**
     * Create default CoordinatorToolVO for a plan template
     * @param planTemplateId Plan template ID
     * @return Default CoordinatorToolVO
     */
    private CoordinatorToolVO createDefaultToolVO(String planTemplateId) {
        CoordinatorToolVO toolVO = new CoordinatorToolVO();
        toolVO.setToolName(planTemplateId); // Use plan template ID as tool name
        toolVO.setToolDescription("Tool created from plan template: " + planTemplateId);
        toolVO.setPlanTemplateId(planTemplateId);
        toolVO.setEndpoint("jmanus"); // Default endpoint
        toolVO.setInputSchema("[]"); // Empty parameters by default
        toolVO.setPublishStatus("UNPUBLISHED");
        toolVO.setMcpSchema(generateMcpSchema("[]"));
        
        return toolVO;
    }

}
