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
package com.alibaba.cloud.ai.manus.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.manus.model.model.enums.ModelType;
import com.alibaba.cloud.ai.manus.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.manus.model.service.ModelService;

@RestController
@RequestMapping("/api/init")
public class InitController {

	private static final Logger log = LoggerFactory.getLogger(InitController.class);

	@Autowired
	private Environment environment;

	@Autowired
	private ModelService modelService;

	@Autowired
	private DefaultLlmConfiguration defaultLlmConfig;

	/**
	 * Check if system initialization is complete
	 * @return initialization status information
	 */
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getInitStatus() {
		Map<String, Object> response = new HashMap<>();

		try {
			// Based on LlmService mode: check if configured models exist
			boolean hasConfiguredModels = modelService.getAllModels().size() > 0;

			// Check environment variables (for Docker deployment)
			// Support DashScope API Key
			String dashscopeApiKeyFromEnv = environment.getProperty("DASHSCOPE_API_KEY");
			boolean hasDashscopeEnvConfig = dashscopeApiKeyFromEnv != null && !dashscopeApiKeyFromEnv.trim().isEmpty();

			// Support OpenAI compatible API configuration
			String openaiApiKeyFromEnv = environment.getProperty("OPENAI_API_KEY");
			boolean hasOpenaiEnvConfig = openaiApiKeyFromEnv != null && !openaiApiKeyFromEnv.trim().isEmpty();

			// If any environment variable is configured, consider it as configured
			boolean hasEnvConfig = hasDashscopeEnvConfig || hasOpenaiEnvConfig;

			// System initialization status based on configured models or environment
			// variables
			boolean initialized = hasConfiguredModels || hasEnvConfig;

			response.put("initialized", initialized);
			response.put("hasConfiguredModels", hasConfiguredModels);
			response.put("hasEnvConfig", hasEnvConfig);
			response.put("hasDashscopeEnvConfig", hasDashscopeEnvConfig);
			response.put("hasOpenaiEnvConfig", hasOpenaiEnvConfig);
			response.put("success", true);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			response.put("initialized", false);
			response.put("success", false);
			response.put("error", e.getMessage());
			return ResponseEntity.ok(response);
		}
	}

	/**
	 * Save initialization configuration
	 * @param request initialization request
	 * @return save result
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> saveInitConfig(@RequestBody InitConfigRequest request) {
		Map<String, Object> response = new HashMap<>();

		try {
			// Validate basic fields
			if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
				response.put("success", false);
				response.put("error", "API Key cannot be empty");
				return ResponseEntity.badRequest().body(response);
			}

			String configMode = request.getConfigMode();
			if (configMode == null) {
				configMode = "dashscope"; // Default mode
			}

			// Validate required fields for custom mode
			if ("custom".equals(configMode)) {
				if (request.getBaseUrl() == null || request.getBaseUrl().trim().isEmpty()) {
					response.put("success", false);
					response.put("error", "API base URL cannot be empty");
					return ResponseEntity.badRequest().body(response);
				}
				if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
					response.put("success", false);
					response.put("error", "Model name cannot be empty");
					return ResponseEntity.badRequest().body(response);
				}
			}

			// Based on LlmService mode: API Key is managed through dynamic model
			// management, no longer stored separately
			// Create corresponding dynamic model based on configuration mode
			ModelConfig defaultModel = createOrUpdateDefaultModel(request);

			// Set the newly created model as default model
			if (defaultModel != null && defaultModel.getId() != null) {
				modelService.setDefaultModel(defaultModel.getId());
			}

			response.put("success", true);
			response.put("message", "Configuration saved successfully, default model created");
			response.put("requiresRestart", false); // No restart needed because using
													// dynamic models
			response.put("modelId", defaultModel != null ? defaultModel.getId() : null);
			response.put("configMode", configMode);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("error", "Failed to save configuration: " + e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Create or update default dynamic model configuration
	 */
	private ModelConfig createOrUpdateDefaultModel(InitConfigRequest request) {
		try {
			String configMode = request.getConfigMode();
			if (configMode == null) {
				configMode = "dashscope";
			}

			// Create model configuration
			String apiKey = request.getApiKey();
			if (apiKey == null || apiKey.trim().isEmpty()) {
				throw new IllegalArgumentException("API key cannot be null or empty");
			}

			ModelConfig modelConfig = new ModelConfig();
			modelConfig.setApiKey(apiKey.trim());
			modelConfig.setType(ModelType.GENERAL.name());
			modelConfig.setIsDefault(true); // Set as default model

			if ("dashscope".equals(configMode)) {
				// Use DashScope default configuration
				modelConfig.setBaseUrl(defaultLlmConfig.getDefaultBaseUrl());
				modelConfig.setModelName(defaultLlmConfig.getDefaultModelName());
				String description = defaultLlmConfig.getDefaultDescription()
						+ " - Created via initialization wizard (DashScope mode)";
				modelConfig.setModelDescription(description);
			}
			else if ("custom".equals(configMode)) {
				// Use user custom configuration - only baseUrl, modelName, and apiKey are
				// required
				String baseUrl = request.getBaseUrl();
				if (baseUrl == null || baseUrl.trim().isEmpty()) {
					throw new IllegalArgumentException("Base URL cannot be null or empty");
				}
				modelConfig.setBaseUrl(baseUrl.trim());

				String modelName = request.getModelName();
				if (modelName == null || modelName.trim().isEmpty()) {
					throw new IllegalArgumentException("Model name cannot be null or empty");
				}
				modelConfig.setModelName(modelName.trim());

				// Optional fields - can be null or empty
				String completionsPath = request.getCompletionsPath();
				if (completionsPath != null && !completionsPath.trim().isEmpty()) {
					modelConfig.setCompletionsPath(completionsPath.trim());
				}
				// If completionsPath is null/empty, it will use default
				// "/v1/chat/completions"

				String displayName = request.getModelDisplayName();
				if (displayName == null || displayName.trim().isEmpty()) {
					displayName = request.getModelName().trim();
				}

				String description = "Custom OpenAI compatible model - " + displayName
						+ " - Created via initialization wizard";
				modelConfig.setModelDescription(description);
			}
			else {
				throw new IllegalArgumentException("Unsupported configuration mode: " + configMode);
			}

			// Create or update model
			return modelService.createModel(modelConfig);
		}
		catch (Exception e) {
			log.error("Failed to create default model", e);
			// Model creation failure does not affect initialization process
			return null;
		}
	}

	/**
	 * Initialization configuration request class
	 */
	public static class InitConfigRequest {

		private String configMode; // "dashscope" or "custom"

		private String apiKey;

		private String baseUrl;

		private String modelName;

		private String modelDisplayName;

		private String completionsPath;

		public String getConfigMode() {
			return configMode;
		}

		public void setConfigMode(String configMode) {
			this.configMode = configMode;
		}

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getModelName() {
			return modelName;
		}

		public void setModelName(String modelName) {
			this.modelName = modelName;
		}

		public String getModelDisplayName() {
			return modelDisplayName;
		}

		public void setModelDisplayName(String modelDisplayName) {
			this.modelDisplayName = modelDisplayName;
		}

		public String getCompletionsPath() {
			return completionsPath;
		}

		public void setCompletionsPath(String completionsPath) {
			this.completionsPath = completionsPath;
		}

	}

}
