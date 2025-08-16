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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.mcp.Content;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDeployConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerGetToolsRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.TextContent;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.McpServerStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.McpServerTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.McpServerService;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.McpServerEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.MCPManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.McpServerMapper;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.modelcontextprotocol.spec.McpError;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_APP_WORKSPACE_ID_PREFIX;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_EMPTY_ID;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils.TOOL_TASK_EXECUTOR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.GET_TOOLS_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.MCP_NOT_FOUND;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.TOOL_EXECUTION_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.TOOL_PARAMS_MISSING;

/**
 * Implementation of MCP (Model Context Protocol) Server Service Handles CRUD operations
 * and tool management for MCP servers
 */
@Service
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, McpServerEntity> implements McpServerService {

	/** Manager for handling MCP operations */
	private final MCPManager mcpManager;

	/** Manager for handling Redis cache operations */
	private final RedisManager redisManager;

	public McpServerServiceImpl(MCPManager mcpManager, RedisManager redisManager) {
		this.mcpManager = mcpManager;
		this.redisManager = redisManager;
	}

	/**
	 * Creates a new MCP server with the provided configuration
	 * @param detail Server configuration details
	 * @return Unique server code
	 */
	@Override
	public String createMcp(McpServerDetail detail) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			McpServerEntity entity = new McpServerEntity();
			String serverCode = IdGenerator.idStr();
			entity.setServerCode(serverCode);
			entity.setName(detail.getName());
			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			Result<String> checkResult = mcpManager.processInstallConfig(detail.getDeployConfig(),
					detail.getInstallType());
			if (!checkResult.isSuccess()) {
				throw new Exception(String.valueOf(checkResult.getMessage()));
			}
			entity.setDeployConfig(checkResult.getData());
			entity.setHost(fetchHost(checkResult.getData()));
			entity.setDetailConfig(detail.getDetailConfig());
			entity.setDescription(detail.getDescription());
			entity.setWorkspaceId(context.getWorkspaceId());
			entity.setAccountId(context.getAccountId());
			entity.setStatus(McpServerStatusEnum.Normal.getCode());
			entity.setBizType(detail.getBizType());
			entity.setInstallType(detail.getInstallType());
			entity.setDeployEnv(detail.getDeployEnv());
			entity.setSource(detail.getSource());
			entity.setType(detail.getType() == null ? McpServerTypeEnum.CUSTOMER.name() : detail.getType());
			this.save(entity);
			return serverCode;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.CREATE_MCP_ERROR.toError("fail parse InstallConfig"), e);
		}

	}

	/**
	 * Updates an existing MCP server configuration
	 * @param detail Updated server configuration
	 */
	@Override
	public void updateMcp(McpServerDetail detail) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			String serverCode = detail.getServerCode();
			McpServerEntity entity = getMcpByCode(context.getWorkspaceId(), serverCode, null);
			if (entity == null) {
				throw new BizException(MCP_NOT_FOUND.toError());
			}
			entity.setName(detail.getName());
			entity.setGmtModified(new Date());
			Result<String> checkResult = mcpManager.processInstallConfig(detail.getDeployConfig(),
					detail.getInstallType());
			if (!checkResult.isSuccess()) {
				throw new Exception(String.valueOf(checkResult.getMessage()));
			}
			entity.setDeployConfig(checkResult.getData());
			entity.setHost(fetchHost(checkResult.getData()));
			entity.setDetailConfig(detail.getDetailConfig());
			entity.setDescription(detail.getDescription());
			entity.setStatus(detail.getStatus());
			entity.setBizType(detail.getBizType());
			entity.setInstallType(detail.getInstallType());
			entity.setDeployEnv(detail.getDeployEnv());
			entity.setSource(detail.getSource());
			entity.setType(detail.getType() == null ? McpServerTypeEnum.CUSTOMER.name() : detail.getType());
			this.updateById(entity);
			String key = getMcpCacheKey(context.getWorkspaceId(), serverCode);
			redisManager.put(key, entity);
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.UPDATE_MCP_ERROR.toError("fail parse InstallConfig"), e);
		}

	}

	/**
	 * Marks an MCP server as deleted
	 * @param serverCode Unique identifier of the server
	 */
	@Override
	public void deleteMcp(String serverCode) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			McpServerEntity entity = getMcpByCode(context.getWorkspaceId(), serverCode, null);
			if (entity == null) {
				throw new BizException(MCP_NOT_FOUND.toError());
			}
			entity.setGmtModified(new Date());
			entity.setStatus(McpServerStatusEnum.Deleted.getCode());
			this.updateById(entity);
			String key = getMcpCacheKey(context.getWorkspaceId(), serverCode);
			redisManager.put(key, entity);

		}
		catch (Exception e) {
			throw new BizException(ErrorCode.DELETE_MCP_ERROR.toError(), e);
		}
	}

	/**
	 * Retrieves MCP server details by server code
	 * @param serverCode Unique identifier of the server
	 * @param needTools Whether to include tool information
	 * @return Server details
	 */
	@Override
	public McpServerDetail getMcp(String serverCode, boolean needTools) {
		RequestContext context = RequestContextHolder.getRequestContext();
		McpServerEntity entity = getMcpByCode(context.getWorkspaceId(), serverCode, null);
		if (entity == null) {
			throw new BizException(MCP_NOT_FOUND.toError());
		}
		McpServerDetail mcpServerDTO = toMcpServerDTO(entity);
		if (needTools) {
			mcpServerDTO.setTools(mcpManager.getTools(entity));
		}
		return mcpServerDTO;
	}

	/**
	 * Retrieves a paginated list of MCP servers
	 * @param query Search criteria and pagination parameters
	 * @return Paginated list of server details
	 */
	@Override
	public PagingList<McpServerDetail> list(McpQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		Page<McpServerEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<McpServerEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(McpServerEntity::getWorkspaceId, context.getWorkspaceId());
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(McpServerEntity::getName, query.getName());
		}
		if (query.getStatus() != null) {
			queryWrapper.eq(McpServerEntity::getStatus, query.getStatus());
		}
		else {
			queryWrapper.ne(McpServerEntity::getStatus, McpServerStatusEnum.Deleted.getCode());
		}
		queryWrapper.orderByDesc(McpServerEntity::getId);
		IPage<McpServerEntity> pageResult = this.page(page, queryWrapper);
		List<McpServerDetail> details = new ArrayList<>();
		if (!CollectionUtils.isEmpty(pageResult.getRecords())) {
			List<McpServerEntity> McpServerEntities = pageResult.getRecords();
			for (McpServerEntity entity : McpServerEntities) {
				McpServerDetail mcpServerDTO = toMcpServerDTO(entity);
				details.add(mcpServerDTO);
			}

			if (query.getNeedTools()) {
				CountDownLatch latch = new CountDownLatch(pageResult.getRecords().size());
				HashMap<String, List<McpTool>> tools = new HashMap<>();
				for (McpServerEntity entity : pageResult.getRecords()) {
					TOOL_TASK_EXECUTOR.submit(() -> {
						try {
							tools.put(entity.getServerCode(), mcpManager.getTools(entity));
						}
						catch (Exception e) {
							LogUtils.error("get tools error,requestId:{}", context.getWorkspaceId(), e);
						}
						finally {
							latch.countDown();
						}
					});
				}
				try {
					latch.await(3, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				for (McpServerDetail detail : details) {
					if (tools.get(detail.getServerCode()) == null) {
						detail.setTools(new ArrayList<>());
					}
					else {
						detail.setTools(tools.get(detail.getServerCode()));
					}
				}
			}

		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), details);
	}

	/**
	 * Retrieves MCP servers by their server codes
	 * @param query Query containing server codes
	 * @return List of server details
	 */
	@Override
	public List<McpServerDetail> listByCodes(McpQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<McpServerEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(McpServerEntity::getWorkspaceId, context.getWorkspaceId());
		if (!CollectionUtils.isEmpty(query.getServerCodes())) {
			queryWrapper.in(McpServerEntity::getServerCode, query.getServerCodes());
		}
		queryWrapper.orderByDesc(McpServerEntity::getGmtModified);
		List<McpServerEntity> McpServerEntities = this.list(queryWrapper);
		List<McpServerDetail> details = new ArrayList<>();
		if (!CollectionUtils.isEmpty(McpServerEntities)) {
			for (McpServerEntity entity : McpServerEntities) {
				McpServerDetail mcpServerDTO = toMcpServerDTO(entity);
				details.add(mcpServerDTO);
			}
			if (query.getNeedTools()) {
				CountDownLatch latch = new CountDownLatch(McpServerEntities.size());
				HashMap<String, List<McpTool>> tools = new HashMap<>();
				for (McpServerEntity entity : McpServerEntities) {
					TOOL_TASK_EXECUTOR.submit(() -> {
						try {
							tools.put(entity.getServerCode(), mcpManager.getTools(entity));
						}
						catch (Exception e) {
							LogUtils.error("get tools error,requestId:{}", context.getWorkspaceId(), e);
						}
						finally {
							latch.countDown();
						}
					});
				}
				try {
					latch.await(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				for (McpServerDetail detail : details) {
					if (tools.get(detail.getServerCode()) == null) {
						detail.setTools(new ArrayList<>());
					}
					else {
						detail.setTools(tools.get(detail.getServerCode()));
					}
				}
			}
		}

		return details;
	}

	/**
	 * Retrieves MCP server entity by workspace and server code
	 * @param workspaceId Workspace identifier
	 * @param serverCode Server's unique code
	 * @param status Optional status filter
	 * @return Server entity
	 */
	@Override
	public McpServerEntity getMcpByCode(String workspaceId, String serverCode, Integer status) {

		String key = getMcpCacheKey(workspaceId, serverCode);
		McpServerEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}
			return entity;
		}
		LambdaQueryWrapper<McpServerEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(McpServerEntity::getServerCode, serverCode).eq(McpServerEntity::getWorkspaceId, workspaceId);
		if (status != null) {
			queryWrapper.eq(McpServerEntity::getStatus, status);
		}
		else {
			queryWrapper.ne(McpServerEntity::getStatus, McpServerStatusEnum.Deleted.getCode());
		}
		Optional<McpServerEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new McpServerEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}
		entity = entityOptional.get();
		redisManager.put(key, entity);
		return entity;

	}

	/**
	 * Retrieves available tools on the specified MCP server
	 * @param request Request containing workspace and server information
	 * @return List of available tools
	 */
	@Override
	public Result<List<McpTool>> getTools(McpServerGetToolsRequest request) {
		try {
			McpServerEntity entity = getMcpByCode(request.getWorkspaceId(), request.getServerCode(), null);
			if (entity == null) {
				return Result.error(MCP_NOT_FOUND);
			}
			List<McpTool> tools = mcpManager.getTools(entity);
			return Result.success(tools);
		}
		catch (Exception ex) {
			LogUtils.error("getTools exception", ex, request.getServerCode());
			return Result.error(GET_TOOLS_ERROR);
		}
	}

	/**
	 * Executes a specific tool on the MCP server
	 * @param request Tool execution request
	 * @return Tool execution response
	 */
	@Override
	public Result<McpServerCallToolResponse> callTool(McpServerCallToolRequest request) {
		try {
			if (StringUtils.isBlank(request.getServerCode()) || StringUtils.isBlank(request.getToolName())
					|| StringUtils.isBlank(request.getWorkspaceId())) {
				return Result.error(TOOL_PARAMS_MISSING);
			}
			McpServerEntity entity = getMcpByCode(request.getWorkspaceId(), request.getServerCode(), null);
			if (entity == null) {
				return Result.error(MCP_NOT_FOUND);
			}
			return Result.success(mcpManager.callTool(request, entity));
		}
		catch (Exception ex) {
			LogUtils.monitor("McpService", "callTool", 0L, FAIL, request, ex.getMessage(), ex);
			LogUtils.error("callTool exception", ex, request);
			if (ex instanceof McpError) {
				McpServerCallToolResponse errorResponse = new McpServerCallToolResponse();
				errorResponse.setIsError(true);
				TextContent content = new TextContent();
				content.setText(ex.getMessage());
				List<Content> contentList = new ArrayList<>();
				contentList.add(content);
				errorResponse.setContent(contentList);
				return Result.success(errorResponse);
			}
			return Result.error(TOOL_EXECUTION_ERROR);
		}
	}

	/**
	 * Converts entity to DTO
	 * @param entity Server entity
	 * @return Server detail DTO
	 */
	private McpServerDetail toMcpServerDTO(McpServerEntity entity) {
		if (entity == null) {
			return null;
		}
		McpServerDetail detail = new McpServerDetail();
		detail.setServerCode(entity.getServerCode());
		detail.setName(entity.getName());
		detail.setDeployConfig(fetchDeployConfig(entity.getDeployConfig()));
		detail.setDetailConfig(entity.getDetailConfig());
		detail.setStatus(entity.getStatus());
		detail.setType(entity.getType());
		detail.setBizType(entity.getBizType());
		detail.setDescription(entity.getDescription());
		detail.setInstallType(entity.getInstallType());
		detail.setDeployEnv(entity.getDeployEnv());
		detail.setSource(entity.getSource());
		detail.setGmtModified(entity.getGmtModified());
		return detail;
	}

	/**
	 * Extracts deployment configuration from JSON
	 * @param data JSON configuration string
	 * @return Installation configuration
	 */
	private String fetchDeployConfig(String data) {
		McpServerDeployConfig deployConfig = JsonUtils.fromJson(data, McpServerDeployConfig.class);
		return deployConfig.getInstallConfig();
	}

	/**
	 * Generates cache key for MCP server
	 * @param workspaceId Workspace identifier
	 * @param serverCode Server's unique code
	 * @return Cache key
	 */
	public static String getMcpCacheKey(String workspaceId, String serverCode) {
		return String.format(CACHE_APP_WORKSPACE_ID_PREFIX, workspaceId, serverCode);
	}

	/**
	 * Extracts host address from deployment configuration
	 * @param data JSON configuration string
	 * @return Remote host address
	 */
	private String fetchHost(String data) {
		McpServerDeployConfig deployConfig = JsonUtils.fromJson(data, McpServerDeployConfig.class);
		return deployConfig.getRemoteAddress();
	}

}
