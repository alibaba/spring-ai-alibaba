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
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Iterator;
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
		ObjectMapper mapper = new ObjectMapper();
		McpConfigEntity mcpConfigEntity = new McpConfigEntity();

		try {
			// 获取连接类型
			if (requestVO.getConnectionType() != null && !requestVO.getConnectionType().isEmpty()) {
				String connectionTypeStr = requestVO.getConnectionType();
				try {
					McpConfigType connectionType = McpConfigType.valueOf(connectionTypeStr);
					mcpConfigEntity.setConnectionType(connectionType);
				}
				catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body("Invalid connectionType: " + connectionTypeStr);
				}
			}
			else {
				return ResponseEntity.badRequest().body("Missing connectionType field");
			}

			// 获取配置JSON内容
			if (requestVO.getConfigJson() == null || requestVO.getConfigJson().isEmpty()) {
				return ResponseEntity.badRequest().body("Missing configJson field");
			}

			// 解析configJson内容
			String configJsonString = requestVO.getConfigJson();
			JsonNode configNode = mapper.readTree(configJsonString);

			// 处理输入为简单文本节点的情况，例如 "mac-shell"
			if (configNode.isTextual()) {
				String serverName = configNode.asText();
				
				// 使用时间戳作为服务器名称
				mcpConfigEntity.setMcpServerName(serverName);
				
				// 对于文本输入，创建一个空的配置
				ObjectMapper tempMapper = new ObjectMapper();
				com.fasterxml.jackson.databind.node.ObjectNode standardConfig = tempMapper.createObjectNode();
				com.fasterxml.jackson.databind.node.ObjectNode mcpServersNode = tempMapper.createObjectNode();
				com.fasterxml.jackson.databind.node.ObjectNode emptyServerConfig = tempMapper.createObjectNode();
				
				// 至少添加一个默认字段，确保它是有效的配置
				emptyServerConfig.put("command", "default_command");
				mcpServersNode.set(serverName, emptyServerConfig);
				standardConfig.set("mcpServers", mcpServersNode);
				
				// 设置连接配置
				mcpConfigEntity.setConnectionConfig(standardConfig.toString());
				
				// 保存配置
				mcpService.addMcpServer(mcpConfigEntity);
			}
			else if (configNode.isObject() && !configNode.has("mcpServers") && configNode.size() == 1) {
				// 获取第一个（也是唯一一个）字段名作为服务器名称
				String serverName = configNode.fieldNames().next();
				JsonNode serverConfig = configNode.get(serverName);
				
				// 检查是否是有效的服务器配置对象
				if (serverConfig.isObject() && (serverConfig.has("command") || serverConfig.has("url"))) {
					// 为服务器创建一个实体
					mcpConfigEntity.setMcpServerName(serverName);
					
					// 创建标准格式的配置
					ObjectMapper tempMapper = new ObjectMapper();
					com.fasterxml.jackson.databind.node.ObjectNode standardConfig = tempMapper.createObjectNode();
					com.fasterxml.jackson.databind.node.ObjectNode mcpServersNode = tempMapper.createObjectNode();
					mcpServersNode.set(serverName, serverConfig);
					standardConfig.set("mcpServers", mcpServersNode);
					
					// 设置连接配置
					mcpConfigEntity.setConnectionConfig(standardConfig.toString());
					
					// 保存配置
					mcpService.addMcpServer(mcpConfigEntity);
				}
				else {
					return ResponseEntity.badRequest().body("Invalid server configuration format for key: " + serverName);
				}
			}
			else if (configNode.has("mcpServers") && configNode.get("mcpServers").isObject()
					&& configNode.get("mcpServers").size() > 0) {
				// 如果是标准MCP配置格式，处理所有服务器
				JsonNode mcpServers = configNode.get("mcpServers");
				int successCount = 0;
				StringBuilder resultMessage = new StringBuilder();

				// 遍历所有服务器配置
				for (Iterator<String> fieldNames = mcpServers.fieldNames(); fieldNames.hasNext();) {
					String serverName = fieldNames.next();

					try {
						// 为每个服务器创建一个实体
						McpConfigEntity serverEntity = new McpConfigEntity();
						serverEntity.setMcpServerName(serverName);
						serverEntity.setConnectionType(mcpConfigEntity.getConnectionType());

						// 创建该服务器的配置
						ObjectMapper tempMapper = new ObjectMapper();
						com.fasterxml.jackson.databind.node.ObjectNode serverConfig = tempMapper.createObjectNode();
						serverConfig.set("mcpServers",
								tempMapper.createObjectNode().set(serverName, mcpServers.get(serverName)));

						// 设置连接配置
						serverEntity.setConnectionConfig(serverConfig.toString());

						// 保存配置
						mcpService.addMcpServer(serverEntity);
						successCount++;
					}
					catch (Exception e) {
						resultMessage.append("Failed to add server '")
							.append(serverName)
							.append("': ")
							.append(e.getMessage())
							.append("; ");
					}
				}

				if (successCount > 0) {
					String message = "Successfully added " + successCount + " server(s)";
					if (resultMessage.length() > 0) {
						message += ". " + resultMessage.toString();
					}
					return ResponseEntity.ok(message);
				}
				else {
					return ResponseEntity.badRequest().body("Failed to add any servers: " + resultMessage.toString());
				}
			}
			else {
				// 如果都没有，使用时间戳作为服务器名称
				mcpConfigEntity.setMcpServerName("mcp-server-" + System.currentTimeMillis());

				// 设置连接配置
				mcpConfigEntity.setConnectionConfig(configJsonString);

				// 保存配置
				mcpService.addMcpServer(mcpConfigEntity);
			}

			return ResponseEntity.ok("Success");
		}
		catch (Exception e) {
			logger.error("Error adding MCP server: ", e);
			return ResponseEntity.badRequest().body("Invalid JSON format: " + e.getMessage());
		}
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
