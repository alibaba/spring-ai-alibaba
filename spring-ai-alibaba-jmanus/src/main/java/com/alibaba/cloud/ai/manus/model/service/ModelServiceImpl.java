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
package com.alibaba.cloud.ai.manus.model.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.manus.event.JmanusEventPublisher;
import com.alibaba.cloud.ai.manus.event.ModelChangeEvent;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.model.exception.AuthenticationException;
import com.alibaba.cloud.ai.manus.model.exception.NetworkException;
import com.alibaba.cloud.ai.manus.model.exception.RateLimitException;
import com.alibaba.cloud.ai.manus.model.model.vo.AvailableModel;
import com.alibaba.cloud.ai.manus.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.manus.model.model.vo.ValidationResult;
import com.alibaba.cloud.ai.manus.model.repository.DynamicModelRepository;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {

	private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

	private final DynamicModelRepository repository;

	private final DynamicAgentRepository agentRepository;

	@Autowired
	private JmanusEventPublisher publisher;

	// Cache for third-party API calls with 2-second expiration
	private final Map<String, CacheEntry<List<AvailableModel>>> apiCache = new ConcurrentHashMap<>();

	private static final long CACHE_EXPIRY_MS = 2000; // 2 seconds

	@Autowired
	public ModelServiceImpl(DynamicModelRepository repository, DynamicAgentRepository agentRepository) {
		this.repository = repository;
		this.agentRepository = agentRepository;
	}

	// Cache entry class
	private static class CacheEntry<T> {

		private final T data;

		private final long timestamp;

		public CacheEntry(T data) {
			this.data = data;
			this.timestamp = System.currentTimeMillis();
		}

		public T getData() {
			return data;
		}

		public boolean isExpired() {
			return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
		}

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
			// Set default values
			setDefaultConfig(config);
			// Check if an Model with the same name already exists
			DynamicModelEntity existingModel = repository.findByModelName(config.getModelName());
			if (existingModel != null) {
				log.info("Found Model with same name: {}, updating Model", config.getModelName());
				return updateModel(existingModel);
			}

			DynamicModelEntity entity = new DynamicModelEntity();
			updateEntityFromConfig(entity, config);

			if (config.getIsDefault() != null && config.getIsDefault()) {
				clearOtherDefaultModels();
			}

			entity = repository.save(entity);
			publisher.publish(new ModelChangeEvent(entity));
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
		setDefaultConfig(config);
		DynamicModelEntity entity = repository.findById(config.getId())
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + config.getId()));

		if (config.getIsDefault() != null && config.getIsDefault()) {
			clearOtherDefaultModels();
		}

		updateEntityFromConfig(entity, config);
		return updateModel(entity);
	}

	public ModelConfig updateModel(DynamicModelEntity entity) {
		entity = repository.save(entity);
		publisher.publish(new ModelChangeEvent(entity));
		return entity.mapToModelConfig();
	}

	@Override
	public void deleteModel(String id) {
		if (agentRepository.count() == 1) {
			throw new IllegalArgumentException("Cannot clear all models");
		}
		List<DynamicAgentEntity> allByModel = agentRepository
			.findAllByModel(new DynamicModelEntity(Long.parseLong(id)));
		if (allByModel != null && !allByModel.isEmpty()) {
			allByModel.forEach(dynamicAgentEntity -> dynamicAgentEntity.setModel(null));
			agentRepository.saveAll(allByModel);
		}
		repository.deleteById(Long.parseLong(id));
	}

	@Override
	public ValidationResult validateConfig(String baseUrl, String apiKey) {
		log.info("Starting model configuration validation - Base URL: {}, API Key: {}", baseUrl, maskApiKey(apiKey));

		ValidationResult result = new ValidationResult();

		try {
			// 1. Validate Base URL format
			log.debug("Validating Base URL format: {}", baseUrl);
			if (!isValidBaseUrl(baseUrl)) {
				log.warn("Base URL format validation failed: {}", baseUrl);
				result.setValid(false);
				result.setMessage("Base URL format is incorrect");
				return result;
			}
			log.debug("Base URL format validation passed");

			// 2. Validate API Key format
			log.debug("Validating API Key format");
			if (!isValidApiKey(apiKey)) {
				log.warn("API Key format validation failed");
				result.setValid(false);
				result.setMessage("API Key format is incorrect");
				return result;
			}
			log.debug("API Key format validation passed");

			// 3. Call third-party API for validation
			log.info("Starting third-party API validation");
			List<AvailableModel> models = callThirdPartyApiInternal(baseUrl, apiKey);

			result.setValid(true);
			result.setMessage("Validation successful");
			result.setAvailableModels(models);

			log.info("Third-party API validation successful, obtained {} available models", models.size());

		}
		catch (AuthenticationException e) {
			log.error("API Key authentication failed: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("API Key is invalid or expired");
		}
		catch (NetworkException e) {
			log.error("Network connection validation failed: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("Network connection failed, please check Base URL");
		}
		catch (RateLimitException e) {
			log.error("Request rate limit: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("Request rate too high, please try again later");
		}
		catch (Exception e) {
			log.error("Unknown exception occurred during validation: {}", e.getMessage(), e);
			result.setValid(false);
			result.setMessage("Validation failed: " + e.getMessage());
		}

		log.info("Model configuration validation completed - {}", result.isValid() ? "Success" : "Failed");
		log.info("Model configuration validation result - Valid: {}, Message: {}", result.isValid(),
				result.getMessage());

		return result;
	}

	private boolean isValidBaseUrl(String baseUrl) {
		try {
			URL url = new URL(baseUrl);
			boolean isValid = "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
			log.debug("Base URL validation result: {} - Protocol: {}, Host: {}", isValid, url.getProtocol(),
					url.getHost());
			return isValid;
		}
		catch (MalformedURLException e) {
			log.debug("Base URL format invalid: {} - Error: {}", baseUrl, e.getMessage());
			return false;
		}
	}

	private boolean isValidApiKey(String apiKey) {
		boolean isValid = apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() >= 10;
		log.debug("API Key validation result: {} - Length: {}", isValid, apiKey != null ? apiKey.length() : 0);
		return isValid;
	}

	private String maskApiKey(String apiKey) {
		if (apiKey == null || apiKey.length() <= 8) {
			return "***";
		}
		return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
	}

	private List<AvailableModel> parseModelsResponse(Map response) {
		log.debug("Starting to parse API response: {}", response);

		List<AvailableModel> models = new ArrayList<>();

		if (response == null) {
			log.warn("Response is empty");
			return models;
		}

		// Try to parse standard OpenAI format: {"data": [...]}
		Object data = response.get("data");
		if (data instanceof List) {
			List<Map> modelList = (List<Map>) data;
			log.debug("Found response data containing {} models", modelList.size());

			for (int i = 0; i < modelList.size(); i++) {
				Map modelData = modelList.get(i);
				log.debug("Parsing model data #{}: {}", i + 1, modelData);

				String modelId = (String) modelData.get("id");
				String modelName = (String) modelData.get("name");
				String description = (String) modelData.get("description");

				// If no name field, use id as display name
				if (modelName == null) {
					modelName = modelId;
				}

				// If no description field, use default description
				if (description == null) {
					description = "Model ID: " + modelId;
				}

				log.debug("Parsing model - ID: {}, Name: {}, Description: {}", modelId, modelName, description);

				models.add(new AvailableModel(modelId, modelName, description));
			}
		}
		else {
			log.warn("Response format does not meet expectations, data field is not array type");
		}

		log.info("Successfully parsed response, obtained {} available models", models.size());
		return models;
	}

	private void updateEntityFromConfig(DynamicModelEntity entity, ModelConfig config) {
		if (StrUtil.isNotBlank(config.getApiKey()) && !config.getApiKey().contains("*")) {
			entity.setApiKey(config.getApiKey());
		}
		entity.setBaseUrl(config.getBaseUrl());
		entity.setHeaders(config.getHeaders());
		entity.setModelName(config.getModelName());
		entity.setModelDescription(config.getModelDescription());
		entity.setType(config.getType());
		if (config.getIsDefault() != null) {
			entity.setIsDefault(config.getIsDefault());
		}
		entity.setTemperature(config.getTemperature());
		entity.setTopP(config.getTopP());
		entity.setCompletionsPath(config.getCompletionsPath());
	}

	private void setDefaultConfig(ModelConfig modelConfig) {
		if (modelConfig.getTemperature() == null) {
			modelConfig.setTemperature(0.7);
		}
		if (!StringUtils.hasText(modelConfig.getCompletionsPath())) {
			modelConfig.setCompletionsPath("/v1/chat/completions");
		}
	}

	@Override
	@Transactional
	public void setDefaultModel(Long modelId) {
		log.info("Set model: {} as default", modelId);

		List<DynamicModelEntity> allModels = repository.findAll();
		for (DynamicModelEntity model : allModels) {
			if (model.getIsDefault()) {
				model.setIsDefault(false);
				repository.save(model);
				log.info("Cancel {} model as default", model.getId());
			}
		}

		Optional<DynamicModelEntity> targetModel = repository.findById(modelId);
		if (targetModel.isPresent()) {
			DynamicModelEntity model = targetModel.get();
			model.setIsDefault(true);
			repository.save(model);
			log.info("Set {} as default", modelId);
			publisher.publish(new ModelChangeEvent(model));
		}
		else {
			log.error("Cannot find {} model", modelId);
			throw new RuntimeException("Model not present");
		}
	}

	private void clearOtherDefaultModels() {
		List<DynamicModelEntity> allModels = repository.findAll();
		for (DynamicModelEntity model : allModels) {
			if (model.getIsDefault()) {
				model.setIsDefault(false);
				repository.save(model);
				log.info("Cancel {} model as default", model.getId());
			}
		}
	}

	/**
	 * Public method to call third-party API with caching Cache expires every 2 seconds
	 */
	public List<AvailableModel> getAvailableModels(String baseUrl, String apiKey) {
		String cacheKey = baseUrl + ":" + apiKey;

		// Check cache first
		CacheEntry<List<AvailableModel>> cachedEntry = apiCache.get(cacheKey);
		if (cachedEntry != null && !cachedEntry.isExpired()) {
			log.debug("Returning cached result for API call: {}", baseUrl);
			return cachedEntry.getData();
		}

		log.debug("Cache miss or expired, making new API call to: {}", baseUrl);

		// Make new API call using the internal method
		List<AvailableModel> models = callThirdPartyApiInternal(baseUrl, apiKey);

		// Cache the result
		apiCache.put(cacheKey, new CacheEntry<>(models));

		return models;
	}

	/**
	 * Internal method that actually makes the API call
	 */
	private List<AvailableModel> callThirdPartyApiInternal(String baseUrl, String apiKey) {
		log.debug("Starting third-party API call - URL: {}", baseUrl);

		RestTemplate restTemplate = new RestTemplate();

		// Set request headers
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + apiKey);
		headers.set("Content-Type", "application/json");

		log.debug("Setting request headers - Content-Type: application/json, Authorization: Bearer {}",
				maskApiKey(apiKey));

		// Build request URL
		String requestUrl = baseUrl + "/v1/models";
		log.info("Sending HTTP request to: {}", requestUrl);

		try {
			long startTime = System.currentTimeMillis();
			// Create HttpEntity to wrap request headers
			HttpEntity<String> entity = new HttpEntity<>(headers);
			// Send GET request with HttpEntity containing headers
			ResponseEntity<Map> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, Map.class);
			long endTime = System.currentTimeMillis();

			log.info("HTTP request completed - Status code: {}, Duration: {}ms", response.getStatusCodeValue(),
					endTime - startTime);

			// Parse response
			List<AvailableModel> models = parseModelsResponse(response.getBody());
			log.info("Successfully parsed response, obtained {} models", models.size());

			return models;

		}
		catch (Exception e) {
			log.error("API call failed: {}", e.getMessage(), e);
			throw new NetworkException("API call failed: " + e.getMessage(), e);
		}
	}

}
