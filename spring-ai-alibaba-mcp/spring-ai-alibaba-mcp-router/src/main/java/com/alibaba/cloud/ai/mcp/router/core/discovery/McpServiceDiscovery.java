/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.core.discovery;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;

import java.util.List;

/**
 * MCP 服务发现接口
 */
public interface McpServiceDiscovery {

	/**
	 * 获取指定服务信息
	 * @param serviceName 服务名
	 * @return 服务信息
	 */
	McpServerInfo getService(String serviceName);

	/**
	 * 获取所有服务信息
	 * @return 服务列表
	 */
	List<McpServerInfo> getAllServices();

	/**
	 * 搜索服务
	 * @param query 查询条件
	 * @param limit 返回数量限制
	 * @return 匹配的服务列表
	 */
	List<McpServerInfo> searchServices(String query, int limit);

	/**
	 * 刷新服务信息
	 * @param serviceName 服务名
	 * @return 是否成功刷新
	 */
	boolean refreshService(String serviceName);

}
