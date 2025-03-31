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

import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.PlanningFlowManager;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/manus")
public class ManusController {

	@Autowired
	private PlanningFlowManager planningFlowManager;

	@Autowired
	private PlanExecutionRecorder planExecutionRecorder;

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

		// 创建唯一的计划ID
		String planId = "plan_" + System.currentTimeMillis();

		// 获取或创建规划流程
		PlanningFlow planningFlow = planningFlowManager.getOrCreatePlanningFlow(planId);

		// 异步执行任务
		CompletableFuture.supplyAsync(() -> {
			try {
				return planningFlow.execute(query);
			}
			catch (Exception e) {
				e.printStackTrace();
				return "执行出错: " + e.getMessage();
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
	public synchronized ResponseEntity<String> getExecutionDetails(@PathVariable String planId) {
		PlanExecutionRecord planRecord = planExecutionRecorder.getExecutionRecord(planId);

		if (planRecord == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(planRecord.toJson());
	}

}
