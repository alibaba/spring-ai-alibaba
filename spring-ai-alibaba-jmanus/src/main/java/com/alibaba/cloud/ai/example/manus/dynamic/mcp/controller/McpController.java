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
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpService;
import com.fasterxml.jackson.databind.JsonNode;
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
	 * Add MCP Server
	 * @param requestVO MCP Server Configuration Request VO
	 */
	@PostMapping("/add")
	public ResponseEntity<String> add(@RequestBody McpConfigRequestVO requestVO) throws IOException {
		String configJson = requestVO.getConfigJson();

		// Check if it's short format JSON (without mcpServers wrapper)
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(configJson);

			// If mcpServers field is not included, need to convert to full format
			if (!jsonNode.has("mcpServers")) {
				logger.info("Detected short format JSON, converting to full format");

				// Check if it's simple key-value pair format (without outer braces)
				if (configJson.trim().startsWith("\"") && configJson.contains(":")) {
					// Simple key-value pair format, need to add outer braces
					configJson = "{" + configJson + "}";
				}

				// Create complete configuration format
				StringBuilder fullJsonBuilder = new StringBuilder();
				fullJsonBuilder.append("{\n  \"mcpServers\": ");
				fullJsonBuilder.append(configJson);
				fullJsonBuilder.append("\n}");

				// Update configJson in requestVO
				configJson = fullJsonBuilder.toString();
				requestVO.setConfigJson(configJson);
				logger.info("Converted to full format: {}", configJson);
			}
		}
		catch (Exception e) {
			logger.warn("Error checking JSON format, proceeding with original format", e);
		}

		mcpService.addMcpServer(requestVO);
		return ResponseEntity.ok("success");
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

}
