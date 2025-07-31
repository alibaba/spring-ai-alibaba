/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.AgentVectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体Schema初始化控制器
 * 处理智能体的数据库Schema初始化到向量存储
 */
@RestController
@RequestMapping("/api/agent/{agentId}/schema")
@CrossOrigin(origins = "*")
public class AgentSchemaController {

    private static final Logger log = LoggerFactory.getLogger(AgentSchemaController.class);

    @Autowired
    private AgentVectorService agentVectorService;

    /**
     * 为智能体初始化数据库Schema到向量存储
     * 对应前端页面的"初始化信息源"功能
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initializeSchema(
            @PathVariable Long agentId,
            @RequestBody SchemaInitRequest schemaInitRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Initializing schema for agent: {}", agentId);
            
            // 验证请求参数
            if (schemaInitRequest.getDbConfig() == null) {
                response.put("success", false);
                response.put("message", "数据库配置不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (schemaInitRequest.getTables() == null || schemaInitRequest.getTables().isEmpty()) {
                response.put("success", false);
                response.put("message", "表列表不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 执行Schema初始化
            Boolean result = agentVectorService.initializeSchemaForAgent(agentId, schemaInitRequest);
            
            if (result) {
                response.put("success", true);
                response.put("message", "Schema初始化成功");
                response.put("agentId", agentId);
                response.put("tablesCount", schemaInitRequest.getTables().size());
                
                log.info("Successfully initialized schema for agent: {}, tables: {}", 
                        agentId, schemaInitRequest.getTables().size());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Schema初始化失败");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize schema for agent: {}", agentId, e);
            response.put("success", false);
            response.put("message", "Schema初始化失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取智能体的向量存储统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getVectorStatistics(@PathVariable Long agentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> statistics = agentVectorService.getVectorStatistics(agentId);
            
            response.put("success", true);
            response.put("data", statistics);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get vector statistics for agent: {}", agentId, e);
            response.put("success", false);
            response.put("message", "获取统计信息失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 清空智能体的所有向量数据
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearVectorData(@PathVariable Long agentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Clearing all vector data for agent: {}", agentId);
            
            agentVectorService.deleteAllVectorDataForAgent(agentId);
            
            response.put("success", true);
            response.put("message", "向量数据清空成功");
            response.put("agentId", agentId);
            
            log.info("Successfully cleared all vector data for agent: {}", agentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to clear vector data for agent: {}", agentId, e);
            response.put("success", false);
            response.put("message", "清空向量数据失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}