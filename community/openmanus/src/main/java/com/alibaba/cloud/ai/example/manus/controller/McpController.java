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

import com.alibaba.cloud.ai.example.manus.config.entity.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.config.startUp.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

	@Autowired
	private McpService mcpService;

	/**
	 * List All MCP Server
	 * @return All MCP Server
	 */
	@GetMapping("/list")
	public ResponseEntity<List<McpConfigEntity>> list() {
		return ResponseEntity.ok(mcpService.getMcpServers());
	}

	/**
	 * Add MCP Server
	 * @param mcpConfigEntity MCP Server
	 */
	@PostMapping("/add")
	public ResponseEntity<String> add(@RequestBody McpConfigEntity mcpConfigEntity) throws IOException {
		mcpService.addMcpServer(mcpConfigEntity);
		return ResponseEntity.ok("Success");
	}

	/**
	 * Remove MCP Server
	 * @param mcpServerName MCP Server Name
	 */
	@GetMapping("/remove")
	public ResponseEntity<String> remove(@RequestParam("id") long id) throws IOException {
		mcpService.removeMcpServer(id);
		return ResponseEntity.ok("Success");
	}

}
