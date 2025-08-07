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
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorToolVO;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorConfigParser;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.example.manus.coordinator.service.CoordinatorService;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateRepository;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateVersionRepository;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplateVersion;
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

	/**
	 * 获取所有协调器工具
	 */
	@GetMapping
	public ResponseEntity<List<CoordinatorToolVO>> getAllCoordinatorTools() {
		List<CoordinatorToolVO> tools = coordinatorToolRepository.findAll()
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(tools);
	}

	/**
	 * 根据ID获取协调器工具
	 */
	@GetMapping("/{id}")
	public ResponseEntity<CoordinatorToolVO> getCoordinatorToolById(@PathVariable("id") Long id) {
		Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
		if (entity.isPresent()) {
			return ResponseEntity.ok(CoordinatorToolVO.fromEntity(entity.get()));
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * 创建协调器工具
	 */
	@PostMapping
	public ResponseEntity<CoordinatorToolVO> createCoordinatorTool(@RequestBody CoordinatorToolVO toolVO) {
		try {
			System.out.println("Received toolVO: " + toolVO);
			
			// 验证必需字段
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
			entity.setId(null); // 确保新创建
			
			// 设置默认值
			if (entity.getInputSchema() == null || entity.getInputSchema().trim().isEmpty()) {
				entity.setInputSchema("[]");
			}
			if (entity.getPublishStatus() == null) {
				entity.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
			}
			
			// 调用CoordinatorConfigParser生成MCP Schema
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
	 * 更新协调器工具
	 */
	@PutMapping("/{id}")
	public ResponseEntity<CoordinatorToolVO> updateCoordinatorTool(@PathVariable("id") Long id, @RequestBody CoordinatorToolVO toolVO) {
		try {
			Optional<CoordinatorToolEntity> existingEntity = coordinatorToolRepository.findById(id);
			if (existingEntity.isPresent()) {
				CoordinatorToolEntity entity = toolVO.toEntity();
				entity.setId(id);
				
				// 调用CoordinatorConfigParser生成MCP Schema
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
	 * 删除协调器工具
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCoordinatorTool(@PathVariable("id") Long id) {
		try {
			if (coordinatorToolRepository.existsById(id)) {
				coordinatorToolRepository.deleteById(id);
				return ResponseEntity.ok().build();
			}
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * 根据工具名称查找
	 */
	@GetMapping("/by-name/{toolName}")
	public ResponseEntity<CoordinatorToolVO> getCoordinatorToolByName(@PathVariable("toolName") String toolName) {
		Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findByToolName(toolName);
		if (entity.isPresent()) {
			return ResponseEntity.ok(CoordinatorToolVO.fromEntity(entity.get()));
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * 根据计划模板ID查找
	 */
	@GetMapping("/by-template/{planTemplateId}")
	public ResponseEntity<List<CoordinatorToolVO>> getCoordinatorToolsByTemplate(@PathVariable("planTemplateId") String planTemplateId) {
		List<CoordinatorToolVO> tools = coordinatorToolRepository.findByPlanTemplateId(planTemplateId)
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(tools);
	}

	/**
	 * 根据endpoint查找
	 */
	@GetMapping("/by-endpoint/{endpoint}")
	public ResponseEntity<CoordinatorToolVO> getCoordinatorToolByEndpoint(@PathVariable("endpoint") String endpoint) {
		Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findByEndpoint(endpoint);
		if (entity.isPresent()) {
			return ResponseEntity.ok(CoordinatorToolVO.fromEntity(entity.get()));
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * 根据发布状态查找
	 */
	@GetMapping("/by-status/{publishStatus}")
	public ResponseEntity<List<CoordinatorToolVO>> getCoordinatorToolsByStatus(@PathVariable("publishStatus") String publishStatus) {
		try {
			CoordinatorToolEntity.PublishStatus status = CoordinatorToolEntity.PublishStatus.valueOf(publishStatus.toUpperCase());
			List<CoordinatorToolVO> tools = coordinatorToolRepository.findByPublishStatus(status)
					.stream()
					.map(CoordinatorToolVO::fromEntity)
					.collect(Collectors.toList());
			return ResponseEntity.ok(tools);
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * 获取所有已发布的工具
	 */
	@GetMapping("/published")
	public ResponseEntity<List<CoordinatorToolVO>> getPublishedCoordinatorTools() {
		List<CoordinatorToolVO> tools = coordinatorToolRepository.findByPublishStatusOrderByCreateTimeDesc(CoordinatorToolEntity.PublishStatus.PUBLISHED)
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(tools);
	}

	/**
	 * 发布工具
	 */
	@PostMapping("/{id}/publish")
	public ResponseEntity<Map<String, Object>> publishCoordinatorTool(@PathVariable("id") Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
			if (entity.isPresent()) {
				CoordinatorToolEntity tool = entity.get();
				
				// 尝试发布到MCP服务器
				boolean publishSuccess = coordinatorService.publishCoordinatorTool(tool);
				
				if (publishSuccess) {
					// MCP发布成功，更新数据库状态为已发布
					tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);
					coordinatorToolRepository.save(tool);
					
					response.put("success", true);
					response.put("message", "Tool has been published successfully to MCP server");
					return ResponseEntity.ok(response);
				} else {
					// MCP发布失败，忽略，不更新数据库状态
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
	 * 取消发布工具
	 */
	@PostMapping("/{id}/unpublish")
	public ResponseEntity<Map<String, Object>> unpublishCoordinatorTool(@PathVariable("id") Long id) {
		Map<String, Object> response = new HashMap<>();
		try {
			Optional<CoordinatorToolEntity> entity = coordinatorToolRepository.findById(id);
			if (entity.isPresent()) {
				CoordinatorToolEntity tool = entity.get();
				tool.setPublishStatus(CoordinatorToolEntity.PublishStatus.UNPUBLISHED);
				coordinatorToolRepository.save(tool);
				response.put("success", true);
				response.put("message", "Tool has been unpublished successfully");
				return ResponseEntity.ok(response);
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
	 * 获取工具统计信息
	 */
	@GetMapping("/stats")
	public ResponseEntity<Map<String, Object>> getCoordinatorToolStats() {
		Map<String, Object> stats = new HashMap<>();
		try {
			long totalCount = coordinatorToolRepository.count();
			long publishedCount = coordinatorToolRepository.countByPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);
			long unpublishedCount = coordinatorToolRepository.countUnpublishedTools();

			stats.put("totalCount", totalCount);
			stats.put("publishedCount", publishedCount);
			stats.put("unpublishedCount", unpublishedCount);
			stats.put("success", true);

			return ResponseEntity.ok(stats);
		}
		catch (Exception e) {
			stats.put("success", false);
			stats.put("message", "Failed to get stats: " + e.getMessage());
			return ResponseEntity.status(500).body(stats);
		}
	}

	/**
	 * 搜索工具（根据名称或描述）
	 */
	@GetMapping("/search")
	public ResponseEntity<List<CoordinatorToolVO>> searchCoordinatorTools(@RequestParam("keyword") String keyword) {
		List<CoordinatorToolVO> nameResults = coordinatorToolRepository.findByToolNameContainingIgnoreCase(keyword)
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());

		List<CoordinatorToolVO> descResults = coordinatorToolRepository.findByToolDescriptionContainingIgnoreCase(keyword)
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());

		// 合并结果并去重
		nameResults.addAll(descResults);
		List<CoordinatorToolVO> uniqueResults = nameResults.stream()
				.distinct()
				.collect(Collectors.toList());

		return ResponseEntity.ok(uniqueResults);
	}

	/**
	 * 获取最近创建的工具
	 */
	@GetMapping("/recent")
	public ResponseEntity<List<CoordinatorToolVO>> getRecentCoordinatorTools() {
		List<CoordinatorToolVO> tools = coordinatorToolRepository.findRecentTools()
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(tools);
	}

	/**
	 * 获取最近更新的工具
	 */
	@GetMapping("/recently-updated")
	public ResponseEntity<List<CoordinatorToolVO>> getRecentlyUpdatedCoordinatorTools() {
		List<CoordinatorToolVO> tools = coordinatorToolRepository.findRecentlyUpdatedTools()
				.stream()
				.map(CoordinatorToolVO::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(tools);
	}

	/**
	 * 获取所有endpoint列表（包含jmanus）
	 */
	@GetMapping("/endpoints")
	public ResponseEntity<List<String>> getAllEndpoints() {
		List<String> endpoints = coordinatorToolRepository.findEndPoint();
		// 在最前面添加"jmanus"
		endpoints.add(0, "jmanus");
		return ResponseEntity.ok(endpoints);
	}

	/**
	 * 根据计划模板ID获取或创建协调器工具
	 */
	@GetMapping("/get-or-new-by-template/{planTemplateId}")
	public ResponseEntity<Map<String, Object>> getOrNewCoordinatorToolsByTemplate(@PathVariable("planTemplateId") String planTemplateId) {
		Map<String, Object> result = new HashMap<>();
		
		try {
			// 1. 先查询coordinator_tools表中是否已存在
			List<CoordinatorToolEntity> existingTools = coordinatorToolRepository.findByPlanTemplateId(planTemplateId);
			
			if (!existingTools.isEmpty()) {
				// 如果已存在，直接返回
				List<CoordinatorToolVO> tools = existingTools.stream()
						.map(CoordinatorToolVO::fromEntity)
						.collect(Collectors.toList());
				
				result.put("success", true);
				result.put("message", "Found existing coordinator tools");
				result.put("data", tools);
				return ResponseEntity.ok(result);
			}
			
			// 2. 如果不存在，查询plan_template表
			PlanTemplate planTemplate = planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
			if (planTemplate == null) {
				result.put("success", false);
				result.put("message", "Plan template not found: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}
			
			// 3. 查询最新版本
			Integer maxVersionIndex = planTemplateVersionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
			if (maxVersionIndex == null) {
				result.put("success", false);
				result.put("message", "No version found for plan template: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}
			
			PlanTemplateVersion latestVersion = planTemplateVersionRepository.findByPlanTemplateIdAndVersionIndex(planTemplateId, maxVersionIndex);
			if (latestVersion == null) {
				result.put("success", false);
				result.put("message", "Latest version not found for plan template: " + planTemplateId);
				return ResponseEntity.notFound().build();
			}
			
			// 4. 转换plan_json为CoordinatorConfigVO
			CoordinatorConfigVO mcpPlanConfig = coordinatorConfigParser.parser(latestVersion.getPlanJson());
			
			// 5. 创建CoordinatorToolVO
			CoordinatorToolVO coordinatorToolVO = new CoordinatorToolVO();
			coordinatorToolVO.setToolName(mcpPlanConfig.getId()); // id = toolName
			coordinatorToolVO.setPlanTemplateId(planTemplateId);
			coordinatorToolVO.setToolDescription(mcpPlanConfig.getDescription()); // description = toolDescription
			
			// 6. 将parameters转换为JSON作为inputSchema
			try {
				String inputSchema = objectMapper.writeValueAsString(mcpPlanConfig.getParameters());
				coordinatorToolVO.setInputSchema(inputSchema);
			} catch (Exception e) {
				coordinatorToolVO.setInputSchema("[]");
			}
			
			// 7. 设置默认值
			coordinatorToolVO.setMcpSchema("{}");
			coordinatorToolVO.setEndpoint("jmanus");
			coordinatorToolVO.setPublishStatus("UNPUBLISHED");
			
			result.put("success", true);
			result.put("message", "Created new coordinator tool from plan template");
			result.put("data", coordinatorToolVO);
			return ResponseEntity.ok(result);
			
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "Error processing request: " + e.getMessage());
			return ResponseEntity.status(500).body(result);
		}
	}

} 