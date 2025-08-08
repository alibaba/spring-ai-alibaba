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

package com.alibaba.cloud.ai.example.manus.coordinator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;
import com.alibaba.cloud.ai.example.manus.coordinator.server.CoordinatorMCPServer;
import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;
import com.alibaba.cloud.ai.example.manus.coordinator.repository.CoordinatorToolRepository;
import com.alibaba.cloud.ai.example.manus.planning.service.PlanTemplateService;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * 协调器服务
 *
 * 负责加载和管理协调器工具，处理业务逻辑
 */
@Service
public class CoordinatorService {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorService.class);

	@Autowired
	private PlanTemplateService planTemplateService;

	@Autowired
	private CoordinatorMCPServer mcpServer;

	@Autowired
	private CoordinatorToolRepository coordinatorToolRepository;

	private final ObjectMapper objectMapper;

	public CoordinatorService() {
		this.objectMapper = new ObjectMapper();
		// 注册JSR310模块以支持LocalDateTime等Java 8时间类型
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	/**
	 * 加载协调器工具
	 * @return 按endpoint分组的协调器工具Map
	 */
	public Map<String, List<CoordinatorTool>> loadCoordinatorTools() {
		log.info("开始加载协调器工具");

		try {
			// 从数据库查询已发布的工具
			List<CoordinatorToolEntity> publishedEntities = coordinatorToolRepository
				.findByPublishStatus(CoordinatorToolEntity.PublishStatus.PUBLISHED);

			log.info("从数据库查询到 {} 个已发布的工具", publishedEntities.size());

			// 转换为CoordinatorTool对象
			List<CoordinatorTool> coordinatorTools = new ArrayList<>();
			for (CoordinatorToolEntity entity : publishedEntities) {
				CoordinatorTool tool = new CoordinatorTool();
				tool.setToolName(entity.getToolName());
				tool.setToolDescription(entity.getToolDescription());
				tool.setToolSchema(entity.getMcpSchema());
				tool.setEndpoint(entity.getEndpoint());
				coordinatorTools.add(tool);
			}

			log.info("成功转换 {} 个协调器工具", coordinatorTools.size());

			// 按endpoint分组
			Map<String, List<CoordinatorTool>> groupedTools = coordinatorTools.stream()
				.collect(Collectors.groupingBy(CoordinatorTool::getEndpoint));

			log.info("成功加载协调器工具，共 {} 个工具，分组为 {} 个endpoint", coordinatorTools.size(), groupedTools.size());

			// 输出每个endpoint的工具信息
			for (Map.Entry<String, List<CoordinatorTool>> entry : groupedTools.entrySet()) {
				log.info("Endpoint: {}, 工具数量: {}", entry.getKey(), entry.getValue().size());
				for (CoordinatorTool tool : entry.getValue()) {
					log.info("  - 工具: {} (描述: {})", tool.getToolName(), tool.getToolDescription());
				}
			}

			return groupedTools;

		}
		catch (Exception e) {
			log.error("加载协调器工具失败: {}", e.getMessage(), e);
			return Map.of();
		}
	}

	/**
	 * 为协调器工具创建工具规范
	 * @param tool 协调器工具
	 * @return 工具规范
	 */
	public McpServerFeatures.SyncToolSpecification createToolSpecification(CoordinatorTool tool) {
		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(io.modelcontextprotocol.spec.McpSchema.Tool.builder()
				.name(tool.getToolName())
				.description(tool.getToolDescription())
				.inputSchema(tool.getToolSchema())
				.build())
			.callHandler((exchange, request) -> invokeTool(request))
			.build();
	}

	/**
	 * 调用协调器工具
	 * @param request 工具调用请求
	 * @return 工具调用结果
	 */
	public CallToolResult invokeTool(CallToolRequest request) {
		try {
			log.debug("调用计划协调工具，参数: {}", request.arguments());
			String resultString = null;
			String toolName = request.name();

			// 将参数转换为JSON字符串
			String rawParam = objectMapper.writeValueAsString(request.arguments());

			log.info("执行计划模板: {}, 参数: {}", toolName, rawParam);

			// 调用计划模板服务
			ResponseEntity<Map<String, Object>> responseEntity = planTemplateService
				.executePlanByTemplateIdInternal(toolName, rawParam);

			// 创建简化的返回结果
			CoordinatorResult response;

			// 处理服务返回结果
			if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
				Map<String, Object> executionResult = responseEntity.getBody();

				// 获取planId和状态
				String planId = (String) executionResult.get("planId");
				String status = (String) executionResult.get("status");
				String message = (String) executionResult.get("message");
				resultString = "计划执行完成，planId: " + planId;
				response = new CoordinatorResult(planId, request.arguments(), 2, "success", resultString);

			}
			else {
				response = new CoordinatorResult(null, request.arguments(), 2, "failed", resultString);
			}

			// 转换为JSON字符串
			String resultJson = objectMapper.writeValueAsString(response);

			log.info("计划模板执行完成: {}, 结果: {}", toolName, resultJson);

			// 直接返回结果字符串，与其他工具保持一致
			return new CallToolResult(List.of(new McpSchema.TextContent(resultJson)), null);

		}
		catch (Exception e) {
			log.error("计划协调工具调用失败: {}", e.getMessage(), e);

			// 创建简化的错误响应
			CoordinatorResult errorResponse = new CoordinatorResult(null, request.arguments(), 2, "error", null);
			try {
				String errorJson = objectMapper.writeValueAsString(errorResponse);
				return new CallToolResult(List.of(new McpSchema.TextContent(errorJson)), null);
			}
			catch (Exception jsonError) {
				return new CallToolResult(List.of(new McpSchema.TextContent("计划协调工具调用失败: " + e.getMessage())), null);
			}
		}
	}

	/**
	 * 发布CoordinatorTool到MCP服务器
	 * @param tool 要发布的协调器工具
	 * @return 是否发布成功
	 */
	public boolean publishCoordinatorTool(CoordinatorTool tool) {
		if (tool == null) {
			log.warn("CoordinatorTool为空，无法发布");
			return false;
		}

		try {
			log.info("开始发布CoordinatorTool: {} 到endpoint: {}", tool.getToolName(), tool.getEndpoint());

			// 调用MCP服务器进行动态注册
			boolean success = mcpServer.registerCoordinatorTool(tool);

			if (success) {
				log.info("成功发布CoordinatorTool: {} 到endpoint: {}", tool.getToolName(), tool.getEndpoint());
			}
			else {
				log.error("发布CoordinatorTool失败: {} 到endpoint: {}", tool.getToolName(), tool.getEndpoint());
			}

			return success;
		}
		catch (Exception e) {
			log.error("发布CoordinatorTool时发生异常: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 发布CoordinatorToolEntity到MCP服务器
	 * @param entity 要发布的协调器工具实体
	 * @return 是否发布成功
	 */
	public boolean publishCoordinatorTool(CoordinatorToolEntity entity) {
		if (entity == null) {
			log.warn("CoordinatorToolEntity为空，无法发布");
			return false;
		}

		try {
			log.info("开始发布CoordinatorToolEntity: {} 到endpoint: {}", entity.getToolName(), entity.getEndpoint());

			CoordinatorTool tool = new CoordinatorTool();
			tool.setToolName(entity.getToolName());
			tool.setToolDescription(entity.getToolDescription());
			tool.setEndpoint(entity.getEndpoint());
			tool.setToolSchema(entity.getMcpSchema());

			// 先尝试刷新现有工具
			boolean refreshSuccess = mcpServer.refreshTool(entity.getToolName(), tool);
			if (refreshSuccess) {
				log.info("成功刷新工具: {} 在MCP服务器中", entity.getToolName());
				return true;
			}

			// 如果刷新失败，则尝试注册新工具
			log.info("工具: {} 不存在，尝试注册新工具", entity.getToolName());
			boolean success = publishCoordinatorTool(tool);

			if (success) {
				log.info("成功发布CoordinatorToolEntity: {} 到endpoint: {}", entity.getToolName(), entity.getEndpoint());
			}
			else {
				log.error("发布CoordinatorToolEntity失败: {} 到endpoint: {}", entity.getToolName(), entity.getEndpoint());
			}

			return success;
		}
		catch (Exception e) {
			log.error("发布CoordinatorToolEntity时发生异常: {}", e.getMessage(), e);
			return false;
		}
	}

}
