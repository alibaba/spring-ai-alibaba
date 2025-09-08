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
 * MCP Service Class
 * Responsible for business logic related to MCP services
 *
 * @author Makoto
 * @since 2025/1/24
 */
@Service
public class McpService {

	private static final Logger logger = LoggerFactory.getLogger(McpService.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Retrieves all MCP service information
	 * @return List of MCP service information
	 */
	public List<McpServerInfo> getAllMcpServices() {
		return loadMcpServicesFromConfig();
	}

	/**
	 * Creates service summary information
	 * @param services List of MCP services
	 * @return Summary information
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
	 * Loads MCP service information from configuration files
	 * @return List of MCP service information
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
	 * Parses individual server information
	 * @param agentName Agent name
	 * @param serverNode Server node
	 * @return MCP server information
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
	 * Safely retrieves node text values
	 * @param node Node
	 * @param fieldName Field name
	 * @return Text value, returns null if the node is null or does not exist
	 */
	private String getNodeText(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
	}

}
