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

package com.alibaba.cloud.ai.example.deepresearch.repository;

import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * This repository class is responsible for loading agent model parameters from a JSON
 * configuration file. It reads the models defined in the "agents-config.json" file and
 * provides methods to access them.
 *
 * The JSON structure is expected to have a key "models" which maps to a list of
 * AgentModel objects.
 *
 * @author ViliamSun
 * @since 0.1.0
 */

@Repository
public class ModelParamRepositoryImpl implements ModelParamRepository {

	// JSON key in configuration file
	private static final String MODELS_ORER_AGENT = "models";

	private final Map<String, List<AgentModel>> modelSet;

	public ModelParamRepositoryImpl(@Value("classpath:agents-config.json") Resource agentsConfig,
			ObjectMapper objectMapper) {
		try {
			this.modelSet = objectMapper.readValue(ResourceUtil.loadResourceAsString(agentsConfig),
					new TypeReference<Map<String, List<AgentModel>>>() {
					});

		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error in parsing model configuration", e);
		}
	}

	/**
	 * Get the list of agent models.
	 * @return a list of AgentModel parameters.
	 */
	@Override
	public List<AgentModel> loadModels() {
		return modelSet.getOrDefault(MODELS_ORER_AGENT, List.of());
	}

	// fixme: To read external data in the future, this object needs to be redesigned
	public record AgentModel(String name, String modelName) {
	}

}
