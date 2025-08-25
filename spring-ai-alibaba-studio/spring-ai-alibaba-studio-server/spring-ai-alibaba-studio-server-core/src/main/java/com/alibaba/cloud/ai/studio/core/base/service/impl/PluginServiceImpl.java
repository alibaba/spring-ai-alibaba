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

import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.PluginStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.PluginType;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolTestStatus;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ApiParameter;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolQuery;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.PluginEntity;
import com.alibaba.cloud.ai.studio.core.base.entity.ToolEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.PluginMapper;
import com.alibaba.cloud.ai.studio.core.base.mapper.ToolMapper;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.api.OpenApiUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.*;

/**
 * Plugin service implementation for managing plugins and tools. Handles CRUD operations
 * for plugins and their associated tools.
 *
 * @since 1.0.0.3
 */
@Service
public class PluginServiceImpl extends ServiceImpl<PluginMapper, PluginEntity> implements PluginService {

	/** Tool data access mapper */
	private final ToolMapper toolMapper;

	/** Redis cache manager */
	private final RedisManager redisManager;

	public PluginServiceImpl(ToolMapper toolMapper, RedisManager redisManager) {
		this.toolMapper = toolMapper;
		this.redisManager = redisManager;
	}

	/**
	 * Creates a new plugin
	 * @param plugin Plugin information
	 * @return Generated plugin ID
	 */
	@Override
	public String createPlugin(Plugin plugin) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();

			// check if plugin name exists
			PluginEntity pluginEntity = getPluginByName(context.getWorkspaceId(), plugin.getName());
			if (pluginEntity != null) {
				throw new BizException(ErrorCode.PLUGIN_NAME_EXISTS.toError());
			}

			String pluginId = IdGenerator.idStr();
			PluginEntity entity = BeanCopierUtils.copy(plugin, PluginEntity.class);
			entity.setPluginId(pluginId);
			entity.setWorkspaceId(context.getWorkspaceId());

			String config = JsonUtils.toJson(plugin.getConfig());
			entity.setConfig(config);
			entity.setType(PluginType.CUSTOM);
			entity.setStatus(PluginStatus.NORMAL);

			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			entity.setCreator(context.getAccountId());
			entity.setModifier(context.getAccountId());

			this.save(entity);

			String key = getPluginCacheKey(entity.getWorkspaceId(), entity.getPluginId());
			redisManager.put(key, entity);

			return pluginId;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.CREATE_PLUGIN_ERROR.toError(), e);
		}
	}

	/**
	 * Updates an existing plugin
	 * @param plugin Updated plugin information
	 */
	@Override
	public void updatePlugin(Plugin plugin) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();

			PluginEntity entity = getPluginById(context.getWorkspaceId(), plugin.getPluginId());
			if (entity == null) {
				throw new BizException(ErrorCode.PLUGIN_NOT_FOUND.toError());
			}

			// check if plugin name exists
			PluginEntity pluginEntity = getPluginByName(context.getWorkspaceId(), plugin.getName());
			if (pluginEntity != null && !pluginEntity.getId().equals(entity.getId())) {
				throw new BizException(ErrorCode.PLUGIN_NAME_EXISTS.toError());
			}

			entity.setName(plugin.getName());
			entity.setDescription(plugin.getDescription());

			String config = JsonUtils.toJson(plugin.getConfig());
			entity.setConfig(config);

			entity.setSource(plugin.getSource());
			entity.setGmtModified(new Date());
			entity.setModifier(context.getWorkspaceId());

			this.updateById(entity);

			String key = getPluginCacheKey(entity.getWorkspaceId(), entity.getPluginId());
			redisManager.put(key, entity);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.UPDATE_PLUGIN_ERROR.toError(), e);
		}
	}

	/**
	 * Deletes a plugin and its associated tools
	 * @param pluginId ID of the plugin to delete
	 */
	@Override
	public void deletePlugin(String pluginId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		PluginEntity entity = getPluginById(context.getWorkspaceId(), pluginId);
		if (entity == null) {
			return;
		}

		// delete all tools first
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getPluginId, pluginId)
			.eq(ToolEntity::getWorkspaceId, context.getWorkspaceId())
			.ne(ToolEntity::getStatus, ToolStatus.DELETED.getStatus());

		List<ToolEntity> entities = toolMapper.selectList(queryWrapper);
		List<String> keys = new ArrayList<>();
		if (!CollectionUtils.isEmpty(entities)) {
			for (ToolEntity toolEntity : entities) {
				String key = getToolCacheKey(context.getWorkspaceId(), toolEntity.getToolId());
				keys.add(key);
			}
		}
		redisManager.delete(keys);

		LambdaUpdateWrapper<ToolEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(ToolEntity::getPluginId, pluginId)
			.eq(ToolEntity::getWorkspaceId, context.getWorkspaceId())
			.set(ToolEntity::getStatus, ToolStatus.DELETED.getStatus());

		toolMapper.update(updateWrapper);

		// delete from db
		entity.setStatus(PluginStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);

		// delete from cache
		String cacheKey = getPluginCacheKey(context.getWorkspaceId(), entity.getPluginId());
		redisManager.delete(cacheKey);
	}

	/**
	 * Retrieves a plugin by ID
	 * @param pluginId Plugin ID
	 * @return Plugin information
	 */
	@Override
	public Plugin getPlugin(String pluginId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		PluginEntity entity = getPluginById(context.getWorkspaceId(), pluginId);
		if (entity == null) {
			throw new BizException(ErrorCode.PLUGIN_NOT_FOUND.toError());
		}

		return toPluginDTO(entity);
	}

	/**
	 * Lists plugins with pagination
	 * @param query Query parameters including pagination info
	 * @return Paginated list of plugins
	 */
	@Override
	public PagingList<Plugin> listPlugins(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		LambdaQueryWrapper<PluginEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PluginEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.ne(PluginEntity::getStatus, PluginStatus.DELETED.getStatus());
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(PluginEntity::getName, query.getName());
		}
		queryWrapper.orderByDesc(PluginEntity::getId);

		Page<PluginEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<PluginEntity> pageResult = this.page(page, queryWrapper);

		List<Plugin> plugins;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			plugins = new ArrayList<>();
		}
		else {
			plugins = pageResult.getRecords().stream().map(this::toPluginDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), plugins);
	}

	/**
	 * Creates a new tool for a plugin
	 * @param tool Tool information
	 * @return Generated tool ID
	 */
	@Override
	public String createTool(Tool tool) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			Plugin plugin = getPlugin(tool.getPluginId());

			// check if tool name exists
			ToolEntity toolEntity = getToolByName(context.getWorkspaceId(), plugin.getPluginId(), tool.getName());
			if (toolEntity != null) {
				throw new BizException(ErrorCode.TOOL_NAME_EXISTS.toError());
			}

			Tool.ToolConfig config = tool.getConfig();
			List<ApiParameter> inputParams = config.getInputParams();
			if (!CollectionUtils.isEmpty(inputParams)) {
				for (ApiParameter apiParameter : inputParams) {
					String location = apiParameter.getLocation();
					if ("Get".equals(config.getRequestMethod()) && location.equals("Body")) {
						throw new BizException(
								ErrorCode.INVALID_PARAMS.toError("input_params", "Get method not support body params"));
					}
				}
			}

			// convert to swagger yaml
			String yaml = OpenApiUtils.buildOpenAPIYaml(plugin, tool);

			if (StringUtils.isBlank(yaml) || !CollectionUtils.isEmpty(OpenApiUtils.parseOpenAPIObject(yaml))) {
				throw new BizException(ErrorCode.BUILD_TOOL_SCHEMA_ERROR.toError());
			}

			String toolId = IdGenerator.idStr();
			ToolEntity entity = BeanCopierUtils.copy(tool, ToolEntity.class);
			entity.setToolId(toolId);
			entity.setPluginId(tool.getPluginId());
			entity.setWorkspaceId(context.getWorkspaceId());
			entity.setConfig(JsonUtils.toJson(tool.getConfig()));
			entity.setApiSchema(yaml);
			entity.setStatus(ToolStatus.DRAFT);
			entity.setEnabled(false);
			entity.setTestStatus(ToolTestStatus.NOT_TEST);

			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			entity.setCreator(context.getAccountId());
			entity.setModifier(context.getAccountId());

			toolMapper.insert(entity);

			// cache it
			String key = getToolCacheKey(entity.getWorkspaceId(), entity.getToolId());
			redisManager.put(key, entity);

			return toolId;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.CREATE_TOOL_ERROR.toError(), e);
		}
	}

	/**
	 * Updates an existing tool
	 * @param tool Updated tool information
	 */
	@Override
	public void updateTool(Tool tool) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();

			String toolName = tool.getName();
			String pluginId = tool.getPluginId();

			ToolEntity entity = getToolById(context.getWorkspaceId(), tool.getToolId());
			if (entity == null) {
				throw new BizException(ErrorCode.TOOL_NOT_FOUND.toError());
			}

			Plugin plugin = getPlugin(pluginId);

			// check if tool name exists
			ToolEntity toolEntity = getToolByName(context.getWorkspaceId(), plugin.getPluginId(), toolName);
			if (toolEntity != null && !toolEntity.getId().equals(entity.getId())) {
				throw new BizException(ErrorCode.TOOL_NAME_EXISTS.toError());
			}

			Tool.ToolConfig config = tool.getConfig();
			List<ApiParameter> inputParams = config.getInputParams();
			if (!CollectionUtils.isEmpty(inputParams)) {
				for (ApiParameter apiParameter : inputParams) {
					String location = apiParameter.getLocation();
					if ("Get".equals(config.getRequestMethod()) && location.equals("Body")) {
						throw new BizException(
								ErrorCode.INVALID_PARAMS.toError("input_params", "Get method not support body params"));
					}
				}
			}

			// convert to swagger yaml
			String yaml = OpenApiUtils.buildOpenAPIYaml(plugin, tool);

			if (StringUtils.isBlank(yaml) || CollectionUtils.isEmpty(OpenApiUtils.parseOpenAPIObject(yaml))) {
				throw new BizException(ErrorCode.BUILD_TOOL_SCHEMA_ERROR.toError());
			}

			entity.setName(toolName);
			entity.setDescription(tool.getDescription());
			entity.setConfig(JsonUtils.toJson(tool.getConfig()));
			entity.setApiSchema(yaml);
			entity.setGmtModified(new Date());
			entity.setModifier(context.getAccountId());

			if (entity.getStatus() == ToolStatus.PUBLISHED) {
				entity.setStatus(ToolStatus.PUBLISHED_EDITING);
			}

			toolMapper.updateById(entity);

			// cache it
			String key = getToolCacheKey(entity.getWorkspaceId(), entity.getToolId());
			redisManager.put(key, entity);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.UPDATE_TOOL_ERROR.toError(), e);
		}
	}

	/**
	 * Deletes a tool
	 * @param toolId ID of the tool to delete
	 */
	@Override
	public void deleteTool(String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// delete from db
		ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
		if (entity == null) {
			return;
		}

		entity.setStatus(ToolStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		toolMapper.updateById(entity);

		// delete from cache
		String cacheKey = getToolCacheKey(context.getWorkspaceId(), entity.getToolId());
		redisManager.delete(cacheKey);
	}

	/**
	 * Retrieves a tool by ID
	 * @param toolId Tool ID
	 * @return Tool information with associated plugin
	 */
	@Override
	public Tool getTool(String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
		if (entity == null) {
			throw new BizException(ErrorCode.TOOL_NOT_FOUND.toError());
		}
		Tool toolDTO = toToolDTO(entity);
		toolDTO.setPlugin(getPlugin(entity.getPluginId()));
		return toolDTO;
	}

	/**
	 * Lists tools with pagination
	 * @param query Query parameters including pagination info
	 * @return Paginated list of tools
	 */
	@Override
	public PagingList<Tool> listTools(ToolQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, context.getWorkspaceId())
			.eq(ToolEntity::getPluginId, query.getPluginId())
			.ne(ToolEntity::getStatus, ToolStatus.DELETED.getStatus());

		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(ToolEntity::getName, query.getName());
		}

		if (!CollectionUtils.isEmpty(query.getToolIds())) {
			queryWrapper.in(ToolEntity::getId, query.getToolIds());
		}

		queryWrapper.orderByDesc(ToolEntity::getId);

		Page<ToolEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<ToolEntity> pageResult = toolMapper.selectPage(page, queryWrapper);

		List<Tool> tools;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			tools = new ArrayList<>();
		}
		else {
			tools = pageResult.getRecords().stream().map(this::toToolDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), tools);
	}

	/**
	 * Retrieves multiple tools by their IDs
	 * @param toolIds List of tool IDs
	 * @return List of tools with their associated plugins
	 */
	@Override
	public List<Tool> getTools(List<String> toolIds) {
		RequestContext context = RequestContextHolder.getRequestContext();

		List<String> needQueryToolIds = new ArrayList<>();
		List<Tool> tools = new ArrayList<>();
		// from cache first
		for (String toolId : toolIds) {
			ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
			if (entity != null && !CacheConstants.CACHE_EMPTY_ID.equals(entity.getId())) {
				Tool tool = toToolDTO(entity);
				Plugin plugin = getPlugin(entity.getPluginId());
				tool.setPlugin(plugin);

				tools.add(tool);
			}
			else {
				needQueryToolIds.add(toolId);
			}
		}

		if (needQueryToolIds.isEmpty()) {
			return tools;
		}

		ToolQuery toolQuery = ToolQuery.builder().toolIds(needQueryToolIds).size(50).fullFields(true).build();

		PagingList<Tool> toolPagingList = listTools(toolQuery);
		for (Tool tool : toolPagingList.getRecords()) {
			Plugin plugin = getPlugin(tool.getPluginId());
			tool.setPlugin(plugin);

			tools.add(tool);
		}

		return tools;
	}

	/**
	 * Updates the enabled status of a tool
	 * @param toolId Tool ID
	 * @param enabled New enabled status
	 */
	@Override
	public void updateEnableStatus(String toolId, Boolean enabled) {
		RequestContext context = RequestContextHolder.getRequestContext();

		ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
		if (entity == null) {
			throw new BizException(ErrorCode.TOOL_NOT_FOUND.toError());
		}

		entity.setEnabled(enabled);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		toolMapper.updateById(entity);

		// update cache
		String key = getToolCacheKey(context.getWorkspaceId(), toolId);
		redisManager.put(key, entity);
	}

	/**
	 * Updates the test status of a tool
	 * @param toolId Tool ID
	 * @param testStatus New test status
	 */
	@Override
	public void updateTestStatus(String toolId, ToolTestStatus testStatus) {
		RequestContext context = RequestContextHolder.getRequestContext();

		ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
		if (entity == null) {
			throw new BizException(ErrorCode.TOOL_NOT_FOUND.toError());
		}

		entity.setTestStatus(testStatus);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());

		toolMapper.updateById(entity);

		// update cache
		String key = getToolCacheKey(context.getWorkspaceId(), toolId);
		redisManager.put(key, entity);
	}

	/**
	 * Publishes a tool after testing
	 * @param toolId Tool ID
	 */
	@Override
	public void publishTool(String toolId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		ToolEntity entity = getToolById(context.getWorkspaceId(), toolId);
		if (entity == null) {
			throw new BizException(ErrorCode.TOOL_NOT_FOUND.toError());
		}

		if (entity.getTestStatus() != ToolTestStatus.PASSED) {
			throw new BizException(ErrorCode.TOOL_NOT_TESTED.toError());
		}

		entity.setStatus(ToolStatus.PUBLISHED);
		entity.setEnabled(true);
		toolMapper.updateById(entity);

		// update cache
		String key = getToolCacheKey(context.getWorkspaceId(), toolId);
		redisManager.put(key, entity);
	}

	/**
	 * Retrieves a plugin entity from cache or database
	 * @param workspaceId Workspace ID
	 * @param pluginId Plugin ID
	 * @return Plugin entity or null if not found
	 */
	private PluginEntity getPluginById(String workspaceId, String pluginId) {
		String key = getPluginCacheKey(workspaceId, pluginId);

		PluginEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<PluginEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PluginEntity::getPluginId, pluginId)
			.eq(PluginEntity::getWorkspaceId, workspaceId)
			.ne(PluginEntity::getStatus, PluginStatus.DELETED.getStatus());

		Optional<PluginEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new PluginEntity();
			entity.setId(CacheConstants.CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);

			return null;
		}

		entity = entityOptional.get();
		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Retrieves a plugin entity by name
	 * @param workspaceId Workspace ID
	 * @param pluginName Plugin name
	 * @return Plugin entity or null if not found
	 */
	private PluginEntity getPluginByName(String workspaceId, String pluginName) {
		LambdaQueryWrapper<PluginEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PluginEntity::getWorkspaceId, workspaceId)
			.eq(PluginEntity::getName, pluginName)
			.ne(PluginEntity::getStatus, ToolStatus.DELETED.getStatus())
			.last("limit 1");

		return this.getOne(queryWrapper);
	}

	/**
	 * Generates cache key for plugin
	 * @param workspaceId Workspace ID
	 * @param pluginId Plugin ID
	 * @return Cache key string
	 */
	public static String getPluginCacheKey(String workspaceId, String pluginId) {
		return String.format(CACHE_PLUGIN_WORKSPACE_ID_PREFIX, workspaceId, pluginId);
	}

	/**
	 * Converts plugin entity to DTO
	 * @param entity Plugin entity
	 * @return Plugin DTO
	 */
	private Plugin toPluginDTO(PluginEntity entity) {
		if (entity == null) {
			return null;
		}

		Plugin plugin = BeanCopierUtils.copy(entity, Plugin.class);
		String config = entity.getConfig();
		if (StringUtils.isNotBlank(config)) {
			Plugin.PluginConfig pluginConfig = JsonUtils.fromJson(config, Plugin.PluginConfig.class);
			plugin.setConfig(pluginConfig);
		}

		return plugin;
	}

	/**
	 * Retrieves a tool entity from cache or database
	 * @param workspaceId Workspace ID
	 * @param toolId Tool ID
	 * @return Tool entity or null if not found
	 */
	private ToolEntity getToolById(String workspaceId, String toolId) {
		String key = getToolCacheKey(workspaceId, toolId);
		ToolEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getToolId, toolId)
			.eq(ToolEntity::getWorkspaceId, workspaceId)
			.ne(ToolEntity::getStatus, ToolStatus.DELETED.getStatus());

		entity = toolMapper.selectOne(queryWrapper);
		if (entity == null) {
			entity = new ToolEntity();
			entity.setId(CacheConstants.CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Retrieves a tool entity by name
	 * @param workspaceId Workspace ID
	 * @param pluginId Plugin ID
	 * @param toolName Tool name
	 * @return Tool entity or null if not found
	 */
	private ToolEntity getToolByName(String workspaceId, String pluginId, String toolName) {
		LambdaQueryWrapper<ToolEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ToolEntity::getWorkspaceId, workspaceId)
			.eq(ToolEntity::getPluginId, pluginId)
			.eq(ToolEntity::getName, toolName)
			.ne(ToolEntity::getStatus, ToolStatus.DELETED.getStatus())
			.last("limit 1");

		return toolMapper.selectOne(queryWrapper);
	}

	/**
	 * Generates cache key for tool
	 * @param workspaceId Workspace ID
	 * @param toolId Tool ID
	 * @return Cache key string
	 */
	public static String getToolCacheKey(String workspaceId, String toolId) {
		return String.format(CACHE_TOOL_WORKSPACE_ID_PREFIX, workspaceId, toolId);
	}

	/**
	 * Converts tool entity to DTO
	 * @param entity Tool entity
	 * @return Tool DTO
	 */
	private Tool toToolDTO(ToolEntity entity) {
		if (entity == null) {
			return null;
		}

		Tool tool = BeanCopierUtils.copy(entity, Tool.class);
		String config = entity.getConfig();
		if (StringUtils.isNotBlank(config)) {
			Tool.ToolConfig toolConfig = JsonUtils.fromJson(config, Tool.ToolConfig.class);
			tool.setConfig(toolConfig);
		}

		return tool;
	}

}
