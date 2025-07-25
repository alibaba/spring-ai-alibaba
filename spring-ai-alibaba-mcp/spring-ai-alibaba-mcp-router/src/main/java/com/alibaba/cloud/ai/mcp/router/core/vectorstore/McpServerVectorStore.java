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

package com.alibaba.cloud.ai.mcp.router.core.vectorstore;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;

import java.util.List;

/**
 * MCP Server 向量存储接口
 */
public interface McpServerVectorStore {

	/**
	 * 添加服务到向量存储
	 * @param serverInfo 服务信息
	 * @return 是否成功添加
	 */
	boolean addServer(McpServerInfo serverInfo);

	/**
	 * 从向量存储中移除服务
	 * @param serviceName 服务名
	 * @return 是否成功移除
	 */
	boolean removeServer(String serviceName);

	/**
	 * 获取指定服务
	 * @param serviceName 服务名
	 * @return 服务信息
	 */
	McpServerInfo getServer(String serviceName);

	/**
	 * 获取所有服务
	 * @return 服务列表
	 */
	List<McpServerInfo> getAllServers();

	/**
	 * 向量相似度搜索
	 * @param query 查询文本
	 * @param limit 返回数量限制
	 * @return 相似的服务列表
	 */
	List<McpServerInfo> search(String query, int limit);

	/**
	 * 获取存储大小
	 * @return 存储的服务数量
	 */
	int size();

	/**
	 * 清空存储
	 */
	void clear();

}
