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

package com.alibaba.cloud.ai.mcp.router.controller;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.router.service.McpRouterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP Router REST API 控制器
 */
@RestController
@RequestMapping("/api/mcp-router")
public class McpRouterController {

	@Autowired
	private McpRouterManagementService mcpRouterManagementService;

	/**
	 * 初始化 MCP 服务 POST /api/mcp-router/initialize
	 */
	@PostMapping("/initialize")
	public ResponseEntity<Map<String, Object>> initializeServices(@RequestBody List<String> serviceNames) {
		try {
			Boolean result = mcpRouterManagementService.initializeServices(serviceNames);
			return ResponseEntity.ok(Map.of("success", result, "message",
					result ? "Services initialized successfully" : "Failed to initialize services", "serviceCount",
					serviceNames.size()));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 添加 MCP 服务 POST /api/mcp-router/services
	 */
	@PostMapping("/services")
	public ResponseEntity<Map<String, Object>> addService(@RequestParam String serviceName) {
		try {
			Boolean result = mcpRouterManagementService.addService(serviceName);
			return ResponseEntity.ok(Map.of("success", result, "message",
					result ? "Service added successfully" : "Service not found", "serviceName", serviceName));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 移除 MCP 服务 DELETE /api/mcp-router/services/{serviceName}
	 */
	@DeleteMapping("/services/{serviceName}")
	public ResponseEntity<Map<String, Object>> removeService(@PathVariable String serviceName) {
		try {
			Boolean result = mcpRouterManagementService.removeService(serviceName);
			return ResponseEntity.ok(Map.of("success", result, "message",
					result ? "Service removed successfully" : "Service not found", "serviceName", serviceName));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 获取所有 MCP 服务 GET /api/mcp-router/services
	 */
	@GetMapping("/services")
	public ResponseEntity<Map<String, Object>> getAllServices() {
		try {
			List<McpServerInfo> services = mcpRouterManagementService.getAllServices();
			return ResponseEntity.ok(Map.of("success", true, "services", services, "count", services.size()));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 获取指定 MCP 服务 GET /api/mcp-router/services/{serviceName}
	 */
	@GetMapping("/services/{serviceName}")
	public ResponseEntity<Map<String, Object>> getService(@PathVariable String serviceName) {
		try {
			McpServerInfo service = mcpRouterManagementService.getService(serviceName);
			if (service != null) {
				return ResponseEntity.ok(Map.of("success", true, "service", service));
			}
			else {
				return ResponseEntity.notFound().build();
			}
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 搜索 MCP 服务 GET /api/mcp-router/search?query={query}&limit={limit}
	 */
	@GetMapping("/search")
	public ResponseEntity<Map<String, Object>> searchServices(@RequestParam String query,
			@RequestParam(defaultValue = "10") int limit) {
		try {
			List<McpServerInfo> services = mcpRouterManagementService.searchServices(query, limit);
			return ResponseEntity.ok(Map.of("success", true, "query", query, "services", services, "count",
					services.size(), "limit", limit));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 刷新 MCP 服务 PUT /api/mcp-router/services/{serviceName}/refresh
	 */
	@PutMapping("/services/{serviceName}/refresh")
	public ResponseEntity<Map<String, Object>> refreshService(@PathVariable String serviceName) {
		try {
			Boolean result = mcpRouterManagementService.refreshService(serviceName);
			return ResponseEntity.ok(Map.of("success", result, "message",
					result ? "Service refreshed successfully" : "Failed to refresh service", "serviceName",
					serviceName));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 清空所有 MCP 服务 DELETE /api/mcp-router/services
	 */
	@DeleteMapping("/services")
	public ResponseEntity<Map<String, Object>> clearAllServices() {
		try {
			mcpRouterManagementService.clearAllServices();
			return ResponseEntity.ok(Map.of("success", true, "message", "All services cleared successfully"));
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 获取统计信息 GET /api/mcp-router/statistics
	 */
	@GetMapping("/statistics")
	public ResponseEntity<Map<String, Object>> getStatistics() {
		try {
			Map<String, Object> stats = mcpRouterManagementService.getStatistics();
			stats.put("success", true);
			return ResponseEntity.ok(stats);
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
		}
	}

	/**
	 * 健康检查 GET /api/mcp-router/health
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		return ResponseEntity
			.ok(Map.of("status", "UP", "service", "MCP Router", "timestamp", System.currentTimeMillis()));
	}

}
