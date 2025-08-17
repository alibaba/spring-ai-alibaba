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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.model.AddModelRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.model.AddProviderRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import com.alibaba.cloud.ai.studio.runtime.domain.model.QueryProviderRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.model.UpdateModelRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.model.UpdateProviderRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.enums.DataSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelManager;
import com.alibaba.cloud.ai.studio.core.base.manager.ProviderManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelProvider;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelCredential;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ParameterRule;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.security.RSACryptUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_PROVIDER_LIST_CACHE_PREFIX;

/**
 * Controller for managing model providers and their configurations in the SAA Studio
 * platform.
 *
 * This controller provides comprehensive REST endpoints for:
 * <ul>
 * <li>Provider lifecycle management (CRUD operations)</li>
 * <li>Model management within providers</li>
 * <li>Credential and authentication handling</li>
 * <li>Protocol and configuration management</li>
 * <li>Parameter rule management for models</li>
 * </ul>
 *
 * Key features: - Support for multiple provider protocols (OpenAI, custom, etc.) - Secure
 * credential management with RSA encryption - Model type and parameter validation -
 * Caching and performance optimization - Comprehensive error handling
 *
 * @since 1.0.0.3
 */
@Slf4j
@RestController
@Tag(name = "model_management")
@RequestMapping("/console/v1/providers")
public class ProviderController {

	private final ProviderManager providerManager;

	private final Map<String, ModelProvider> providerMap;

	private final ModelManager modelManager;

	private final RedisManager redisManager;

	public ProviderController(ProviderManager providerManager, Map<String, ModelProvider> providerMap,
			ModelManager modelManager, RedisManager redisManager) {
		this.providerManager = providerManager;
		this.providerMap = providerMap;
		this.modelManager = modelManager;
		this.redisManager = redisManager;
	}

	/**
	 * Adds a new model provider to the system.
	 *
	 * This endpoint creates a new provider with the following features: - Generates a
	 * unique provider code - Configures provider metadata (name, description, icon) -
	 * Sets up supported model types - Handles credential encryption for sensitive data -
	 * Manages protocol-specific configurations
	 * @param request The provider creation request containing provider details
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if required parameters are missing or invalid
	 */
	@PostMapping
	public Result<Boolean> addProvider(@RequestBody AddProviderRequest request) {
		if (request == null || StringUtils.isBlank(request.getName())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "request is valid"));
		}

		// Create provider configuration information
		ProviderConfigInfo providerConfigInfo = new ProviderConfigInfo();
		// Generate an 8-character random code
		String providerCode = IdGenerator.uuid().substring(0, 8);
		providerConfigInfo.setProvider(providerCode);
		providerConfigInfo.setName(request.getName());
		providerConfigInfo.setDescription(request.getDescription());
		providerConfigInfo.setIcon(request.getIcon());
		providerConfigInfo.setSource(DataSourceEnum.custom.name());
		providerConfigInfo.setEnable(true);
		List<String> supportedModelTypes = Lists.newArrayList();
		if (StringUtils.isNotBlank(request.getSupportedModelTypes())) {
			supportedModelTypes
				.addAll(Arrays.stream(request.getSupportedModelTypes().split(",")).collect(Collectors.toList()));
		}
		else {
			// Set default to llm
			supportedModelTypes.addAll(Lists.newArrayList(ModelConfigInfo.ModelTypeEnum.llm.name()));
		}
		providerConfigInfo.setSupportedModelTypes(supportedModelTypes);

		// Set protocol
		if (StringUtils.isNotBlank(request.getProtocol())) {
			providerConfigInfo.setProtocol(request.getProtocol());
		}

		// Handle credential information
		if (request.getCredentialConfig() != null) {
			Map<String, Object> credentialConfig = request.getCredentialConfig();
			ModelCredential credential = new ModelCredential();
			// For OpenAI protocol, handle API key encryption
			if (StringUtils.isBlank(request.getProtocol()) || "openai".equals(request.getProtocol().toLowerCase())) {
				String endpoint = MapUtils.getString(credentialConfig, "endpoint");
				String apikey = MapUtils.getString(credentialConfig, "api_key");
				credential.setEndpoint(endpoint);
				credential.setApiKey(RSACryptUtils.encrypt(apikey));
			}
			providerConfigInfo.setCredential(credential);
		}
		boolean b = providerManager.addProvider(providerConfigInfo);
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		redisManager.delete(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		return Result.success(b);
	}

	/**
	 * Updates an existing model provider.
	 *
	 * This endpoint modifies provider configurations including: - Basic provider
	 * information - Supported model types - Protocol settings - Credential configurations
	 * - Provider status (enabled/disabled)
	 * @param provider The provider code to update
	 * @param request The update request containing new provider details
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if required parameters are missing or invalid
	 */
	@PutMapping("/{provider}")
	public Result<Boolean> updateProvider(@PathVariable("provider") String provider,
			@RequestBody UpdateProviderRequest request) {
		if (request == null || provider == null) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "request is valid"));
		}
		// Create provider configuration information
		ProviderConfigInfo providerConfigInfo = new ProviderConfigInfo();
		providerConfigInfo.setProvider(provider);
		providerConfigInfo.setName(request.getName());
		providerConfigInfo.setDescription(request.getDescription());
		providerConfigInfo.setIcon(request.getIcon());
		providerConfigInfo.setSource(DataSourceEnum.custom.name());
		providerConfigInfo.setEnable(request.getEnable() == null ? true : request.getEnable());
		List<String> supportedModelTypes = null;
		if (StringUtils.isNotBlank(request.getSupportedModelTypes())) {
			supportedModelTypes = Lists.newArrayList();
			supportedModelTypes
				.addAll(Arrays.stream(request.getSupportedModelTypes().split(",")).collect(Collectors.toList()));
		}
		providerConfigInfo.setSupportedModelTypes(supportedModelTypes);

		// Set protocol
		if (StringUtils.isNotBlank(request.getProtocol())) {
			providerConfigInfo.setProtocol(request.getProtocol());
		}

		// Handle credential information
		if (request.getCredentialConfig() != null) {
			Map<String, Object> credentialConfig = request.getCredentialConfig();
			// For OpenAI protocol, handle API key encryption
			ModelCredential credential = null;
			if (StringUtils.isBlank(request.getProtocol()) || "openai".equals(request.getProtocol().toLowerCase())) {
				credential = buildOpenaiCredentialConfig(credentialConfig);
			}
			else {
				List<CredentialSpec> credentialSpecs = providerMap.get(provider + "Provider").getCredentialSpecs();
				if (CollectionUtils.isEmpty(credentialSpecs)) {
					credential = buildOpenaiCredentialConfig(credentialConfig);
				}
				else {
					Map<String, Object> tmpCredentialConfig = Maps.newHashMap();
					credentialSpecs.stream().forEach(credentialSpec -> {
						if (credentialSpec.isSensitive()) {
							tmpCredentialConfig.put(credentialSpec.getCode(), RSACryptUtils
								.encrypt(MapUtils.getString(credentialConfig, credentialSpec.getCode())));
						}
						else {
							tmpCredentialConfig.put(credentialSpec.getCode(),
									MapUtils.getString(credentialConfig, credentialSpec.getCode()));
						}
					});
				}
			}
			providerConfigInfo.setCredential(credential);
		}
		boolean b = providerManager.updateProvider(providerConfigInfo);
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		redisManager.delete(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		return Result.success(b);
	}

	private ModelCredential buildOpenaiCredentialConfig(Map<String, Object> credentialConfig) {
		String endpoint = MapUtils.getString(credentialConfig, "endpoint");
		String apikey = MapUtils.getString(credentialConfig, "api_key");

		ModelCredential credential = new ModelCredential();
		credential.setEndpoint(endpoint);
		credential.setApiKey(RSACryptUtils.encrypt(apikey));

		return credential;
	}

	/**
	 * Deletes a model provider from the system.
	 *
	 * This endpoint removes a provider and its associated configurations: - Removes
	 * provider metadata - Cleans up associated models - Updates cache
	 * @param provider The provider code to delete
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if provider is not found or deletion fails
	 */
	@DeleteMapping("/{provider}")
	public Result<Boolean> deleteProvider(@PathVariable("provider") String provider) {
		if (StringUtils.isBlank(provider)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "providerId is required"));
		}

		boolean b = providerManager.deleteProvider(provider);
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		redisManager.delete(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		return Result.success(b);
	}

	/**
	 * Queries available model providers.
	 *
	 * This endpoint retrieves a list of providers based on query parameters: - Supports
	 * filtering by various criteria - Returns paginated results - Includes provider
	 * metadata and status
	 * @param request The query parameters for filtering providers
	 * @return Result containing a list of matching providers
	 */
	@GetMapping
	public Result<List<ProviderConfigInfo>> queryProviders(@ModelAttribute QueryProviderRequest request) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (request == null) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "request is valid"));
		}
		List<ProviderConfigInfo> providerConfigInfos = redisManager
			.get(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		if (providerConfigInfos != null) {
			return Result.success(providerConfigInfos);
		}
		providerConfigInfos = providerManager.queryProviders(request.getName());
		if (CollectionUtils.isNotEmpty(providerConfigInfos)) {
			// Clear sensitive information in list view
			providerConfigInfos.stream().forEach(providerConfigInfo -> {
				providerConfigInfo.setCredential(null);
				List<ModelConfigInfo> modelConfigInfos = modelManager.queryModels(providerConfigInfo.getProvider());
				if (CollectionUtils.isNotEmpty(modelConfigInfos)) {
					providerConfigInfo.setModelCount(modelConfigInfos.size());
				}
				else {
					providerConfigInfo.setModelCount(0);
				}
			});
		}
		redisManager.put(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId(), providerConfigInfos,
				Duration.ofHours(12));
		return Result.success(providerConfigInfos);
	}

	/**
	 * Retrieves detailed information about a specific provider.
	 *
	 * This endpoint returns comprehensive provider information including: - Provider
	 * metadata - Configuration details - Supported model types - Protocol information
	 * @param provider The provider code to retrieve
	 * @return Result containing the provider details
	 * @throws BizException if provider is not found
	 */
	@GetMapping("/{provider}")
	public Result<ProviderConfigInfo> getProviderDetail(@PathVariable("provider") String provider) {
		if (StringUtils.isBlank(provider)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "providerId is required"));
		}
		ProviderConfigInfo providerDetail = providerManager.getProviderDetail(provider, false);
		// 处理验权凭证结构返回
		ModelProvider providerInstance = providerMap.get(provider + "Provider");
		if (providerInstance == null) {
			providerDetail.setCredentialSpecs(providerMap.get("OpenAIProvider").getCredentialSpecs());
		}
		else {
			providerDetail.setCredentialSpecs(providerInstance.getCredentialSpecs());
		}
		return Result.success(providerDetail);
	}

	/**
	 * Adds a new model to a provider.
	 *
	 * This endpoint creates a new model configuration: - Validates model parameters -
	 * Sets up model metadata - Configures model-specific settings
	 * @param provider The provider code
	 * @param request The model creation request
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if required parameters are missing or invalid
	 */
	@PostMapping("/{provider}/models")
	public Result<Boolean> addModel(@PathVariable("provider") String provider, @RequestBody AddModelRequest request) {
		if (request == null || StringUtils.isBlank(request.getModelId())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "request is valid"));
		}
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		String tagStrings = request.getTags();
		List<String> tags = Lists.newArrayList();
		if (StringUtils.isNotBlank(tagStrings)) {
			tags.addAll(Arrays.stream(tagStrings.split(",")).collect(Collectors.toList()));
		}
		// Create model configuration information
		ModelConfigInfo modelConfigInfo = new ModelConfigInfo();
		modelConfigInfo.setModelId(request.getModelId().trim());
		modelConfigInfo.setName(request.getModelName());
		modelConfigInfo.setProvider(provider);
		modelConfigInfo.setMode(ModelConfigInfo.ModeEnum.chat.name());
		modelConfigInfo.setTags(tags);
		String type = request.getType() == null ? ModelConfigInfo.ModelTypeEnum.llm.name() : request.getType();
		modelConfigInfo.setType(type);
		boolean b = modelManager.addModel(modelConfigInfo);
		redisManager.delete(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		return Result.success(b);
	}

	/**
	 * Updates an existing model configuration.
	 *
	 * This endpoint modifies model settings including: - Model metadata (name, tags,
	 * icon) - Model status (enabled/disabled) - Model-specific configurations
	 * @param provider The provider code
	 * @param modelId The model identifier
	 * @param request The update request containing new model details
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if required parameters are missing or invalid
	 */
	@PutMapping("/{provider}/models/{modelId}")
	public Result<Boolean> updateModel(@PathVariable("provider") String provider,
			@PathVariable("modelId") String modelId, @RequestBody UpdateModelRequest request) {
		if (request == null) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "request is valid"));
		}
		request.setModelId(modelId);
		String tagStrings = request.getTags();
		List<String> tags = Lists.newArrayList();
		if (StringUtils.isNotBlank(tagStrings)) {
			tags.addAll(Arrays.stream(tagStrings.split(",")).collect(Collectors.toList()));
		}
		// Create model configuration information
		ModelConfigInfo modelConfigInfo = new ModelConfigInfo();
		modelConfigInfo.setModelId(request.getModelId());
		modelConfigInfo.setName(request.getModelName());
		modelConfigInfo.setProvider(provider);
		modelConfigInfo.setTags(tags);
		modelConfigInfo.setIcon(request.getIcon());
		modelConfigInfo.setEnable(request.getEnable());
		return Result.success(modelManager.updateModel(modelConfigInfo));
	}

	/**
	 * Deletes a model from a provider.
	 *
	 * This endpoint removes a model and its associated configurations: - Removes model
	 * metadata - Updates provider cache
	 * @param provider The provider code
	 * @param modelId The model identifier to delete
	 * @return Result indicating success or failure of the operation
	 * @throws BizException if model is not found or deletion fails
	 */
	@DeleteMapping("/{provider}/models/{modelId}")
	public Result<Boolean> deleteModel(@PathVariable("provider") String provider,
			@PathVariable("modelId") String modelId) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(modelId)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "modelId is required"));
		}
		redisManager.delete(CACHE_PROVIDER_LIST_CACHE_PREFIX + requestContext.getWorkspaceId());
		return Result.success(modelManager.deleteModel(provider, modelId));
	}

	/**
	 * Retrieves a list of models for a specific provider.
	 *
	 * This endpoint returns all models associated with a provider: - Includes model
	 * metadata - Includes model status - Includes model configurations
	 * @param provider The provider code
	 * @return Result containing a list of models
	 * @throws BizException if provider is not found
	 */
	@GetMapping("/{provider}/models")
	public Result<List<ModelConfigInfo>> queryModels(@PathVariable("provider") String provider) {
		return Result.success(modelManager.queryModels(provider));
	}

	/**
	 * Retrieves detailed information about a specific model.
	 *
	 * This endpoint returns comprehensive model information including: - Model metadata -
	 * Configuration details - Model type and parameters
	 * @param provider The provider code
	 * @param modelId The model identifier
	 * @return Result containing the model details
	 * @throws BizException if model is not found
	 */
	@GetMapping("/{provider}/models/{modelId}")
	public Result<ModelConfigInfo> getModelDetail(@PathVariable("provider") String provider,
			@PathVariable("modelId") String modelId) {
		if (StringUtils.isBlank(modelId)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "modelId is required"));
		}
		ModelConfigInfo modelDetail = modelManager.getModelDetail(provider, modelId);
		return Result.success(modelDetail);
	}

	/**
	 * Retrieves parameter rules for a specific model.
	 *
	 * This endpoint returns the parameter configuration rules for a model: - Parameter
	 * validation rules - Required and optional parameters - Parameter types and
	 * constraints
	 * @param provider The provider code
	 * @param modelId The model identifier
	 * @return Result containing the parameter rules
	 * @throws BizException if model is not found
	 */
	@GetMapping("/{provider}/models/{modelId}/parameter_rules")
	public Result<List<ParameterRule>> getModelParamRules(@PathVariable("provider") String provider,
			@PathVariable("modelId") String modelId) {
		if (StringUtils.isBlank(provider) || StringUtils.isBlank(modelId)) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider or modelId is required"));
		}

		// 获取模型详情
		ModelConfigInfo modelDetail = modelManager.getModelDetail(provider, modelId);
		if (modelDetail == null) {
			return Result.error(IdGenerator.uuid(), ErrorCode.MODEL_NOT_FOUND);
		}

		ModelProvider providerInstance = providerMap.get(provider + "Provider");
		if (providerInstance == null) {
			// 返回默认的
			providerInstance = providerMap.get("OpenAIProvider");
		}
		return Result.success(providerInstance.getParameterRules(modelId, modelDetail.getType()));
	}

	/**
	 * Retrieves a list of supported provider protocols.
	 *
	 * This endpoint returns all supported protocol types: - Protocol names - Protocol
	 * versions (if applicable)
	 * @return Result containing a list of supported protocols
	 */
	@GetMapping("/protocols")
	public Result<List<String>> getProviderProtocols() {
		// Return supported protocol types based on actual implementation
		// For example: OpenAI, Azure, Anthropic, etc.
		List<String> protocols = Lists.newArrayList("OpenAI");
		return Result.success(protocols);
	}

}
