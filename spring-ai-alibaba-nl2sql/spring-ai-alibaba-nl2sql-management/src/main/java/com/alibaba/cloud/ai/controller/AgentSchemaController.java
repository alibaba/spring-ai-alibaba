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

import com.alibaba.cloud.ai.service.AgentVectorService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体Schema初始化控制器 处理智能体的数据库Schema初始化到向量存储
 */
@Controller
@RequestMapping("/api/agent/{agentId}/schema")
@CrossOrigin(origins = "*")
public class AgentSchemaController {

	private static final Logger log = LoggerFactory.getLogger(AgentSchemaController.class);

	private final AgentVectorService agentVectorService;

	private final Nl2sqlForGraphController nl2sqlForGraphController;

	public AgentSchemaController(AgentVectorService agentVectorService,
			Nl2sqlForGraphController nl2sqlForGraphController) {
		this.agentVectorService = agentVectorService;
		this.nl2sqlForGraphController = nl2sqlForGraphController;
	}

	/**
	 * 为智能体初始化数据库Schema到向量存储 对应前端页面的"初始化信息源"功能
	 */
	@PostMapping("/init")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> initializeSchema(@PathVariable Long agentId,
			@RequestBody Map<String, Object> requestData) {

		Map<String, Object> response = new HashMap<>();

		try {
			log.info("Initializing schema for agent: {}", agentId);

			// 从请求中提取数据源ID和表列表
			Integer datasourceId = null;
			List<String> tables = null;

			// 尝试从不同的请求格式中提取数据
			if (requestData.containsKey("datasourceId")) {
				datasourceId = (Integer) requestData.get("datasourceId");
			}
			else if (requestData.containsKey("dbConfig")) {
				Map<String, Object> dbConfig = (Map<String, Object>) requestData.get("dbConfig");
				if (dbConfig.containsKey("id")) {
					datasourceId = (Integer) dbConfig.get("id");
				}
			}

			if (requestData.containsKey("tables")) {
				tables = (List<String>) requestData.get("tables");
			}

			// 验证请求参数
			if (datasourceId == null) {
				response.put("success", false);
				response.put("message", "数据源ID不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			if (tables == null || tables.isEmpty()) {
				response.put("success", false);
				response.put("message", "表列表不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			// 执行Schema初始化
			Boolean result = agentVectorService.initializeSchemaForAgentWithDatasource(agentId, datasourceId, tables);

			if (result) {
				response.put("success", true);
				response.put("message", "Schema初始化成功");
				response.put("agentId", agentId);
				response.put("tablesCount", tables.size());

				log.info("Successfully initialized schema for agent: {}, tables: {}", agentId, tables.size());

				return ResponseEntity.ok(response);
			}
			else {
				response.put("success", false);
				response.put("message", "Schema初始化失败");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
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
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getVectorStatistics(@PathVariable Long agentId) {
		Map<String, Object> response = new HashMap<>();

		try {
			Map<String, Object> statistics = agentVectorService.getVectorStatistics(agentId);

			response.put("success", true);
			response.put("data", statistics);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
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
	@ResponseBody
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

		}
		catch (Exception e) {
			log.error("Failed to clear vector data for agent: {}", agentId, e);
			response.put("success", false);
			response.put("message", "清空向量数据失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取智能体配置的数据源列表
	 */
	@GetMapping("/datasources")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getAgentDatasources(@PathVariable Long agentId) {
		Map<String, Object> response = new HashMap<>();

		try {
			log.info("Getting datasources for agent: {}", agentId);

			List<Map<String, Object>> datasources = agentVectorService.getAgentDatasources(agentId);

			response.put("success", true);
			response.put("data", datasources);
			response.put("agentId", agentId);

			log.info("Successfully retrieved {} datasources for agent: {}", datasources.size(), agentId);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Failed to get datasources for agent: {}", agentId, e);
			response.put("success", false);
			response.put("message", "获取数据源失败：" + e.getMessage());
			response.put("data", new ArrayList<>());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取数据源的表列表
	 */
	@GetMapping("/datasources/{datasourceId}/tables")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getDatasourceTables(@PathVariable Integer datasourceId) {
		Map<String, Object> response = new HashMap<>();

		try {
			log.info("Getting tables for datasource: {}", datasourceId);

			List<String> tables = agentVectorService.getDatasourceTables(datasourceId);

			response.put("success", true);
			response.put("data", tables);
			response.put("datasourceId", datasourceId);

			log.info("Successfully retrieved {} tables for datasource: {}", tables.size(), datasourceId);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("Failed to get tables for datasource: {}", datasourceId, e);
			response.put("success", false);
			response.put("message", "获取表列表失败：" + e.getMessage());
			response.put("data", new ArrayList<>());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 智能体聊天接口 - 流式响应 直接调用Nl2sqlForGraphController的streamSearch方法
	 */
	@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@ResponseBody
	public Flux<ServerSentEvent<String>> agentChat(@PathVariable Long agentId,
			@RequestBody Map<String, Object> requestData, HttpServletResponse response) {

		try {
			String query = (String) requestData.get("query");
			if (query == null || query.trim().isEmpty()) {
				return Flux.just(ServerSentEvent.builder("{\"success\": false, \"message\": \"查询内容不能为空\"}")
					.event("error")
					.build());
			}

			log.info("Agent {} chat request: {}", agentId, query);

			// 直接调用Nl2sqlForGraphController的streamSearch方法
			return nl2sqlForGraphController.streamSearch(query.trim(), String.valueOf(agentId), response);

		}
		catch (Exception e) {
			log.error("Failed to process chat request for agent: {}", agentId, e);
			return Flux
				.just(ServerSentEvent.builder("{\"success\": false, \"message\": \"聊天处理失败：" + e.getMessage() + "\"}")
					.event("error")
					.build());
		}
	}

}
