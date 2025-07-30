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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerRequestVO;

/**
 * MCP服务接口（重构后） 定义了MCP服务的核心业务方法
 */
public interface IMcpService {

	/**
	 * 批量保存MCP服务器配置
	 * @param configJson MCP配置JSON字符串
	 * @return 配置实体列表
	 * @throws IOException IO异常
	 */
	List<McpConfigEntity> saveMcpServers(String configJson) throws IOException;

	/**
	 * 保存单个MCP服务器配置
	 * @param requestVO MCP服务器表单请求
	 * @return 配置实体
	 * @throws IOException IO异常
	 */
	McpConfigEntity saveMcpServer(McpServerRequestVO requestVO) throws IOException;

	/**
	 * 删除MCP服务器
	 * @param id MCP服务器ID
	 */
	void removeMcpServer(long id);

	/**
	 * 删除MCP服务器
	 * @param mcpServerName MCP服务器名称
	 */
	void removeMcpServer(String mcpServerName);

	/**
	 * 获取所有MCP服务器配置
	 * @return MCP配置实体列表
	 */
	List<McpConfigEntity> getMcpServers();

	/**
	 * 根据ID查找MCP配置
	 * @param id MCP配置ID
	 * @return 可选的MCP配置实体
	 */
	Optional<McpConfigEntity> findById(Long id);

	/**
	 * 获取MCP服务实体列表
	 * @param planId 计划ID
	 * @return MCP服务实体列表
	 */
	List<McpServiceEntity> getFunctionCallbacks(String planId);

	/**
	 * 关闭指定计划的MCP服务
	 * @param planId 计划ID
	 */
	void close(String planId);

	/**
	 * 更新MCP服务器状态
	 * @param id MCP服务器ID
	 * @param status 目标状态
	 * @return true if updated successfully, false otherwise
	 */
	boolean updateMcpServerStatus(Long id, McpConfigStatus status);

}
