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

package com.alibaba.cloud.ai.mcp.gateway.core;

import java.util.List;

/**
 * MCP Gateway 服务注册抽象接口 定义了服务注册、发现、监控等核心功能
 */
public interface McpGatewayServiceRegistry {

	/**
	 * 获取服务详情信息
	 * @param serviceName 服务名称
	 * @return 服务详情信息
	 */
	McpServiceDetail getServiceDetail(String serviceName);

	/**
	 * 获取所有服务名称列表
	 * @return 服务名称列表
	 */
	List<String> getServiceNames();

	/**
	 * 选择服务端点
	 * @param serviceRef 服务引用信息
	 * @return 服务端点信息
	 */
	McpEndpointInfo selectEndpoint(McpServiceDetail.McpServiceRef serviceRef);

	/**
	 * 注册服务变更监听器
	 * @param listener 监听器
	 */
	void registerServiceChangeListener(ServiceChangeListener listener);

	/**
	 * 移除服务变更监听器
	 * @param listener 监听器
	 */
	void removeServiceChangeListener(ServiceChangeListener listener);

	/**
	 * 服务变更监听器接口
	 */
	interface ServiceChangeListener {

		/**
		 * 服务变更回调
		 * @param serviceName 服务名称
		 * @param oldDetail 旧的服务详情
		 * @param newDetail 新的服务详情
		 */
		void onServiceChanged(String serviceName, McpServiceDetail oldDetail, McpServiceDetail newDetail);

	}

}
