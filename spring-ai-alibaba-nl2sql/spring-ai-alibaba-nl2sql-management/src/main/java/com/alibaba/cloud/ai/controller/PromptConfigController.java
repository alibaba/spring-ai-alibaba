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
 * 用户提示词配置管理
 *
 * @author Makoto
 */
@RestController
@RequestMapping("/api/prompt-config")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PromptConfigController {

	private static final Logger logger = LoggerFactory.getLogger(PromptConfigController.class);

	private UserPromptConfigService promptConfigService;

	public PromptConfigController() {
		this.promptConfigService = new UserPromptConfigService();
	}

	/**
	 * 创建或更新提示词配置
	 * @param configDTO 配置数据
	 * @return 操作结果
	 */
	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody PromptConfigDTO configDTO) {
		try {
			logger.info("保存提示词配置请求：{}", configDTO);

			UserPromptConfig savedConfig = promptConfigService.saveOrUpdateConfig(configDTO);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "配置保存成功");
			response.put("data", savedConfig);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("保存提示词配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "配置保存失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 根据ID获取配置
	 * @param id 配置ID
	 * @return 配置信息
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String id) {
		try {
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
		catch (Exception e) {
			logger.error("获取配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "获取配置失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取所有配置列表
	 * @return 配置列表
	 */
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getAllConfigs() {
		try {
			List<UserPromptConfig> configs = promptConfigService.getAllConfigs();

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", configs);
			response.put("total", configs.size());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("获取配置列表失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "获取配置列表失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 根据提示词类型获取配置列表
	 * @param promptType 提示词类型
	 * @return 配置列表
	 */
	@GetMapping("/list-by-type/{promptType}")
	public ResponseEntity<Map<String, Object>> getConfigsByType(@PathVariable String promptType) {
		try {
			List<UserPromptConfig> configs = promptConfigService.getConfigsByType(promptType);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", configs);
			response.put("total", configs.size());

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("根据类型获取配置列表失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "获取配置列表失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取当前启用的配置
	 * @param promptType 提示词类型
	 * @return 当前启用的配置
	 */
	@GetMapping("/active/{promptType}")
	public ResponseEntity<Map<String, Object>> getActiveConfig(@PathVariable String promptType) {
		try {
			UserPromptConfig config = promptConfigService.getActiveConfigByType(promptType);

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", config);
			response.put("hasCustomConfig", config != null);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("获取启用配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "获取启用配置失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 删除配置
	 * @param id 配置ID
	 * @return 操作结果
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable String id) {
		try {
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
		catch (Exception e) {
			logger.error("删除配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "删除配置失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 启用指定配置
	 * @param id 配置ID
	 * @return 操作结果
	 */
	@PostMapping("/{id}/enable")
	public ResponseEntity<Map<String, Object>> enableConfig(@PathVariable String id) {
		try {
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
		catch (Exception e) {
			logger.error("启用配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "启用配置失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 禁用指定配置
	 * @param id 配置ID
	 * @return 操作结果
	 */
	@PostMapping("/{id}/disable")
	public ResponseEntity<Map<String, Object>> disableConfig(@PathVariable String id) {
		try {
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
		catch (Exception e) {
			logger.error("禁用配置失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "禁用配置失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取支持的提示词类型列表
	 * @return 提示词类型列表
	 */
	@GetMapping("/types")
	public ResponseEntity<Map<String, Object>> getSupportedPromptTypes() {
		try {
			// 支持的提示词类型
			String[] types = { "report-generator", "planner", "sql-generator", "python-generator", "rewrite" };

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("data", types);

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("获取支持的提示词类型失败", e);

			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", "获取支持的提示词类型失败：" + e.getMessage());

			return ResponseEntity.badRequest().body(response);
		}
	}

}
