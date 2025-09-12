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
package com.alibaba.cloud.ai.manus.model.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.alibaba.cloud.ai.manus.model.model.enums.ModelType;
import com.alibaba.cloud.ai.manus.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.manus.model.model.vo.ValidationRequest;
import com.alibaba.cloud.ai.manus.model.model.vo.ValidationResult;
import com.alibaba.cloud.ai.manus.model.service.ModelService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*") // Add cross-origin support
public class ModelController {

	@Autowired
	private ModelService modelService;

	@Autowired
	private com.alibaba.cloud.ai.manus.model.repository.DynamicModelRepository dynamicModelRepository;

	@GetMapping
	public ResponseEntity<List<ModelConfig>> getAllModels() {
		return ResponseEntity.ok(modelService.getAllModels());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ModelConfig> getModelById(@PathVariable("id") String id) {
		return ResponseEntity.ok(modelService.getModelById(id));
	}

	@PostMapping
	public ResponseEntity<ModelConfig> createModel(@RequestBody ModelConfig modelConfig) {
		return ResponseEntity.ok(modelService.createModel(modelConfig));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ModelConfig> updateModel(@PathVariable("id") Long id, @RequestBody ModelConfig modelConfig) {
		modelConfig.setId(id);
		try {
			return ResponseEntity.ok(modelService.updateModel(modelConfig));
		}
		catch (UnsupportedOperationException e) {
			return ResponseEntity.status(499).build();
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteModel(@PathVariable("id") String id) {
		try {
			modelService.deleteModel(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
		catch (UnsupportedOperationException e) {
			return ResponseEntity.status(499).build();
		}
	}

	@GetMapping("/types")
	public ResponseEntity<List<String>> getAllModelTypes() {
		return ResponseEntity.ok(Arrays.stream(ModelType.values()).map(Enum::name).collect(Collectors.toList()));
	}

	@PostMapping("/validate")
	public ResponseEntity<ValidationResult> validateConfig(@RequestBody ValidationRequest request) {
		try {
			ValidationResult result = modelService.validateConfig(request.getBaseUrl(), request.getApiKey());
			return ResponseEntity.ok(result);
		}
		catch (Exception e) {
			ValidationResult errorResult = new ValidationResult();
			errorResult.setValid(false);
			errorResult.setMessage("Validation failed: " + e.getMessage());
			return ResponseEntity.ok(errorResult);
		}
	}

	/**
	 * Set model as default
	 */
	@PostMapping("/{id}/set-default")
	public ResponseEntity<Map<String, Object>> setDefaultModel(@PathVariable("id") Long modelId) {
		Map<String, Object> response = new HashMap<>();
		try {
			modelService.setDefaultModel(modelId);
			response.put("success", true);
			response.put("message", "Model has set to default");
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			response.put("success", false);
			response.put("message", "Set failed: " + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "Set failed: " + e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Get all available models from third-party vendors
	 */
	@GetMapping("/available-models")
	public ResponseEntity<Map<String, Object>> getAvailableModels() {
		Map<String, Object> response = new HashMap<>();
		try {
			// Get the default model entity directly from database to avoid masked API key
			List<com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity> configuredModels = dynamicModelRepository
				.findAll();
			if (configuredModels.isEmpty()) {
				response.put("options", new java.util.ArrayList<>());
				response.put("total", 0);
				response.put("error", "No configured models found");
				return ResponseEntity.ok(response);
			}

			// Find the default model entity
			com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity defaultModel = configuredModels.stream()
				.filter(com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity::getIsDefault)
				.findFirst()
				.orElse(configuredModels.get(0)); // Fallback to first model if no default
													// found

			List<com.alibaba.cloud.ai.manus.model.model.vo.AvailableModel> availableModels = ((com.alibaba.cloud.ai.manus.model.service.ModelServiceImpl) modelService)
				.getAvailableModels(defaultModel.getBaseUrl(), defaultModel.getApiKey());

			// Convert to the expected format
			List<Map<String, Object>> modelOptions = availableModels.stream().map(model -> {
				Map<String, Object> option = new HashMap<>();
				option.put("value", model.getModelName());
				option.put("label", model.getModelName() + " (" + model.getDescription() + ")");
				return option;
			}).collect(Collectors.toList());

			response.put("options", modelOptions);
			response.put("total", modelOptions.size());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			response.put("options", new java.util.ArrayList<>());
			response.put("total", 0);
			response.put("error", "Failed to fetch available models: " + e.getMessage());
			return ResponseEntity.ok(response);
		}
	}

}
