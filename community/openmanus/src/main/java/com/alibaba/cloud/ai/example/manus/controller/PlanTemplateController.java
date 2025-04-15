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
package com.alibaba.cloud.ai.example.manus.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

/**
 * 计划模板控制器，处理计划模板页面的API请求
 */
@RestController
@RequestMapping("/api/plan-template")
public class PlanTemplateController {

	private static final Logger logger = LoggerFactory.getLogger(PlanTemplateController.class);

	@Autowired
	@Lazy
	private PlanningFactory planningFactory;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;
	
	@Autowired
	private PlanTemplateService planTemplateService;
	
	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	/**
	 * 生成计划
	 * @param request 包含计划需求的请求和可选的JSON数据
	 * @return 计划的完整JSON数据
	 */
	@PostMapping("/generate")
	public ResponseEntity<Map<String, Object>> generatePlan(@RequestBody Map<String, String> request) {
		String query = request.get("query");
		String existingJson = request.get("existingJson"); // 获取可能存在的JSON数据
		
		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划描述不能为空"));
		}
		
		ExecutionContext context = new ExecutionContext();
		// 如果存在已有JSON数据，将其添加到用户请求中
		String enhancedQuery;
		if (existingJson != null && !existingJson.trim().isEmpty()) {
			// 转义JSON中的花括号，防止被String.format误解为占位符
			String escapedJson = existingJson.replace("{", "\\{").replace("}", "\\}");
			enhancedQuery = String.format("参照过去的执行计划 %s 。以及用户的新的query：%s。构建一个新的执行计划。", escapedJson, query);
		} else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);
		
		// 使用 PlanIdDispatcher 生成唯一的计划模板ID
		String planTemplateId = planIdDispatcher.generatePlanTemplateId();
		context.setPlanId(planTemplateId);
		context.setNeedSummary(false); // 不需要生成摘要，因为我们只需要计划
		
		// 获取规划流程
		PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planTemplateId);
		
		try {
			// 立即执行创建计划的阶段，而不是异步
			planningCoordinator.createPlan(context);
			logger.info("计划生成成功: {}", planTemplateId);
			
			// 从记录器中获取生成的计划
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError().body(Map.of("error", "计划生成失败，无法获取计划数据"));
			}
			
			// 获取计划JSON
			String planJson = context.getPlan().toJson();
			
			// 保存到版本历史
			saveToVersionHistory(planTemplateId, planJson);
			
			// 返回计划数据
			Map<String, Object> response = new HashMap<>();
			response.put("planTemplateId", planTemplateId);
			response.put("status", "completed");
			response.put("planJson", planJson);
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("生成计划失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "计划生成失败: " + e.getMessage()));
		}
	}

	/**
	 * 执行计划
	 * @param request 包含计划ID的请求
	 * @return 结果状态
	 */
	@PostMapping("/execute")
	public ResponseEntity<Map<String, Object>> executePlan(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划ID不能为空"));
		}
		
		// 获取规划流程
		PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planId);
		ExecutionContext context = new ExecutionContext();
		context.setPlanId(planId);
		context.setNeedSummary(true); // 需要生成摘要
		
		// 从记录中获取用户请求
		try {
			context.setUserRequest(planExecutionRecorder.getExecutionRecord(planId).getUserRequest());
		}
		catch (Exception e) {
			logger.error("获取用户请求失败", e);
			context.setUserRequest("执行计划: " + planId);
		}

		// 异步执行任务
		CompletableFuture.runAsync(() -> {
			try {
				// 执行完整的计划流程
				planningCoordinator.executePlan(context);
				logger.info("计划执行成功: {}", planId);
			}
			catch (Exception e) {
				logger.error("执行计划失败", e);
			}
		});

		// 返回任务ID及初始状态
		Map<String, Object> response = new HashMap<>();
		response.put("planId", planId);
		response.put("status", "processing");
		response.put("message", "计划执行请求已提交，正在处理中");

		return ResponseEntity.ok(response);
	}

	/**
	 * 保存版本历史
	 * @param planId 计划ID
	 * @param planJson 计划JSON数据
	 */
	private void saveToVersionHistory(String planId, String planJson) {
		// 从JSON中提取标题
		String title = planTemplateService.extractTitleFromPlan(planJson);
		
		// 检查计划是否存在
		PlanTemplate template = planTemplateService.getPlanTemplate(planId);
		if (template == null) {
			// 如果不存在，则创建新计划
			planTemplateService.savePlanTemplate(planId, title, "用户请求生成计划: " + planId, planJson);
		} else {
			// 如果存在，则保存新版本
			planTemplateService.saveVersionToHistory(planId, planJson);
		}
		
		logger.info("已保存计划 {} 的新版本", planId);
	}
	
	/**
	 * 保存计划
	 * @param request 包含计划ID和JSON的请求
	 * @return 保存结果
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> savePlan(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String planJson = request.get("planJson");
		
		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划ID不能为空"));
		}
		
		if (planJson == null || planJson.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划数据不能为空"));
		}
		
		try {
			// 保存到版本历史
			saveToVersionHistory(planId, planJson);
			
			// 计算版本数量
			List<String> versions = planTemplateService.getPlanVersions(planId);
			int versionCount = versions.size();
			
			// 返回成功响应
			return ResponseEntity.ok(Map.of(
				"status", "success",
				"message", "计划已保存",
				"planId", planId,
				"versionCount", versionCount
			));
		} catch (Exception e) {
			logger.error("保存计划失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "保存计划失败: " + e.getMessage()));
		}
	}
	
	/**
	 * 获取计划的版本历史
	 * @param request 包含计划ID的请求
	 * @return 版本历史列表
	 */
	@PostMapping("/versions")
	public ResponseEntity<Map<String, Object>> getPlanVersions(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		
		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划ID不能为空"));
		}
		
		List<String> versions = planTemplateService.getPlanVersions(planId);
		
		Map<String, Object> response = new HashMap<>();
		response.put("planId", planId);
		response.put("versionCount", versions.size());
		response.put("versions", versions);
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 获取特定版本的计划
	 * @param request 包含计划ID和版本索引的请求
	 * @return 特定版本的计划
	 */
	@PostMapping("/get-version")
	public ResponseEntity<Map<String, Object>> getVersionPlan(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String versionIndex = request.get("versionIndex");
		
		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划ID不能为空"));
		}
		
		try {
			int index = Integer.parseInt(versionIndex);
			List<String> versions = planTemplateService.getPlanVersions(planId);
			
			if (versions.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			
			if (index < 0 || index >= versions.size()) {
				return ResponseEntity.badRequest().body(Map.of("error", "版本索引超出范围"));
			}
			
			String planJson = planTemplateService.getPlanVersion(planId, index);
			
			if (planJson == null) {
				return ResponseEntity.notFound().build();
			}
			
			Map<String, Object> response = new HashMap<>();
			response.put("planId", planId);
			response.put("versionIndex", index);
			response.put("versionCount", versions.size());
			response.put("planJson", planJson);
			
			return ResponseEntity.ok(response);
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "版本索引必须是数字"));
		} catch (Exception e) {
			logger.error("获取计划版本失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "获取计划版本失败: " + e.getMessage()));
		}
	}
	
	/**
	 * 获取所有计划模板列表
	 * @return 所有计划模板的列表
	 */
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getAllPlanTemplates() {
		try {
			// 使用 repository 的 findAll 方法获取所有计划模板
			List<PlanTemplate> templates = planTemplateRepository.findAll();
			
			// 构造响应数据
			List<Map<String, Object>> templateList = new ArrayList<>();
			for (PlanTemplate template : templates) {
				Map<String, Object> templateData = new HashMap<>();
				templateData.put("id", template.getPlanTemplateId());
				templateData.put("title", template.getTitle());
				templateData.put("description", template.getUserRequest());
				templateData.put("createTime", template.getCreateTime());
				templateData.put("updateTime", template.getUpdateTime());
				templateList.add(templateData);
			}
			
			Map<String, Object> response = new HashMap<>();
			response.put("templates", templateList);
			response.put("count", templateList.size());
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("获取计划模板列表失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "获取计划模板列表失败: " + e.getMessage()));
		}
	}
}
