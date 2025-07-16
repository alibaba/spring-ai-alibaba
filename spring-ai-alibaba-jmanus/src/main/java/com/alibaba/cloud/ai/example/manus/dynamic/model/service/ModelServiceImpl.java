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
package com.alibaba.cloud.ai.example.manus.dynamic.model.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.model.repository.DynamicModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {

	private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

	private final DynamicModelRepository repository;

	private final DynamicAgentRepository agentRepository;

	@Autowired
	public ModelServiceImpl(DynamicModelRepository repository, DynamicAgentRepository agentRepository) {
		this.repository = repository;
		this.agentRepository = agentRepository;
	}

	@Override
	public List<ModelConfig> getAllModels() {
		return repository.findAll().stream().map(DynamicModelEntity::mapToModelConfig).collect(Collectors.toList());
	}

	@Override
	public ModelConfig getModelById(String id) {
		DynamicModelEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
		return entity.mapToModelConfig();
	}

	@Override
	public ModelConfig createModel(ModelConfig config) {
		try {
			// Check if an Model with the same name already exists
			DynamicModelEntity existingModel = repository.findByModelName(config.getModelName());
			if (existingModel != null) {
				log.info("Found Model with same name: {}, updating Model", config.getModelName());
				config.setId(existingModel.getId());
				return updateModel(config);
			}

			DynamicModelEntity entity = new DynamicModelEntity();
			updateEntityFromConfig(entity, config);
			entity = repository.save(entity);
			log.info("Successfully created new Model: {}", config.getModelName());
			return entity.mapToModelConfig();
		}
		catch (Exception e) {
			log.warn("Exception occurred during Model creation: {}, error message: {}", config.getModelName(),
					e.getMessage());
			// If it's a uniqueness constraint violation exception, try returning the
			// existing Model
			if (e.getMessage() != null && e.getMessage().contains("Unique")) {
				DynamicModelEntity existingModel = repository.findByModelName(config.getModelName());
				if (existingModel != null) {
					log.info("Return existing Model: {}", config.getModelName());
					return existingModel.mapToModelConfig();
				}
			}
			throw e;
		}
	}

	@Override
	public ModelConfig updateModel(ModelConfig config) {
		DynamicModelEntity entity = repository.findById(config.getId())
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + config.getId()));
		updateEntityFromConfig(entity, config);
		entity = repository.save(entity);
		return entity.mapToModelConfig();
	}

	@Override
	public void deleteModel(String id) {
		List<DynamicAgentEntity> allByModel = agentRepository
			.findAllByModel(new DynamicModelEntity(Long.parseLong(id)));
		if (allByModel != null && !allByModel.isEmpty()) {
			allByModel.forEach(dynamicAgentEntity -> dynamicAgentEntity.setModel(null));
			agentRepository.saveAll(allByModel);
		}
		repository.deleteById(Long.parseLong(id));
	}

	private void updateEntityFromConfig(DynamicModelEntity entity, ModelConfig config) {
		if (StrUtil.isNotBlank(config.getApiKey()) && !config.getApiKey().contains("*")) {
			entity.setApiKey(config.getApiKey());
		}
		entity.setBaseUrl(config.getBaseUrl());
		entity.setModelName(config.getModelName());
		entity.setModelDescription(config.getModelDescription());
		entity.setType(config.getType());
	}

}
