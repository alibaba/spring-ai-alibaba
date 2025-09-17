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

import com.alibaba.cloud.ai.manus.config.DefaultLlmConfiguration;
import com.alibaba.cloud.ai.manus.config.IConfigService;
import com.alibaba.cloud.ai.manus.event.JmanusEventPublisher;
import com.alibaba.cloud.ai.manus.event.ModelChangeEvent;
import com.alibaba.cloud.ai.manus.llm.LlmService;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.model.model.enums.ModelType;
import com.alibaba.cloud.ai.manus.model.repository.DynamicModelRepository;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author lizhenning
 * @since 2025/7/8
 */
@Service
public class ModelDataInitialization implements IModelDataInitialization {

	private static final Logger log = LoggerFactory.getLogger(ModelDataInitialization.class);

	// To ensure llmService is initialized first
	@Autowired
	private LlmService llmService;

	@Autowired
	private JmanusEventPublisher jmanusEventPublisher;

	@Autowired(required = false)
	private IConfigService configService;

	@Autowired
	private DefaultLlmConfiguration defaultLlmConfig;

	@Autowired
	private Environment environment;

	private final DynamicModelRepository repository;

	public ModelDataInitialization(DynamicModelRepository repository) {
		this.repository = repository;
	}

	@PostConstruct
	public void init() {
		// Check environment variables and automatically create model configuration (for
		// Docker deployment scenarios)
		try {
			createModelFromEnvironmentVariablesIfNeeded();
		}
		catch (Exception e) {
			log.warn("Failed to create default model from environment variables", e);
		}

		// Check if models saved through configuration system exist (maintain backward
		// compatibility)
		if (configService != null) {
			try {
				String configValue = configService.getConfigValue("manus.dashscope.apiKey");
				if (configValue != null && !configValue.trim().isEmpty()) {
					// If API key exists in configuration system but no corresponding
					// dynamic model, create one
					createModelFromConfigIfNeeded(configValue.trim());
				}
			}
			catch (Exception e) {
				// Configuration system may not be initialized yet, ignore errors
			}
		}
	}

	/**
	 * Check environment variables and automatically create corresponding model
	 * configuration
	 */
	private void createModelFromEnvironmentVariablesIfNeeded() {
		// First check if default model already exists in database
		DynamicModelEntity existingDefaultModel = repository.findByIsDefaultTrue();
		if (existingDefaultModel != null) {
			log.info("Default model already exists: {}, skipping environment variable model creation",
					existingDefaultModel.getModelName());
			return;
		}

		// Check DashScope environment variables first
		String dashscopeApiKey = environment.getProperty("DASHSCOPE_API_KEY");
		if (dashscopeApiKey != null && !dashscopeApiKey.trim().isEmpty()) {
			createDashScopeModelFromEnv(dashscopeApiKey.trim());
			return; // If DashScope configuration exists, use DashScope first
		}

		// Check OpenAI compatible environment variables
		String openaiApiKey = environment.getProperty("OPENAI_API_KEY");
		String openaiBaseUrl = environment.getProperty("OPENAI_BASE_URL");
		String openaiModel = environment.getProperty("OPENAI_MODEL");

		if (openaiApiKey != null && !openaiApiKey.trim().isEmpty()) {
			createOpenAICompatibleModelFromEnv(openaiApiKey.trim(),
					openaiBaseUrl != null ? openaiBaseUrl.trim() : "https://api.openai.com/v1",
					openaiModel != null ? openaiModel.trim() : "gpt-3.5-turbo");
		}
	}

	/**
	 * Create DashScope model from environment variables
	 */
	private void createDashScopeModelFromEnv(String apiKey) {
		try {
			DynamicModelEntity existingDefaultModel = repository.findByIsDefaultTrue();
			if (existingDefaultModel != null) {
				log.info("Default model already exists: {}, skipping DashScope model creation",
						existingDefaultModel.getModelName());
				return;
			}

			String modelName = defaultLlmConfig.getDefaultModelName();
			DynamicModelEntity existingModel = repository.findByModelName(modelName);
			if (existingModel == null) {
				DynamicModelEntity dynamicModelEntity = new DynamicModelEntity();
				dynamicModelEntity.setBaseUrl(defaultLlmConfig.getDefaultBaseUrl());
				dynamicModelEntity.setHeaders(null);
				dynamicModelEntity.setApiKey(apiKey);
				dynamicModelEntity.setModelName(modelName);
				dynamicModelEntity.setModelDescription(
						defaultLlmConfig.getDefaultDescription() + " - Auto-created from environment variables");
				dynamicModelEntity.setType(ModelType.GENERAL.name());
				dynamicModelEntity.setIsDefault(true);
				dynamicModelEntity.setCompletionsPath(defaultLlmConfig.getDefaultCompletionsPath());

				DynamicModelEntity save = repository.save(dynamicModelEntity);
				jmanusEventPublisher.publish(new ModelChangeEvent(save));
				log.info("Auto-created DashScope model configuration from environment variable DASHSCOPE_API_KEY: {}",
						modelName);
			}
		}
		catch (Exception e) {
			log.error("Failed to create DashScope model from environment variables", e);
		}
	}

	/**
	 * Create OpenAI compatible model from environment variables
	 */
	private void createOpenAICompatibleModelFromEnv(String apiKey, String baseUrl, String modelName) {
		try {
			DynamicModelEntity existingDefaultModel = repository.findByIsDefaultTrue();
			if (existingDefaultModel != null) {
				log.info("Default model already exists: {}, skipping OpenAI compatible model creation",
						existingDefaultModel.getModelName());
				return;
			}

			DynamicModelEntity existingModel = repository.findByModelName(modelName);
			if (existingModel == null) {
				DynamicModelEntity dynamicModelEntity = new DynamicModelEntity();
				dynamicModelEntity.setBaseUrl(baseUrl);
				dynamicModelEntity.setHeaders(null);
				dynamicModelEntity.setApiKey(apiKey);
				dynamicModelEntity.setModelName(modelName);
				dynamicModelEntity.setModelDescription(
						"OpenAI compatible model - " + modelName + " - Auto-created from environment variables");
				dynamicModelEntity.setType(ModelType.GENERAL.name());
				dynamicModelEntity.setIsDefault(true);

				DynamicModelEntity save = repository.save(dynamicModelEntity);
				jmanusEventPublisher.publish(new ModelChangeEvent(save));
				log.info("Auto-created OpenAI compatible model configuration from environment variables: {} ({})",
						modelName, baseUrl);
			}
		}
		catch (Exception e) {
			log.error("Failed to create OpenAI compatible model from environment variables", e);
		}
	}

	/**
	 * Create a model if API key exists in configuration system but no corresponding
	 * dynamic model exists
	 */
	private void createModelFromConfigIfNeeded(String apiKey) {
		try {
			DynamicModelEntity existingDefaultModel = repository.findByIsDefaultTrue();
			if (existingDefaultModel != null) {
				log.info("Default model already exists: {}, skipping config system model creation",
						existingDefaultModel.getModelName());
				return;
			}

			String modelName = defaultLlmConfig.getDefaultModelName();
			DynamicModelEntity existingModel = repository.findByModelName(modelName);
			if (existingModel == null) {
				// Only create if no model with same name exists
				DynamicModelEntity dynamicModelEntity = new DynamicModelEntity();
				dynamicModelEntity.setBaseUrl(defaultLlmConfig.getDefaultBaseUrl());
				dynamicModelEntity.setHeaders(null); // No longer depend on injected
														// ChatModel default options
				dynamicModelEntity.setApiKey(apiKey);
				dynamicModelEntity.setModelName(modelName);
				dynamicModelEntity.setModelDescription(
						defaultLlmConfig.getDefaultDescription() + " - Synced from configuration system");
				dynamicModelEntity.setType(ModelType.GENERAL.name());

				DynamicModelEntity save = repository.save(dynamicModelEntity);
				jmanusEventPublisher.publish(new ModelChangeEvent(save));
				log.info("Auto-created model from config system: {}", modelName);
			}
		}
		catch (Exception e) {
			// Creation failure does not affect system startup
		}
	}

	private String getDynamicApiKey() {
		// Based on LlmService mode: API Key is completely managed through dynamic model
		// management
		// No longer get independent API Key from configuration system here
		// If environment variables exist at system startup, they will be used to create
		// default model
		// Otherwise wait for user to configure through initialization page
		return null;
	}

}
