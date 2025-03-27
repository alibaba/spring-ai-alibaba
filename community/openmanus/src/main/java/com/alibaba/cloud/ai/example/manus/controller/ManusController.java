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

import com.alibaba.cloud.ai.example.manus.config.ManusConfiguration.PlanningFlowManager;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.recorder.entity.AgentExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.cloud.ai.example.manus.recorder.entity.ThinkActRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/manus")
public class ManusController {

    @Autowired
    private PlanningFlowManager planningFlowManager;
    
    @Autowired
    private PlanExecutionRecorder planExecutionRecorder;
    
    // 存储正在执行中的任务
    private final Map<String, CompletableFuture<String>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 异步执行 Manus 请求
     * 
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
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return planningFlow.execute(query);
            } catch (Exception e) {
                e.printStackTrace();
                return "执行出错: " + e.getMessage();
            }
        });
        
        // 存储任务以便后续查询
        runningTasks.put(planId, future);
        
        // 返回任务ID及初始状态
        Map<String, Object> response = new HashMap<>();
        response.put("planId", planId);
        response.put("status", "processing");
        response.put("message", "任务已提交，正在处理中");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务执行状态
     * 
     * @param planId 计划ID
     * @return 任务执行状态及结果
     */
    @GetMapping("/status/{planId}")
    public ResponseEntity<Map<String, Object>> getExecutionStatus(@PathVariable String planId) {
        Map<String, Object> response = new HashMap<>();
        
        // 获取执行记录
        PlanExecutionRecord planRecord = planExecutionRecorder.getExecutionRecord(planId);
        
        if (planRecord == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 检查任务是否完成
        CompletableFuture<String> future = runningTasks.get(planId);
        boolean isCompleted = future != null && future.isDone();
        
        response.put("planId", planId);
        response.put("title", planRecord.getTitle());
        response.put("progress", planRecord.getProgress());
        response.put("completed", isCompleted);
        response.put("startTime", planRecord.getStartTime());
        
        if (planRecord.getEndTime() != null) {
            response.put("endTime", planRecord.getEndTime());
        }
        
        if (isCompleted) {
            try {
                String result = future.get();
                response.put("result", result);
                response.put("status", "completed");
                
                // 完成后从运行中任务列表删除
                runningTasks.remove(planId);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("error", e.getMessage());
            }
        } else {
            response.put("status", "processing");
        }

        // 添加步骤信息
        response.put("steps", planRecord.getSteps());
        response.put("stepStatuses", planRecord.getStepStatuses());
        response.put("stepNotes", planRecord.getStepNotes());
        response.put("currentStepIndex", planRecord.getCurrentStepIndex());
        
        // 添加执行细节
        if (planRecord.getAgentExecutionSequence() != null && !planRecord.getAgentExecutionSequence().isEmpty()) {
            response.put("agentExecutions", planRecord.getAgentExecutionSequence());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取详细的执行记录，包括思考-行动记录
     * 
     * @param planId 计划ID
     * @return 完整的执行记录
     */
    @GetMapping("/details/{planId}")
    public ResponseEntity<Map<String, Object>> getExecutionDetails(@PathVariable String planId) {
        PlanExecutionRecord planRecord = planExecutionRecorder.getExecutionRecord(planId);
        
        if (planRecord == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("planRecord", planRecord);
        
        // 获取所有智能体执行记录
        if (planRecord.getAgentExecutionSequence() != null) {
            Map<String, Object> agentRecords = new HashMap<>();
            
            for (AgentExecutionRecord agentRecord : planRecord.getAgentExecutionSequence()) {
                Map<String, Object> agentDetails = new HashMap<>();
                agentDetails.put("id", agentRecord.getId());
                agentDetails.put("agentName", agentRecord.getAgentName());
                agentDetails.put("status", agentRecord.getStatus());
                agentDetails.put("startTime", agentRecord.getStartTime());
                
                if (agentRecord.getEndTime() != null) {
                    agentDetails.put("endTime", agentRecord.getEndTime());
                }
                
                // 添加思考-行动步骤
                if (agentRecord.getThinkActSteps() != null) {
                    agentDetails.put("thinkActSteps", agentRecord.getThinkActSteps());
                }
                
                agentRecords.put("agent_" + agentRecord.getId(), agentDetails);
            }
            
            response.put("agentRecords", agentRecords);
        }
        
        return ResponseEntity.ok(response);
    }
}
