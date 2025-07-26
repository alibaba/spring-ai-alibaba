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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.controller;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerFormRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerBatchImportRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(McpController.class);

	@Autowired
	private McpService mcpService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * List All MCP Server
	 * @return All MCP Server as VO objects
	 */
	@GetMapping("/list")
	public ResponseEntity<List<McpConfigVO>> list() {
		List<McpConfigEntity> entities = mcpService.getMcpServers();
		List<McpConfigVO> vos = McpConfigVO.fromEntities(entities);
		return ResponseEntity.ok(vos);
	}

	/**
	 * 批量导入MCP服务器（JSON方式）
	 * @param requestVO 批量导入请求VO
	 */
	@PostMapping("/batch-import")
	public ResponseEntity<String> batchImportMcpServers(@RequestBody McpServerBatchImportRequestVO requestVO)
			throws IOException {
		// 验证请求数据
		if (!requestVO.isValid()) {
			return ResponseEntity.badRequest().body("Invalid JSON format");
		}

		// 标准化JSON格式
		String configJson = requestVO.getNormalizedConfigJson();
		logger.info("Batch importing {} MCP servers", requestVO.getServerCount());

		// 转换为旧的请求格式以兼容现有Service
		McpConfigRequestVO legacyRequestVO = new McpConfigRequestVO();
		legacyRequestVO.setConfigJson(configJson);

		mcpService.addMcpServer(legacyRequestVO);
		return ResponseEntity.ok("Successfully imported " + requestVO.getServerCount() + " MCP servers");
	}

	/**
	 * 单个MCP服务器操作（新增/更新）
	 * @param requestVO 单个MCP服务器请求VO
	 */
	@PostMapping("/server")
	public ResponseEntity<String> saveMcpServer(@RequestBody McpServerFormRequestVO requestVO) throws IOException {
		// 验证请求数据
		if (!requestVO.isValid()) {
			return ResponseEntity.badRequest().body("Invalid request data");
		}

		// 构建完整的JSON配置
		String configJson = requestVO.buildFullConfigJson();
		logger.info("Processing {} operation for server: {}", requestVO.isUpdate() ? "update" : "add",
				requestVO.getMcpServerName());

		// 转换为旧的请求格式以兼容现有Service
		McpConfigRequestVO legacyRequestVO = new McpConfigRequestVO();
		legacyRequestVO.setConfigJson(configJson);

		try {
			if (requestVO.isUpdate()) {
				// 更新操作
				mcpService.updateMcpServer(requestVO.getId(), legacyRequestVO);
				return ResponseEntity.ok("MCP server updated successfully");
			}
			else {
				// 新增操作
				mcpService.addMcpServer(legacyRequestVO);
				return ResponseEntity.ok("MCP server added successfully");
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("MCP server not found with id: {}", requestVO.getId(), e);
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			String errorMessage = "Failed to " + (requestVO.isUpdate() ? "update" : "add") + " MCP server: "
					+ e.getMessage();
			logger.error(errorMessage, e);
			return ResponseEntity.badRequest().body(errorMessage);
		}
	}

	/**
	 * Remove MCP Server
	 * @param id MCP Config ID
	 */
	@GetMapping("/remove")
	public ResponseEntity<String> remove(@RequestParam(name = "id") long id) throws IOException {
		mcpService.removeMcpServer(id);
		return ResponseEntity.ok("Success");
	}

	/**
	 * remove mcp server by name
	 */
	@PostMapping("/remove/{name}")
	public ResponseEntity<String> removeByName(@PathVariable("name") String mcpServerName) throws IOException {
		mcpService.removeMcpServer(mcpServerName);
		return ResponseEntity.ok("Success");
	}

	/**
	 * Enable MCP Server
	 * @param id MCP Server ID
	 */
	@PostMapping("/enable/{id}")
	public ResponseEntity<String> enableMcpServer(@PathVariable("id") Long id) {
		try {
			boolean success = mcpService.enableMcpServer(id);
			if (success) {
				return ResponseEntity.ok("MCP server enabled successfully");
			}
			else {
				return ResponseEntity.badRequest().body("Failed to enable MCP server");
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("MCP server not found with id: {}", id, e);
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			String errorMessage = "Failed to enable MCP server: " + e.getMessage();
			logger.error(errorMessage, e);
			return ResponseEntity.badRequest().body(errorMessage);
		}
	}

	/**
	 * Disable MCP Server
	 * @param id MCP Server ID
	 */
	@PostMapping("/disable/{id}")
	public ResponseEntity<String> disableMcpServer(@PathVariable("id") Long id) {
		try {
			boolean success = mcpService.disableMcpServer(id);
			if (success) {
				return ResponseEntity.ok("MCP server disabled successfully");
			}
			else {
				return ResponseEntity.badRequest().body("Failed to disable MCP server");
			}
		}
		catch (IllegalArgumentException e) {
			logger.error("MCP server not found with id: {}", id, e);
			return ResponseEntity.notFound().build();
		}
		catch (Exception e) {
			String errorMessage = "Failed to disable MCP server: " + e.getMessage();
			logger.error(errorMessage, e);
			return ResponseEntity.badRequest().body(errorMessage);
		}
	}

}
