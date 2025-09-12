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
package com.alibaba.cloud.ai.manus.mcp.controller;

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpConfigVO;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerRequestVO;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServersRequestVO;
import com.alibaba.cloud.ai.manus.mcp.service.McpService;
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

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * List All MCP Server
	 * @return All MCP Server as VO objects
	 */
	@GetMapping("/list")
	public ResponseEntity<List<McpConfigVO>> list() {
		List<McpConfigEntity> entities = mcpService.getMcpServers();
		List<McpConfigVO> vos = McpConfigVO.fromEntities(entities, objectMapper);
		return ResponseEntity.ok(vos);
	}

	/**
	 * Batch import MCP servers (JSON method)
	 * @param requestVO Batch import request VO
	 */
	@PostMapping("/batch-import")
	public ResponseEntity<String> batchImportMcpServers(@RequestBody McpServersRequestVO requestVO) throws IOException {
		// Validate request data
		if (!requestVO.isValid()) {
			return ResponseEntity.badRequest().body("Invalid JSON format");
		}

		// Standardize JSON format
		String configJson = requestVO.getNormalizedConfigJson();
		logger.info("Batch importing {} MCP servers", requestVO.getServerCount());

		// Use new recommended method directly
		mcpService.saveMcpServers(configJson);
		return ResponseEntity.ok("Successfully imported " + requestVO.getServerCount() + " MCP servers");
	}

	/**
	 * Single MCP server operation (add/update)
	 * @param requestVO Single MCP server request VO
	 */
	@PostMapping("/server")
	public ResponseEntity<String> saveMcpServer(@RequestBody McpServerRequestVO requestVO) throws IOException {
		logger.info("Processing {} operation for server: {}", requestVO.isUpdate() ? "update" : "add",
				requestVO.getMcpServerName());

		try {
			// Use new recommended method directly
			mcpService.saveMcpServer(requestVO);
			return ResponseEntity.ok("MCP server " + (requestVO.isUpdate() ? "updated" : "added") + " successfully");
		}
		catch (IllegalArgumentException e) {
			// Check if it's a duplicate name error
			if (e.getMessage().contains("already exists")) {
				logger.warn("MCP server with name '{}' already exists", requestVO.getMcpServerName());
				return ResponseEntity.badRequest()
					.body("MCP server with name '" + requestVO.getMcpServerName() + "' already exists");
			}
			// Check if it's a not found error
			if (e.getMessage().contains("not found")) {
				logger.error("MCP server not found with id: {}", requestVO.getId(), e);
				return ResponseEntity.notFound().build();
			}
			// Check if it's a validation error
			if (e.getMessage().contains("MCP server configuration validation failed")) {
				logger.warn("Validation failed for MCP server '{}': {}", requestVO.getMcpServerName(), e.getMessage());
				return ResponseEntity.badRequest().body(e.getMessage());
			}
			// Other parameter errors
			logger.error("Invalid argument for MCP server operation: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().body("Invalid argument: " + e.getMessage());
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
