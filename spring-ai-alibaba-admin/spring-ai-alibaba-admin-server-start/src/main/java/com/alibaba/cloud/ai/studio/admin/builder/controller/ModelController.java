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

package com.alibaba.cloud.ai.studio.admin.builder.controller;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.core.base.entity.ModelEntity;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelManager;
import com.alibaba.cloud.ai.studio.core.base.manager.ProviderManager;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model Management Controller This controller provides APIs for managing and retrieving
 * model information. It supports: 1. Model selection by type and provider 2. Grouping
 * models by provider 3. Filtering enabled providers and their models
 *
 * @since 1.0.0.3
 */
@Slf4j
@RestController
@Tag(name = "model_function")
@RequestMapping("/console/v1/models")
public class ModelController {

	private final ModelManager modelManager;

	private final ProviderManager providerManager;

	public ModelController(ModelManager modelManager, ProviderManager providerManager) {
		this.modelManager = modelManager;
		this.providerManager = providerManager;
	}

	/**
	 * Model Selector API Retrieves a list of models grouped by their providers for a
	 * specific model type. Only returns models from enabled providers.
	 * @param modelType The type of models to retrieve (e.g., "chat", "embedding")
	 * @return Result containing a list of ModelProviderGroup objects, where each group
	 * contains: - Provider information (ProviderConfigInfo) - List of models
	 * (ModelConfigInfo) for that provider Returns empty list if no models or providers
	 * are found
	 */
	@GetMapping("/{modelType}/selector")
	public Result<List<ModelProviderGroup>> getModelSelector(@PathVariable("modelType") String modelType) {
		try {
			List<ProviderConfigInfo> providers = providerManager.queryProviders(null);
			if (CollectionUtils.isEmpty(providers)) {
				return Result.success(Lists.newArrayList());
			}
			List<ProviderConfigInfo> enableProviders = providers.stream()
				.filter(provider -> BooleanUtils.isTrue(provider.getEnable()))
				.toList();
			if (CollectionUtils.isEmpty(enableProviders)) {
				return Result.success(Lists.newArrayList());
			}
			List<ModelConfigInfo> allModels = modelManager.queryModels(null);
			if (CollectionUtils.isEmpty(allModels)) {
				return Result.success(Lists.newArrayList());
			}
			// group by provider
			Map<String, List<ModelConfigInfo>> groupedModels = allModels.stream()
				.filter(model -> model.getType().equals(modelType))
				.collect(Collectors.groupingBy(ModelConfigInfo::getProvider));
			List<ModelProviderGroup> modelProviderGroups = Lists.newArrayList();
			for (ProviderConfigInfo providerConfig : providers) {
				if (!CollectionUtils.isEmpty(groupedModels.get(providerConfig.getProvider()))) {
					ModelProviderGroup modelProviderGroup = new ModelProviderGroup();
					modelProviderGroup.setProvider(providerConfig);
					modelProviderGroup.setModels(groupedModels.get(providerConfig.getProvider()));
					modelProviderGroups.add(modelProviderGroup);
				}
			}

			if (CollectionUtils.isEmpty(modelProviderGroups)) {
				return Result.success(Lists.newArrayList());
			}
			return Result.success(modelProviderGroups);
		}
		catch (BizException e) {
			return Result.error(IdGenerator.uuid(), e.getError());
		}
		catch (Exception e) {
			log.error("getModelSelector error", e);
			return Result.error(IdGenerator.uuid(), ErrorCode.SYSTEM_ERROR);
		}
	}

	/**
	 * Get all enabled models for prompt usage
	 * This API returns models in a format compatible with the legacy prompt API
	 * @return Result containing a list of models with id field (Long) for prompt compatibility
	 */
	@GetMapping("/enabled")
	public Result<List<Map<String, Object>>> getEnabledModels() {
		try {
			List<ModelEntity> modelEntities = modelManager.queryEnabledModelEntities();
			if (CollectionUtils.isEmpty(modelEntities)) {
				return Result.success(Lists.newArrayList());
			}
			
			// Convert to format compatible with legacy prompt API
			List<Map<String, Object>> models = modelEntities.stream().map(entity -> {
				Map<String, Object> model = new HashMap<>();
				model.put("id", entity.getId()); // Long id for prompt compatibility
				model.put("name", entity.getName());
				model.put("provider", entity.getProvider());
				model.put("modelName", entity.getModelId()); // model_id as modelName for compatibility
				model.put("baseUrl", ""); // Not available in ModelEntity
				model.put("defaultParameters", new HashMap<>()); // Not available in ModelEntity
				model.put("supportedParameters", Lists.newArrayList()); // Not available in ModelEntity
				model.put("status", entity.getEnable() ? 1 : 0);
				return model;
			}).collect(Collectors.toList());
			
			return Result.success(models);
		}
		catch (BizException e) {
			return Result.error(IdGenerator.uuid(), e.getError());
		}
		catch (Exception e) {
			log.error("getEnabledModels error", e);
			return Result.error(IdGenerator.uuid(), ErrorCode.SYSTEM_ERROR);
		}
	}

	/**
	 * Model Provider Group Data structure representing a group of models under a specific
	 * provider
	 */
	@Data
	public static class ModelProviderGroup {

		private ProviderConfigInfo provider;

		private List<ModelConfigInfo> models;

	}

}
