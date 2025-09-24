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

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.dto.PromptConfigDTO;
import com.alibaba.cloud.ai.entity.UserPromptConfig;
import com.alibaba.cloud.ai.service.UserPromptConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Prompt Configuration Management
 *
 * @author Makoto
 */
@RestController
@RequestMapping("/api/prompt-config")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PromptConfigController {

	private static final Logger logger = LoggerFactory.getLogger(PromptConfigController.class);

	private final UserPromptConfigService promptConfigService;

	public PromptConfigController(UserPromptConfigService promptConfigService) {
		this.promptConfigService = promptConfigService;
	}

	/**
	 * Create or update prompt configuration
	 * @param configDTO configuration data
	 * @return operation result
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody PromptConfigDTO configDTO) {
		logger.info("保存提示词优化配置请求：{}", configDTO);

		UserPromptConfig savedConfig = promptConfigService.saveOrUpdateConfig(configDTO);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "优化配置保存成功");
		response.put("data", savedConfig);

		return ResponseEntity.ok(response);
	}

	/**
	 * Get configuration by ID
	 * @param id configuration ID
	 * @return configuration information
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getConfig(@PathVariable(value = "id") String id) {
		UserPromptConfig config = promptConfigService.getConfigById(id);

		Map<String, Object> response = new HashMap<>();
		if (config != null) {
			response.put("success", true);
			response.put("data", config);
		}
		else {
			response.put("success", false);
			response.put("message", "配置不存在");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Get all configuration list
	 * @return configuration list
	 */
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getAllConfigs() {
		List<UserPromptConfig> configs = promptConfigService.getAllConfigs();

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", configs);
		response.put("total", configs.size());

		return ResponseEntity.ok(response);
	}

	/**
	 * Get configuration list by prompt type
	 * @param promptType prompt type
	 * @return configuration list
	 */
	@GetMapping("/list-by-type/{promptType}")
	public ResponseEntity<Map<String, Object>> getConfigsByType(@PathVariable(value = "promptType") String promptType) {
		List<UserPromptConfig> configs = promptConfigService.getConfigsByType(promptType);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", configs);
		response.put("total", configs.size());

		return ResponseEntity.ok(response);
	}

	/**
	 * Get currently enabled configuration
	 * @param promptType prompt type
	 * @return currently enabled configuration
	 */
	@GetMapping("/active/{promptType}")
	public ResponseEntity<Map<String, Object>> getActiveConfig(@PathVariable(value = "promptType") String promptType) {
		UserPromptConfig config = promptConfigService.getActiveConfigByType(promptType);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", config);
		response.put("hasCustomConfig", config != null);

		return ResponseEntity.ok(response);
	}

	/**
	 * 获取某个类型的所有启用的优化配置
	 * @param promptType 提示词类型
	 * @return 启用的优化配置列表
	 */
	@GetMapping("/active-all/{promptType}")
	public ResponseEntity<Map<String, Object>> getActiveConfigs(@PathVariable(value = "promptType") String promptType) {
		List<UserPromptConfig> configs = promptConfigService.getActiveConfigsByType(promptType);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", configs);
		response.put("total", configs.size());
		response.put("hasOptimizationConfigs", !configs.isEmpty());

		return ResponseEntity.ok(response);
	}

	/**
	 * Delete configuration
	 * @param id configuration ID
	 * @return operation result
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable(value = "id") String id) {
		boolean deleted = promptConfigService.deleteConfig(id);

		Map<String, Object> response = new HashMap<>();
		if (deleted) {
			response.put("success", true);
			response.put("message", "配置删除成功");
		}
		else {
			response.put("success", false);
			response.put("message", "配置不存在或删除失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Enable specified configuration
	 * @param id configuration ID
	 * @return operation result
	 */
	@PostMapping("/{id}/enable")
	public ResponseEntity<Map<String, Object>> enableConfig(@PathVariable(value = "id") String id) {
		boolean enabled = promptConfigService.enableConfig(id);

		Map<String, Object> response = new HashMap<>();
		if (enabled) {
			response.put("success", true);
			response.put("message", "配置启用成功");
		}
		else {
			response.put("success", false);
			response.put("message", "配置不存在或启用失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Disable specified configuration
	 * @param id configuration ID
	 * @return operation result
	 */
	@PostMapping("/{id}/disable")
	public ResponseEntity<Map<String, Object>> disableConfig(@PathVariable(value = "id") String id) {
		boolean disabled = promptConfigService.disableConfig(id);

		Map<String, Object> response = new HashMap<>();
		if (disabled) {
			response.put("success", true);
			response.put("message", "配置禁用成功");
		}
		else {
			response.put("success", false);
			response.put("message", "配置不存在或禁用失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Get supported prompt type list
	 * @return prompt type list
	 */
	@GetMapping("/types")
	public ResponseEntity<Map<String, Object>> getSupportedPromptTypes() {
		// Supported prompt types
		String[] types = { "report-generator", "planner", "sql-generator", "python-generator", "rewrite" };

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", types);

		return ResponseEntity.ok(response);
	}

	/**
	 * 批量启用配置
	 * @param ids 配置ID列表
	 * @return 操作结果
	 */
	@PostMapping("/batch-enable")
	public ResponseEntity<Map<String, Object>> batchEnableConfigs(@RequestBody List<String> ids) {
		boolean success = promptConfigService.enableConfigs(ids);

		Map<String, Object> response = new HashMap<>();
		if (success) {
			response.put("success", true);
			response.put("message", "批量启用配置成功");
		}
		else {
			response.put("success", false);
			response.put("message", "批量启用配置失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 批量禁用配置
	 * @param ids 配置ID列表
	 * @return 操作结果
	 */
	@PostMapping("/batch-disable")
	public ResponseEntity<Map<String, Object>> batchDisableConfigs(@RequestBody List<String> ids) {
		boolean success = promptConfigService.disableConfigs(ids);

		Map<String, Object> response = new HashMap<>();
		if (success) {
			response.put("success", true);
			response.put("message", "批量禁用配置成功");
		}
		else {
			response.put("success", false);
			response.put("message", "批量禁用配置失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 更新配置优先级
	 * @param id 配置ID
	 * @param requestBody 包含优先级的请求体
	 * @return 操作结果
	 */
	@PostMapping("/{id}/priority")
	public ResponseEntity<Map<String, Object>> updatePriority(@PathVariable(value = "id") String id, 
			@RequestBody Map<String, Object> requestBody) {
		Integer priority = (Integer) requestBody.get("priority");
		boolean success = promptConfigService.updatePriority(id, priority);

		Map<String, Object> response = new HashMap<>();
		if (success) {
			response.put("success", true);
			response.put("message", "更新优先级成功");
		}
		else {
			response.put("success", false);
			response.put("message", "更新优先级失败");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 更新配置显示顺序
	 * @param id 配置ID
	 * @param requestBody 包含显示顺序的请求体
	 * @return 操作结果
	 */
	@PostMapping("/{id}/display-order")
	public ResponseEntity<Map<String, Object>> updateDisplayOrder(@PathVariable(value = "id") String id, 
			@RequestBody Map<String, Object> requestBody) {
		Integer displayOrder = (Integer) requestBody.get("displayOrder");
		boolean success = promptConfigService.updateDisplayOrder(id, displayOrder);

		Map<String, Object> response = new HashMap<>();
		if (success) {
			response.put("success", true);
			response.put("message", "更新显示顺序成功");
		}
		else {
			response.put("success", false);
			response.put("message", "更新显示顺序失败");
		}

		return ResponseEntity.ok(response);
	}

}
