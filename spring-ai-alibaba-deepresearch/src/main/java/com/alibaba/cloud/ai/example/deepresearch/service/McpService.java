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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.McpServerInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 服务类 负责 MCP 服务相关的业务逻辑
 *
 * @author Makoto
 * @since 2025/1/24
 */
@Service
public class McpService {

	private static final Logger logger = LoggerFactory.getLogger(McpService.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 获取所有 MCP 服务信息
	 * @return MCP 服务信息列表
	 */
	public List<McpServerInfo> getAllMcpServices() {
		return loadMcpServicesFromConfig();
	}

	/**
	 * 创建服务摘要信息
	 * @param services MCP 服务列表
	 * @return 摘要信息
	 */
	public Map<String, Object> createServiceSummary(List<McpServerInfo> services) {
		Map<String, Object> summary = new HashMap<>();

		long totalServices = services.size();
		long enabledServices = services.stream().filter(McpServerInfo::enabled).count();
		long disabledServices = totalServices - enabledServices;

		List<String> availableServices = services.stream()
			.filter(McpServerInfo::enabled)
			.map(McpServerInfo::serviceName)
			.distinct()
			.toList();

		summary.put("totalServices", totalServices);
		summary.put("enabledServices", enabledServices);
		summary.put("disabledServices", disabledServices);
		summary.put("availableServices", availableServices);

		return summary;
	}

	/**
	 * 从配置文件加载 MCP 服务信息
	 * @return MCP 服务信息列表
	 */
	private List<McpServerInfo> loadMcpServicesFromConfig() {
		List<McpServerInfo> services = new ArrayList<>();

		ClassPathResource resource = new ClassPathResource("mcp-config.json");

		try (InputStream inputStream = resource.getInputStream()) {
			JsonNode rootNode = objectMapper.readTree(inputStream);

			rootNode.fieldNames().forEachRemaining(agentName -> {
				JsonNode agentNode = rootNode.get(agentName);
				JsonNode serversNode = agentNode.get("mcp-servers");

				if (serversNode != null && serversNode.isArray()) {
					for (JsonNode serverNode : serversNode) {
						McpServerInfo serverInfo = parseServerInfo(agentName, serverNode);
						if (serverInfo != null) {
							services.add(serverInfo);
						}
					}
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException("读取 MCP 配置文件失败", e);
		}

		return services;
	}

	/**
	 * 解析单个服务器信息
	 * @param agentName 代理名称
	 * @param serverNode 服务器节点
	 * @return MCP 服务器信息
	 */
	private McpServerInfo parseServerInfo(String agentName, JsonNode serverNode) {
		String url = getNodeText(serverNode, "url");
		String description = getNodeText(serverNode, "description");
		boolean enabled = serverNode.has("enabled") && serverNode.get("enabled").asBoolean();
		String serviceName = McpServerInfo.extractServiceName(url);
		String agentDisplayName = McpServerInfo.getAgentDisplayName(agentName);

		return new McpServerInfo(agentName, agentDisplayName, url, description, enabled, serviceName);
	}

	/**
	 * 安全地获取节点文本值
	 * @param node 节点
	 * @param fieldName 字段名
	 * @return 文本值，如果节点为 null 或不存在则返回 null
	 */
	private String getNodeText(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
	}

}
