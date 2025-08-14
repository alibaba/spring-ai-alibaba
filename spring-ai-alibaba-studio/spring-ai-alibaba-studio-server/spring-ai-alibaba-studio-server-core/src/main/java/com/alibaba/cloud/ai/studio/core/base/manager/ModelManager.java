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
package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.DataSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.ModelEntity;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.base.mapper.ModelMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model management service for handling model operations
 */
@Slf4j
@Component
public class ModelManager {

	@Resource
	private ProviderManager providerManager;

	@Resource
	private ModelMapper modelMapper;

	/**
	 * Add a new model
	 * @param modelConfigInfo Model configuration information
	 * @return true if model was added successfully
	 */
	public boolean addModel(ModelConfigInfo modelConfigInfo) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// 检查提供商是否存在
		ProviderConfigInfo providerDetail = providerManager.getProviderDetail(modelConfigInfo.getProvider(), false);
		if (providerDetail == null) {
			log.error("提供商[{}]不存在", modelConfigInfo.getProvider());
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider is invalid"));
		}

		QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("model_id", modelConfigInfo.getModelId());
		queryWrapper.eq("provider", modelConfigInfo.getProvider());
		queryWrapper.eq("workspace_id", context.getWorkspaceId());
		ModelEntity existModelEntity = modelMapper.selectOne(queryWrapper);
		if (existModelEntity != null) {
			log.error("模型[{}]已存在", modelConfigInfo.getModelId());
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "model existed"));
		}

		ModelEntity modelEntity = new ModelEntity();
		modelEntity.setWorkspaceId(context.getWorkspaceId());
		modelEntity.setGmtCreate(new Date());
		modelEntity.setGmtModified(new Date());
		modelEntity.setIcon(modelConfigInfo.getIcon());
		modelEntity.setName(modelConfigInfo.getName());
		modelEntity.setProvider(modelConfigInfo.getProvider());
		modelEntity.setSource(DataSourceEnum.custom.name());
		modelEntity.setEnable(true);
		modelEntity.setType(modelConfigInfo.getType());
		modelEntity.setModelId(modelConfigInfo.getModelId());
		modelEntity.setTags(modelConfigInfo.getTags().stream().collect(Collectors.joining(",")));
		int insert = modelMapper.insert(modelEntity);
		return insert > 0;
	}

	/**
	 * Update an existing model
	 * @param modelConfigInfo Model configuration information
	 * @return true if model was updated successfully
	 */
	public boolean updateModel(ModelConfigInfo modelConfigInfo) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// 检查提供商是否存在
		ProviderConfigInfo providerDetail = providerManager.getProviderDetail(modelConfigInfo.getProvider(), false);
		if (providerDetail == null) {
			log.error("提供商[{}]不存在", modelConfigInfo.getProvider());
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider is invalid"));
		}

		// 检查模型是否存在
		QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("provider", providerDetail.getProvider());
		queryWrapper.eq("model_id", modelConfigInfo.getModelId());
		if (StringUtils.isNotBlank(context.getWorkspaceId())) {
			queryWrapper.eq("workspace_id", context.getWorkspaceId());
		}
		ModelEntity existingModel = modelMapper.selectOne(queryWrapper);
		if (existingModel == null) {
			log.error("模型[{}]不存在", modelConfigInfo.getModelId());
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "model not found"));
		}

		// 更新模型信息
		existingModel.setGmtModified(new Date());
		if (StringUtils.isNotBlank(modelConfigInfo.getName())) {
			existingModel.setName(modelConfigInfo.getName());
		}
		if (StringUtils.isNotBlank(modelConfigInfo.getProvider())) {
			existingModel.setProvider(modelConfigInfo.getProvider());
		}
		if (modelConfigInfo.getTags() != null && !modelConfigInfo.getTags().isEmpty()) {
			existingModel.setTags(modelConfigInfo.getTags().stream().collect(Collectors.joining(",")));
		}
		if (StringUtils.isNotBlank(modelConfigInfo.getIcon())) {
			existingModel.setIcon(modelConfigInfo.getIcon());
		}
		if (modelConfigInfo.getEnable() != null) {
			existingModel.setEnable(modelConfigInfo.getEnable());
		}

		int update = modelMapper.updateById(existingModel);
		return update > 0;
	}

	/**
	 * Delete a model
	 * @param provider Model provider
	 * @param modelId Model ID
	 * @return true if model was deleted successfully
	 */
	public boolean deleteModel(String provider, String modelId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// 检查模型是否存在
		QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("model_id", modelId);
		queryWrapper.eq("provider", provider);
		if (StringUtils.isNotBlank(context.getWorkspaceId())) {
			queryWrapper.eq("workspace_id", context.getWorkspaceId());
		}
		ModelEntity existingModel = modelMapper.selectOne(queryWrapper);
		if (existingModel == null) {
			log.error("模型[{}]不存在", modelId);
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "model not found"));
		}

		// 删除模型
		int delete = modelMapper.deleteById(existingModel.getId());
		return delete > 0;
	}

	/**
	 * Query models by provider
	 * @param provider Model provider
	 * @return List of model configurations
	 */
	public List<ModelConfigInfo> queryModels(String provider) {
		RequestContext context = RequestContextHolder.getRequestContext();
		QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("workspace_id", context.getWorkspaceId());
		if (StringUtils.isNotBlank(provider)) {
			queryWrapper.eq("provider", provider);
		}

		List<ModelEntity> modelEntities = modelMapper.selectList(queryWrapper);
		return modelEntities.stream().map(this::convertToModelConfigInfo).collect(Collectors.toList());
	}

	/**
	 * Get model details
	 * @param provider Model provider
	 * @param modelId Model ID
	 * @return Model configuration information
	 */
	public ModelConfigInfo getModelDetail(String provider, String modelId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		try {
			QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("model_id", modelId);
			queryWrapper.eq("provider", provider);
			queryWrapper.eq("workspace_id", context.getWorkspaceId());

			ModelEntity modelEntity = modelMapper.selectOne(queryWrapper);
			if (modelEntity == null) {
				log.error("模型[{}]不存在", modelId);
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "model not found"));
			}

			return convertToModelConfigInfo(modelEntity);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("获取模型详情失败: " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Convert model entity to model configuration info
	 * @param entity Model entity
	 * @return Model configuration information
	 */
	private ModelConfigInfo convertToModelConfigInfo(ModelEntity entity) {
		ModelConfigInfo modelConfigInfo = new ModelConfigInfo();
		modelConfigInfo.setModelId(entity.getModelId());
		modelConfigInfo.setName(entity.getName());
		modelConfigInfo.setProvider(entity.getProvider());
		modelConfigInfo.setIcon(entity.getIcon());
		if (StringUtils.isNotBlank(entity.getTags())) {
			modelConfigInfo.setTags(Arrays.asList(entity.getTags().split(",")));
		}
		else {
			modelConfigInfo.setTags(new ArrayList<>());
		}
		modelConfigInfo.setType(entity.getType());
		modelConfigInfo.setSource(entity.getSource());
		modelConfigInfo.setEnable(entity.getEnable());
		return modelConfigInfo;
	}

}
