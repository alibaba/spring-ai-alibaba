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

package com.alibaba.cloud.ai.example.deepresearch.model.dto;

/**
 * MCP 服务信息数据传输对象
 *
 * @author Makoto
 * @since 2025/1/24
 */
public record McpServerInfo(String agentName, String agentDisplayName, String url, String description, boolean enabled,
		String serviceName) {

	/**
	 * 从 URL 中提取服务名称
	 * @param url MCP 服务器 URL
	 * @return 服务名称
	 */
	public static String extractServiceName(String url) {
		if (url == null || url.isEmpty()) {
			return "未知服务";
		}

		if (url.contains("amap.com")) {
			return "高德地图服务";
		}

		// TODO: 可以在这里添加其他服务的识别逻辑
		return "MCP 服务";
	}

	/**
	 * 获取代理显示名称
	 * @param agentName 代理名称
	 * @return 代理显示名称
	 */
	public static String getAgentDisplayName(String agentName) {
		// TODO: 可以在这里添加其他服务的agent名称映射逻辑
		return switch (agentName) {
			case "coderAgent" -> "编程代理";
			case "researchAgent" -> "研究代理";
			default -> agentName;
		};
	}
}
