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
package com.alibaba.cloud.ai.studio.core.base.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.DataSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.entity.ProviderEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.ProviderMapper;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelCredential;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ProviderConfigInfo;
import com.alibaba.cloud.ai.studio.core.utils.security.CryptoUtils;
import com.alibaba.cloud.ai.studio.core.utils.security.RSACryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_EMPTY_ID;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_WORKSPACE_PROVIDER_PREFIX;

/**
 * Provider management service for handling LLM provider operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderManager extends ServiceImpl<ProviderMapper, ProviderEntity> {

	private final RedisManager redisManager;

	/**
	 * Add a new provider
	 * @param providerConfigInfo Provider configuration information
	 * @return true if successful
	 */
	public boolean addProvider(ProviderConfigInfo providerConfigInfo) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		try {
			// 检查提供商是否存在
			QueryWrapper<ProviderEntity> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("provider", providerConfigInfo.getProvider());
			if (StringUtils.isNotBlank(context.getWorkspaceId())) {
				queryWrapper.eq("workspace_id", context.getWorkspaceId());
			}
			String provider = providerConfigInfo.getProvider();
			ProviderEntity existingProvider = getProviderEntity(provider, workspaceId);
			if (existingProvider != null) {
				log.error("provider [{}] already exist", providerConfigInfo.getProvider());
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider already exists"));
			}

			ProviderEntity providerEntity = new ProviderEntity();
			providerEntity.setWorkspaceId(context.getWorkspaceId());
			providerEntity.setGmtCreate(new Date());
			providerEntity.setGmtModified(new Date());
			providerEntity.setIcon(providerConfigInfo.getIcon());
			providerEntity.setName(providerConfigInfo.getName());
			providerEntity.setProvider(providerConfigInfo.getProvider());
			providerEntity.setDescription(providerConfigInfo.getDescription());
			providerEntity.setSource(StringUtils.isNotBlank(providerConfigInfo.getSource())
					? providerConfigInfo.getSource() : DataSourceEnum.custom.name());
			providerEntity.setEnable(providerConfigInfo.getEnable() != null ? providerConfigInfo.getEnable() : true);
			providerEntity.setCredential(providerConfigInfo.getCredential() == null ? "{}"
					: JsonUtils.toJson(providerConfigInfo.getCredential()));
			providerEntity.setSupportedModelTypes(
					providerConfigInfo.getSupportedModelTypes().stream().collect(Collectors.joining(",")));
			providerEntity.setCreator(context.getAccountId());
			providerEntity.setModifier(context.getAccountId());
			providerEntity.setProtocol(StringUtils.isNotBlank(providerConfigInfo.getProtocol())
					? providerConfigInfo.getProtocol() : "openai");

			// update db
			boolean isSuccess = this.save(providerEntity);

			// cache it
			if (isSuccess) {
				String key = getProviderCacheKey(provider, workspaceId);
				redisManager.put(key, providerEntity);

				String nonWorkspaceKey = getProviderCacheKey(provider, null);
				redisManager.put(nonWorkspaceKey, providerEntity);
			}

			return isSuccess;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("新增提供商失败: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Update an existing provider
	 * @param providerConfigInfo Provider configuration information
	 * @return true if successful
	 */
	public boolean updateProvider(ProviderConfigInfo providerConfigInfo) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		try {
			// 检查提供商是否存在
			String provider = providerConfigInfo.getProvider();
			ProviderEntity existingProvider = getProviderEntity(provider, workspaceId);
			if (existingProvider == null) {
				log.error("provider [{}] does not exist", providerConfigInfo.getProvider());
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider not found"));
			}

			// 更新提供商信息
			existingProvider.setGmtModified(new Date());
			existingProvider.setModifier(context.getAccountId());

			if (StringUtils.isNotBlank(providerConfigInfo.getName())) {
				existingProvider.setName(providerConfigInfo.getName());
			}
			if (StringUtils.isNotBlank(providerConfigInfo.getIcon())) {
				existingProvider.setIcon(providerConfigInfo.getIcon());
			}
			if (StringUtils.isNotBlank(providerConfigInfo.getDescription())) {
				existingProvider.setDescription(providerConfigInfo.getDescription());
			}
			if (providerConfigInfo.getCredential() != null) {
				existingProvider.setCredential(JsonUtils.toJson(providerConfigInfo.getCredential()));
			}
			if (providerConfigInfo.getEnable() != null) {
				existingProvider.setEnable(providerConfigInfo.getEnable());
			}
			if (CollectionUtils.isNotEmpty(providerConfigInfo.getSupportedModelTypes())) {
				existingProvider.setSupportedModelTypes(
						providerConfigInfo.getSupportedModelTypes().stream().collect(Collectors.joining(",")));
			}
			if (StringUtils.isNotBlank(providerConfigInfo.getProtocol())) {
				existingProvider.setProtocol(providerConfigInfo.getProtocol());
			}

			// update db
			boolean isSuccess = this.updateById(existingProvider);

			// cache it
			if (isSuccess) {
				String key = getProviderCacheKey(provider, workspaceId);
				redisManager.put(key, existingProvider);

				String nonWorkspaceKey = getProviderCacheKey(provider, null);
				redisManager.put(nonWorkspaceKey, existingProvider);
			}

			return isSuccess;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("update provider error: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Delete a provider
	 * @param provider Provider ID
	 * @return true if successful
	 */
	public boolean deleteProvider(String provider) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context == null ? null : context.getWorkspaceId();
		try {
			ProviderEntity existingProvider = getProviderEntity(provider, workspaceId);
			if (existingProvider == null) {
				log.error("provider [{}]does not exist", provider);
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider not found"));
			}

			// delete from db
			boolean isSuccess = this.removeById(existingProvider.getId());

			// delete from cache
			if (isSuccess) {
				String key = getProviderCacheKey(provider, workspaceId);
				redisManager.delete(key);

				String nonWorkspaceKey = getProviderCacheKey(provider, null);
				redisManager.delete(nonWorkspaceKey);
			}

			return isSuccess;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("delete provider error: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Query provider list
	 * @param name Provider name for filtering
	 * @return List of provider configurations
	 */
	public List<ProviderConfigInfo> queryProviders(String name) {
		RequestContext context = RequestContextHolder.getRequestContext();
		try {
			LambdaQueryWrapper<ProviderEntity> queryWrapper = new LambdaQueryWrapper<>();
			if (StringUtils.isNotBlank(context.getWorkspaceId())) {
				queryWrapper.eq(ProviderEntity::getWorkspaceId, context.getWorkspaceId());
			}
			if (StringUtils.isNotBlank(name)) {
				queryWrapper.like(ProviderEntity::getName, name);
			}
			queryWrapper.orderByDesc(ProviderEntity::getId);
			List<ProviderEntity> providerEntities = this.list(queryWrapper);
			return providerEntities.stream().map(x -> toProviderConfig(x, true)).collect(Collectors.toList());
		}
		catch (Exception e) {
			log.error("query providers error: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get provider details
	 * @param provider Provider ID
	 * @param mask Whether to mask sensitive information
	 * @return Provider configuration information
	 */
	public ProviderConfigInfo getProviderDetail(String provider, boolean mask) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context == null ? null : context.getWorkspaceId();
		try {
			ProviderEntity providerEntity = getProviderEntity(provider, workspaceId);
			if (providerEntity == null) {
				log.error("provider [{}]does not exists.", provider);
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("input_params", "provider not found"));
			}

			return toProviderConfig(providerEntity, mask);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("get provider error: {}", e.getMessage(), e);
			throw new BizException(ErrorCode.SYSTEM_ERROR.toError());
		}
	}

	/**
	 * Get provider entity from cache or database
	 * @param provider Provider ID
	 * @param workspaceId Workspace ID
	 * @return Provider entity
	 */
	private ProviderEntity getProviderEntity(String provider, String workspaceId) {
		String key = getProviderCacheKey(provider, workspaceId);
		ProviderEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<ProviderEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ProviderEntity::getProvider, provider);
		if (StringUtils.isNotBlank(workspaceId)) {
			queryWrapper.eq(ProviderEntity::getWorkspaceId, workspaceId);
		}

		Optional<ProviderEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new ProviderEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		entity = entityOptional.get();
		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Generate cache key for provider
	 * @param provider Provider ID
	 * @param workspaceId Workspace ID
	 * @return Cache key string
	 */
	public static String getProviderCacheKey(String provider, String workspaceId) {
		if (StringUtils.isBlank(workspaceId)) {
			workspaceId = "-";
		}

		return String.format(CACHE_WORKSPACE_PROVIDER_PREFIX, workspaceId, provider);
	}

	/**
	 * Convert provider entity to configuration info
	 * @param entity Provider entity
	 * @param mask Whether to mask sensitive information
	 * @return Provider configuration information
	 */
	private ProviderConfigInfo toProviderConfig(ProviderEntity entity, boolean mask) {
		ProviderConfigInfo configInfo = new ProviderConfigInfo();
		configInfo.setProvider(entity.getProvider());
		configInfo.setName(entity.getName());
		configInfo.setDescription(entity.getDescription());
		configInfo.setIcon(entity.getIcon());
		configInfo.setEnable(entity.getEnable());
		configInfo.setSource(entity.getSource());
		if (StringUtils.isNotBlank(entity.getSupportedModelTypes())) {
			configInfo.setSupportedModelTypes(Arrays.asList(entity.getSupportedModelTypes().split(",")));
		}
		else {
			configInfo.setSupportedModelTypes(new ArrayList<>());
		}

		if (StringUtils.isNotBlank(entity.getCredential())) {
			ModelCredential credential = JsonUtils.fromJson(entity.getCredential(), ModelCredential.class);
			if (StringUtils.isNotBlank(credential.getApiKey())) {
				try {
					String key = RSACryptUtils.decrypt(credential.getApiKey());
					if (mask) {
						key = CryptoUtils.mask(key);
					}

					credential.setApiKey(key);
				}
				catch (Exception e) {
					credential.setApiKey(null);
				}
			}

			configInfo.setCredential(credential);
		}

		configInfo.setGmtCreate(entity.getGmtCreate());
		configInfo.setGmtModified(entity.getGmtModified());
		configInfo.setCreator(entity.getCreator());
		configInfo.setModifier(entity.getModifier());
		configInfo.setProtocol(entity.getProtocol());
		return configInfo;
	}

}
