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
import com.alibaba.cloud.ai.example.manus.dynamic.model.exception.AuthenticationException;
import com.alibaba.cloud.ai.example.manus.dynamic.model.exception.NetworkException;
import com.alibaba.cloud.ai.example.manus.dynamic.model.exception.RateLimitException;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.AvailableModel;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.ModelConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.ValidationResult;
import com.alibaba.cloud.ai.example.manus.dynamic.model.repository.DynamicModelRepository;
import com.alibaba.cloud.ai.example.manus.event.JmanusEventPublisher;
import com.alibaba.cloud.ai.example.manus.event.ModelChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelServiceImpl implements ModelService {

	private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

	private final DynamicModelRepository repository;

	private final DynamicAgentRepository agentRepository;

	@Autowired
	private JmanusEventPublisher publisher;

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
				return updateModel(existingModel);
			}

			DynamicModelEntity entity = new DynamicModelEntity();
			entity.setAllowChange(true);
			updateEntityFromConfig(entity, config);
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
		DynamicModelEntity entity = repository.findById(config.getId())
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + config.getId()));
		updateEntityFromConfig(entity, config);
		return updateModel(entity);
	}

	public ModelConfig updateModel(DynamicModelEntity entity) {
		// 如果不允许修改，则返回原有数据
		if (!entity.isAllowChange()) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
		entity = repository.save(entity);
		publisher.publish(new ModelChangeEvent(entity));
		return entity.mapToModelConfig();
	}

	@Override
	public void deleteModel(String id) {
		DynamicModelEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
		// 如果不允许修改，则返回原有数据
		if (entity.isAllowChange()) {
			List<DynamicAgentEntity> allByModel = agentRepository
				.findAllByModel(new DynamicModelEntity(Long.parseLong(id)));
			if (allByModel != null && !allByModel.isEmpty()) {
				allByModel.forEach(dynamicAgentEntity -> dynamicAgentEntity.setModel(null));
				agentRepository.saveAll(allByModel);
			}
			repository.deleteById(Long.parseLong(id));
		}
		else {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	@Override
	public ValidationResult validateConfig(String baseUrl, String apiKey) {
		log.info("开始验证模型配置 - Base URL: {}, API Key: {}", baseUrl, maskApiKey(apiKey));

		ValidationResult result = new ValidationResult();

		try {
			// 1. 验证Base URL格式
			log.debug("验证Base URL格式: {}", baseUrl);
			if (!isValidBaseUrl(baseUrl)) {
				log.warn("Base URL格式验证失败: {}", baseUrl);
				result.setValid(false);
				result.setMessage("Base URL格式不正确");
				return result;
			}
			log.debug("Base URL格式验证通过");

			// 2. 验证API Key格式
			log.debug("验证API Key格式");
			if (!isValidApiKey(apiKey)) {
				log.warn("API Key格式验证失败");
				result.setValid(false);
				result.setMessage("API Key格式不正确");
				return result;
			}
			log.debug("API Key格式验证通过");

			// 3. 调用第三方API验证
			log.info("开始调用第三方API验证配置");
			List<AvailableModel> models = callThirdPartyApi(baseUrl, apiKey);

			result.setValid(true);
			result.setMessage("验证成功");
			result.setAvailableModels(models);

			log.info("第三方API验证成功，获取到 {} 个可用模型", models.size());

		}
		catch (AuthenticationException e) {
			log.error("API Key认证失败: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("API Key无效或已过期");
		}
		catch (NetworkException e) {
			log.error("网络连接验证失败: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("网络连接失败，请检查Base URL");
		}
		catch (RateLimitException e) {
			log.error("请求频率限制: {}", e.getMessage());
			result.setValid(false);
			result.setMessage("请求频率过高，请稍后重试");
		}
		catch (Exception e) {
			log.error("验证过程中发生未知异常: {}", e.getMessage(), e);
			result.setValid(false);
			result.setMessage("验证失败: " + e.getMessage());
		}

		log.info("模型配置验证完成 - {}", result.isValid() ? "成功" : "失败");
		log.info("模型配置验证结果 - 有效: {}, 消息: {}", result.isValid(), result.getMessage());

		return result;
	}

	private boolean isValidBaseUrl(String baseUrl) {
		try {
			URL url = new URL(baseUrl);
			boolean isValid = "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
			log.debug("Base URL验证结果: {} - 协议: {}, 主机: {}", isValid, url.getProtocol(), url.getHost());
			return isValid;
		}
		catch (MalformedURLException e) {
			log.debug("Base URL格式无效: {} - 错误: {}", baseUrl, e.getMessage());
			return false;
		}
	}

	private boolean isValidApiKey(String apiKey) {
		boolean isValid = apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() >= 10;
		log.debug("API Key验证结果: {} - 长度: {}", isValid, apiKey != null ? apiKey.length() : 0);
		return isValid;
	}

	private String maskApiKey(String apiKey) {
		if (apiKey == null || apiKey.length() <= 8) {
			return "***";
		}
		return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
	}

	private List<AvailableModel> callThirdPartyApi(String baseUrl, String apiKey) {
		log.debug("开始调用第三方API - URL: {}", baseUrl);

		RestTemplate restTemplate = new RestTemplate();

		// 设置请求头
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + apiKey);
		headers.set("Content-Type", "application/json");

		log.debug("设置请求头 - Content-Type: application/json, Authorization: Bearer {}", maskApiKey(apiKey));

		// 构建请求URL
		String requestUrl = baseUrl + "/v1/models";
		log.info("发送HTTP请求到: {}", requestUrl);

		try {
			long startTime = System.currentTimeMillis();
			// 发送GET请求
			ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);
			long endTime = System.currentTimeMillis();

			log.info("HTTP请求完成 - 状态码: {}, 耗时: {}ms", response.getStatusCodeValue(), endTime - startTime);

			// 解析响应
			List<AvailableModel> models = parseModelsResponse(response.getBody());
			log.info("成功解析响应，获取到 {} 个模型", models.size());

			return models;

		}
		catch (Exception e) {
			log.error("API调用失败: {}", e.getMessage(), e);
			throw new NetworkException("API调用失败: " + e.getMessage(), e);
		}
	}

	private List<AvailableModel> parseModelsResponse(Map response) {
		log.debug("开始解析API响应: {}", response);

		List<AvailableModel> models = new ArrayList<>();

		if (response == null) {
			log.warn("响应为空");
			return models;
		}

		// 尝试解析标准OpenAI格式: {"data": [...]}
		Object data = response.get("data");
		if (data instanceof List) {
			List<Map> modelList = (List<Map>) data;
			log.debug("找到响应数据，包含 {} 个模型", modelList.size());

			for (int i = 0; i < modelList.size(); i++) {
				Map modelData = modelList.get(i);
				log.debug("解析第 {} 个模型数据: {}", i + 1, modelData);

				String modelId = (String) modelData.get("id");
				String modelName = (String) modelData.get("name");
				String description = (String) modelData.get("description");

				// 如果没有name字段，使用id作为显示名称
				if (modelName == null) {
					modelName = modelId;
				}

				// 如果没有description字段，使用默认描述
				if (description == null) {
					description = "模型ID: " + modelId;
				}

				log.debug("解析模型 - ID: {}, 名称: {}, 描述: {}", modelId, modelName, description);

				models.add(new AvailableModel(modelId, modelName, description));
			}
		}
		else {
			log.warn("响应格式不符合预期，data字段不是数组类型");
		}

		log.info("成功解析响应，获取到 {} 个可用模型", models.size());
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
	}

}
