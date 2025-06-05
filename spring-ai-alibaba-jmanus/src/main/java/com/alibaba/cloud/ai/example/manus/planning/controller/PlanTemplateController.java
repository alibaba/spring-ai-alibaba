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
package com.alibaba.cloud.ai.example.manus.planning.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
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
		}
		else {
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
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planTemplateId, planJson);

			// 返回计划数据
			Map<String, Object> response = new HashMap<>();
			response.put("planTemplateId", planTemplateId);
			response.put("status", "completed");
			response.put("planJson", planJson);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("saveMessage", saveResult.getMessage());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("生成计划失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "计划生成失败: " + e.getMessage()));
		}
	}

	/**
	 * 根据计划模板ID执行计划（POST方法）
	 * @param request 包含计划模板ID的请求
	 * @return 结果状态
	 */
	@PostMapping("/executePlanByTemplateId")
	public ResponseEntity<Map<String, Object>> executePlanByTemplateId(@RequestBody Map<String, String> request) {
		String planTemplateId = request.get("planTemplateId");
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划模板ID不能为空"));
		}

		String rawParam = request.get("rawParam");
		return executePlanByTemplateIdInternal(planTemplateId, rawParam);
	}

	/**
	 * 根据计划模板ID执行计划（GET方法）
	 * @param planTemplateId 计划模板ID
	 * @param allParams 所有URL查询参数
	 * @return 结果状态
	 */
	@GetMapping("/execute/{planTemplateId}")
	public ResponseEntity<Map<String, Object>> executePlanByTemplateIdGet(
			@PathVariable("planTemplateId") String planTemplateId,
			@RequestParam(required = false, name = "allParams") Map<String, String> allParams) {
		if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划模板ID不能为空"));
		}

		logger.info("执行计划模板，ID: {}, 参数: {}", planTemplateId, allParams);
		String rawParam = allParams != null ? allParams.get("rawParam") : null;
		// 如果有URL参数，使用带参数的执行方法
		return executePlanByTemplateIdInternal(planTemplateId, rawParam);
	}

	/**
	 * 执行计划的内部共用方法（带URL参数版本）
	 * @param planTemplateId 计划模板ID
	 * @param rawParam URL查询参数
	 * @return 结果状态
	 */
	private ResponseEntity<Map<String, Object>> executePlanByTemplateIdInternal(String planTemplateId,
			String rawParam) {
		try {
			// 第一步：从存储库中通过planTemplateId获取执行JSON
			PlanTemplate template = planTemplateService.getPlanTemplate(planTemplateId);
			if (template == null) {
				return ResponseEntity.notFound().build();
			}

			// 获取最新版本的计划JSON
			List<String> versions = planTemplateService.getPlanVersions(planTemplateId);
			if (versions.isEmpty()) {
				return ResponseEntity.internalServerError().body(Map.of("error", "计划模板没有可执行的版本"));
			}
			String planJson = planTemplateService.getPlanVersion(planTemplateId, versions.size() - 1);
			if (planJson == null || planJson.trim().isEmpty()) {
				return ResponseEntity.internalServerError().body(Map.of("error", "无法获取计划JSON数据"));
			}

			// 生成新的计划ID，而不是使用模板ID
			String newPlanId = planIdDispatcher.generatePlanId();

			// 获取规划流程，使用新的计划ID
			PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(newPlanId);
			ExecutionContext context = new ExecutionContext();
			context.setPlanId(newPlanId);
			context.setNeedSummary(true); // 需要生成摘要

			try {
				ExecutionPlan plan = ExecutionPlan.fromJson(planJson, newPlanId);

				// 设置URL参数到ExecutionPlan中
				if (rawParam != null && !rawParam.isEmpty()) {
					logger.info("设置执行参数到计划中: {}", rawParam);
					plan.setExecutionParams(rawParam);
				}

				// 设置计划到上下文
				context.setPlan(plan);

				// 从记录中获取用户请求
				context.setUserRequest(template.getTitle());
			}
			catch (Exception e) {
				logger.error("解析计划JSON或获取用户请求失败", e);
				context.setUserRequest("执行计划: " + newPlanId + "\n来自模板: " + planTemplateId);

				// 如果解析失败，记录错误但继续执行流程
				logger.warn("将使用原始JSON继续执行", e);
			}

			// 异步执行任务
			CompletableFuture.runAsync(() -> {
				try {
					// 执行计划的执行和总结步骤，跳过创建计划
					planningCoordinator.executeExistingPlan(context);
					logger.info("计划执行成功: {}", newPlanId);
				}
				catch (Exception e) {
					logger.error("执行计划失败", e);
				}
			});

			// 返回任务ID及初始状态
			Map<String, Object> response = new HashMap<>();
			response.put("planId", newPlanId);
			response.put("status", "processing");
			response.put("message", "计划执行请求已提交，正在处理中");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("执行计划失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "执行计划失败: " + e.getMessage()));
		}
	}

	/**
	 * 保存版本历史
	 * @param planId 计划ID
	 * @param planJson 计划JSON数据
	 * @return 保存结果
	 */
	private PlanTemplateService.VersionSaveResult saveToVersionHistory(String planId, String planJson) {
		// 从JSON中提取标题
		String title = planTemplateService.extractTitleFromPlan(planJson);

		// 检查计划是否存在
		PlanTemplate template = planTemplateService.getPlanTemplate(planId);
		if (template == null) {
			// 如果不存在，则创建新计划
			planTemplateService.savePlanTemplate(planId, title, "用户请求生成计划: " + planId, planJson);
			logger.info("已创建新计划 {} 及其第一个版本", planId);
			return new PlanTemplateService.VersionSaveResult(true, false, "新计划已创建", 0);
		}
		else {
			// 如果存在，则保存新版本
			PlanTemplateService.VersionSaveResult result = planTemplateService.saveToVersionHistory(planId, planJson);
			if (result.isSaved()) {
				logger.info("已保存计划 {} 的新版本 {}", planId, result.getVersionIndex());
			}
			else {
				logger.info("计划 {} 内容相同，未保存新版本", planId);
			}
			return result;
		}
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
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planId, planJson);

			// 计算版本数量
			List<String> versions = planTemplateService.getPlanVersions(planId);
			int versionCount = versions.size();

			// 构建响应
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("planId", planId);
			response.put("versionCount", versionCount);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("message", saveResult.getMessage());
			response.put("versionIndex", saveResult.getVersionIndex());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
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
		}
		catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "版本索引必须是数字"));
		}
		catch (Exception e) {
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
			// 使用 PlanTemplateService 获取所有计划模板
			// 由于没有直接提供获取所有模板的方法，我们使用 PlanTemplateRepository 的 findAll 方法
			List<PlanTemplate> templates = planTemplateService.getAllPlanTemplates();

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
		}
		catch (Exception e) {
			logger.error("获取计划模板列表失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "获取计划模板列表失败: " + e.getMessage()));
		}
	}

	/**
	 * 更新计划模板
	 * @param request 包含计划模板ID、计划需求和可选的JSON数据的请求
	 * @return 更新后的计划JSON数据
	 */
	@PostMapping("/update")
	public ResponseEntity<Map<String, Object>> updatePlanTemplate(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");
		String query = request.get("query");
		String existingJson = request.get("existingJson"); // 获取可能存在的JSON数据

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划模板ID不能为空"));
		}

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划描述不能为空"));
		}

		// 检查计划模板是否存在
		PlanTemplate template = planTemplateService.getPlanTemplate(planId);
		if (template == null) {
			return ResponseEntity.notFound().build();
		}

		ExecutionContext context = new ExecutionContext();
		// 如果存在已有JSON数据，将其添加到用户请求中
		String enhancedQuery;
		if (existingJson != null && !existingJson.trim().isEmpty()) {
			// 转义JSON中的花括号，防止被String.format误解为占位符
			String escapedJson = existingJson.replace("{", "\\{").replace("}", "\\}");
			enhancedQuery = String.format("参照过去的执行计划 %s 。以及用户的新的query：%s。更新这个执行计划。", escapedJson, query);
		}
		else {
			enhancedQuery = query;
		}
		context.setUserRequest(enhancedQuery);

		// 使用已有的计划模板ID
		context.setPlanId(planId);
		context.setNeedSummary(false); // 不需要生成摘要，因为我们只需要计划

		// 获取规划流程
		PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planId);

		try {
			// 立即执行创建计划的阶段，而不是异步
			planningCoordinator.createPlan(context);
			logger.info("计划模板更新成功: {}", planId);

			// 从记录器中获取生成的计划
			if (context.getPlan() == null) {
				return ResponseEntity.internalServerError().body(Map.of("error", "计划更新失败，无法获取计划数据"));
			}

			// 获取计划JSON
			String planJson = context.getPlan().toJson();

			// 保存到版本历史
			PlanTemplateService.VersionSaveResult saveResult = saveToVersionHistory(planId, planJson);

			// 返回计划数据
			Map<String, Object> response = new HashMap<>();
			response.put("planTemplateId", planId);
			response.put("status", "completed");
			response.put("planJson", planJson);
			response.put("saved", saveResult.isSaved());
			response.put("duplicate", saveResult.isDuplicate());
			response.put("saveMessage", saveResult.getMessage());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("更新计划模板失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "计划模板更新失败: " + e.getMessage()));
		}
	}

	/**
	 * 删除计划模板
	 * @param request 包含计划ID的请求
	 * @return 删除结果
	 */
	@PostMapping("/delete")
	public ResponseEntity<Map<String, Object>> deletePlanTemplate(@RequestBody Map<String, String> request) {
		String planId = request.get("planId");

		if (planId == null || planId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "计划ID不能为空"));
		}

		try {
			// 检查计划模板是否存在
			PlanTemplate template = planTemplateService.getPlanTemplate(planId);
			if (template == null) {
				return ResponseEntity.notFound().build();
			}

			// 删除计划模板及其所有版本
			boolean deleted = planTemplateService.deletePlanTemplate(planId);

			if (deleted) {
				logger.info("计划模板删除成功: {}", planId);
				return ResponseEntity.ok(Map.of("status", "success", "message", "计划模板已删除", "planId", planId));
			}
			else {
				logger.error("计划模板删除失败: {}", planId);
				return ResponseEntity.internalServerError().body(Map.of("error", "计划模板删除失败"));
			}
		}
		catch (Exception e) {
			logger.error("删除计划模板失败", e);
			return ResponseEntity.internalServerError().body(Map.of("error", "删除计划模板失败: " + e.getMessage()));
		}
	}

}
