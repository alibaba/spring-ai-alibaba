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

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.repository.DynamicModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {

	private static final String DEFAULT_AGENT_NAME = "DEFAULT_AGENT";

	private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

	private final DynamicModelRepository repository;

	@Autowired
	public ModelServiceImpl(DynamicModelRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<ModelConfig> getAllModels() {
		return repository.findAll().stream().map(this::mapToModelConfig).collect(Collectors.toList());
	}

	@Override
	public ModelConfig getModelById(String id) {
		DynamicModelEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
		return mapToModelConfig(entity);
	}

	@Override
	public ModelConfig createModel(ModelConfig config) {
		try {
			// Check if an Model with the same name already exists
			DynamicModelEntity existingModel = repository.findByModelName(config.getName());
			if (existingModel != null) {
				log.info("Found Model with same name: {}, updating Model", config.getName());
				config.setId(existingModel.getId().toString());
				return updateModel(config);
			}

			DynamicModelEntity entity = new DynamicModelEntity();
			updateEntityFromConfig(entity, config);
			entity = repository.save(entity);
			log.info("Successfully created new Model: {}", config.getName());
			return mapToModelConfig(entity);
		}
		catch (Exception e) {
			log.warn("Exception occurred during Model creation: {}, error message: {}", config.getName(),
					e.getMessage());
			// If it's a uniqueness constraint violation exception, try returning the
			// existing Model
			if (e.getMessage() != null && e.getMessage().contains("Unique")) {
				DynamicModelEntity existingModel = repository.findByModelName(config.getName());
				if (existingModel != null) {
					log.info("Return existing Model: {}", config.getName());
					return mapToModelConfig(existingModel);
				}
			}
			throw e;
		}
	}

	@Override
	public ModelConfig updateModel(ModelConfig config) {
		DynamicModelEntity entity = repository.findById(Long.parseLong(config.getId()))
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + config.getId()));
		updateEntityFromConfig(entity, config);
		entity = repository.save(entity);
		return mapToModelConfig(entity);
	}

	@Override
	public void deleteModel(String id) {
		DynamicModelEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));

		if (DEFAULT_AGENT_NAME.equals(entity.getModelName())) {
			throw new IllegalArgumentException("Cannot delete default Model");
		}

		repository.deleteById(Long.parseLong(id));
	}

	private ModelConfig mapToModelConfig(DynamicModelEntity entity) {
		ModelConfig config = new ModelConfig();
		config.setId(entity.getId().toString());
		config.setName(entity.getModelName());
		config.setDescription(entity.getModelDescription());
		return config;
	}

	private void updateEntityFromConfig(DynamicModelEntity entity, ModelConfig config) {
		entity.setModelName(config.getName());
		entity.setModelDescription(config.getDescription());

	}

}
