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

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.UserInputWaitState;
import com.alibaba.cloud.ai.example.manus.planning.service.UserInputService;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/executor")
public class ManusController {

	private static final Logger logger = LoggerFactory.getLogger(ManusController.class);

	private final ObjectMapper objectMapper;

	@Autowired
	@Lazy
	private PlanningFactory planningFactory;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private UserInputService userInputService;

	@Autowired
	public ManusController(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// Register JavaTimeModule to handle LocalDateTime serialization/deserialization
		this.objectMapper.registerModule(new JavaTimeModule());
		// Ensure pretty printing is disabled by default for compact JSON
		// this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
	}

	/**
	 * 异步执行 Manus 请求
	 * @param request 包含用户查询的请求
	 * @return 任务ID及状态
	 */
	@PostMapping("/execute")
	public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, String> request) {
		String query = request.get("query");
		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "查询内容不能为空"));
		}
		ExecutionContext context = new ExecutionContext();
		context.setUserRequest(query);
		// 使用 PlanIdDispatcher 生成唯一的计划ID
		String planId = planIdDispatcher.generatePlanId();
		context.setPlanId(planId);
		context.setNeedSummary(true);
		// 获取或创建规划流程
		PlanningCoordinator planningFlow = planningFactory.createPlanningCoordinator(planId);

		// 异步执行任务
		CompletableFuture.supplyAsync(() -> {
			try {
				return planningFlow.executePlan(context);
			}
			catch (Exception e) {
				logger.error("执行计划失败", e);
				throw new RuntimeException("执行计划失败: " + e.getMessage(), e);
			}
		});

		// 返回任务ID及初始状态
		Map<String, Object> response = new HashMap<>();
		response.put("planId", planId);
		response.put("status", "processing");
		response.put("message", "任务已提交，正在处理中");

		return ResponseEntity.ok(response);
	}

	/**
	 * 获取详细的执行记录
	 * @param planId 计划ID
	 * @return 执行记录的 JSON 表示
	 */
	@GetMapping("/details/{planId}")
	public synchronized ResponseEntity<?> getExecutionDetails(@PathVariable("planId") String planId) {
		PlanExecutionRecord planRecord = planExecutionRecorder.getExecutionRecord(planId);

		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		// Check for user input wait state and merge it into the plan record
		UserInputWaitState waitState = userInputService.getWaitState(planId);
		if (waitState != null && waitState.isWaiting()) {
			// Assuming PlanExecutionRecord has a method like setUserInputWaitState
			// You will need to add this field and method to your PlanExecutionRecord
			// class
			planRecord.setUserInputWaitState(waitState);
			logger.info("Plan {} is waiting for user input. Merged waitState into details response.", planId);
		}
		else {
			planRecord.setUserInputWaitState(null); // Clear if not waiting
		}

		try {
			// 使用Jackson ObjectMapper将对象转换为JSON字符串
			String jsonResponse = objectMapper.writeValueAsString(planRecord);
			return ResponseEntity.ok(jsonResponse);
		}
		catch (JsonProcessingException e) {
			logger.error("Error serializing PlanExecutionRecord to JSON for planId: {}", planId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error processing request: " + e.getMessage());
		}
	}

	/**
	 * 删除指定计划ID的执行记录
	 * @param planId 计划ID
	 * @return 删除操作的结果
	 */
	@DeleteMapping("/details/{planId}")
	public ResponseEntity<Map<String, String>> removeExecutionDetails(@PathVariable("planId") String planId) {
		PlanExecutionRecord planRecord = planExecutionRecorder.getExecutionRecord(planId);
		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		try {
			planExecutionRecorder.removeExecutionRecord(planId);
			return ResponseEntity.ok(Map.of("message", "执行记录已成功删除", "planId", planId));
		}
		catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", "删除记录失败: " + e.getMessage()));
		}
	}

	/**
	 * Submits user input for a plan that is waiting.
	 * @param planId The ID of the plan.
	 * @param formData The user-submitted form data, expected as Map<String, String>.
	 * @return ResponseEntity indicating success or failure.
	 */
	@PostMapping("/submit-input/{planId}")
	public ResponseEntity<Map<String, Object>> submitUserInput(@PathVariable("planId") String planId,
			@RequestBody Map<String, String> formData) { // Changed formData to
															// Map<String, String>
		try {
			logger.info("Received user input for plan {}: {}", planId, formData);
			boolean success = userInputService.submitUserInputs(planId, formData);
			if (success) {
				return ResponseEntity.ok(Map.of("message", "Input submitted successfully", "planId", planId));
			}
			else {
				// This case might mean the plan was no longer waiting, or input was
				// invalid.
				// UserInputService should ideally throw specific exceptions for clearer
				// error handling.
				logger.warn("Failed to submit user input for plan {}, it might not be waiting or input was invalid.",
						planId);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Failed to submit input. Plan not waiting or input invalid.", "planId",
							planId));
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("Error submitting user input for plan {}: {}", planId, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage(), "planId", planId));
		}
		catch (Exception e) {
			logger.error("Unexpected error submitting user input for plan {}: {}", planId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "An unexpected error occurred.", "planId", planId));
		}
	}

}
