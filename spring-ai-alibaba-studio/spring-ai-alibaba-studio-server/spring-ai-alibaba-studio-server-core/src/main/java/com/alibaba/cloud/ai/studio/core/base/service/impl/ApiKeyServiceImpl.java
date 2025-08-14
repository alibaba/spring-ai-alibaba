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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.account.ApiKey;
import com.alibaba.cloud.ai.studio.core.base.service.ApiKeyService;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.ApiKeyEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.ApiKeyMapper;
import com.alibaba.cloud.ai.studio.core.utils.security.AESCryptUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.security.CryptoUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.*;

/**
 * Service implementation for managing API keys. Handles CRUD operations for API keys with
 * caching support.
 *
 * @since 1.0.0.3
 */
@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKeyEntity> implements ApiKeyService {

	/** Maximum number of API keys allowed per account */
	private static final int MAX_API_KEY_PER_ACCOUNT = 20;

	/** Mapper for API key database operations */
	private final ApiKeyMapper apiKeyMapper;

	/** Redis manager for caching operations */
	private final RedisManager redisManager;

	public ApiKeyServiceImpl(ApiKeyMapper apiKeyMapper, RedisManager redisManager) {
		this.apiKeyMapper = apiKeyMapper;
		this.redisManager = redisManager;
	}

	/**
	 * Creates a new API key for the current account. Enforces maximum API key limit per
	 * account.
	 * @param apiKey API key details
	 * @return ID of the created API key
	 */
	@Override
	public Long createApiKey(ApiKey apiKey) {
		RequestContext context = RequestContextHolder.getRequestContext();

		long apiKeyCount = getApiKeyCount(context.getAccountId());
		if (apiKeyCount >= MAX_API_KEY_PER_ACCOUNT) {
			throw new BizException(
					ErrorCode.INVALID_REQUEST.toError("api key can not be more than " + MAX_API_KEY_PER_ACCOUNT + "."));
		}

		ApiKeyEntity entity = BeanCopierUtils.copy(apiKey, ApiKeyEntity.class);
		String apiKeyString = IdGenerator.genApiKey();
		entity.setApiKey(AESCryptUtils.encrypt(apiKeyString));
		entity.setAccountId(context.getAccountId());
		entity.setGmtCreate(new Date());
		entity.setGmtModified(new Date());
		entity.setCreator(context.getAccountId());
		entity.setModifier(context.getAccountId());

		this.save(entity);

		// cache it
		String key = getApiKeyCacheKey(apiKeyString);
		redisManager.put(key, entity);

		String idKey = getApiKeyCacheKey(context.getAccountId(), entity.getId());
		redisManager.put(idKey, entity);

		return entity.getId();
	}

	/**
	 * Updates an existing API key's description.
	 * @param apiKey API key details to update
	 */
	@Override
	public void updateApiKey(ApiKey apiKey) {
		RequestContext context = RequestContextHolder.getRequestContext();

		ApiKeyEntity entity = getApiKeyById(context.getAccountId(), apiKey.getId());
		if (entity == null) {
			throw new BizException(ErrorCode.API_KEY_NOT_FOUND.toError());
		}

		entity.setDescription(apiKey.getDescription());
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());

		this.updateById(entity);

		// cache it
		String originalKey = AESCryptUtils.decrypt(entity.getApiKey());
		String key = getApiKeyCacheKey(originalKey);
		redisManager.put(key, entity);

		String idKey = getApiKeyCacheKey(context.getAccountId(), entity.getId());
		redisManager.put(idKey, entity);
	}

	/**
	 * Soft deletes an API key by marking it as deleted.
	 * @param id API key ID to delete
	 */
	@Override
	public void deleteApiKey(Long id) {
		RequestContext context = RequestContextHolder.getRequestContext();

		// delete from db
		ApiKeyEntity entity = getApiKeyById(context.getAccountId(), id);
		if (entity == null) {
			return;
		}

		// delete api key
		entity.setStatus(CommonStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);

		// delete from cache
		String originalKey = AESCryptUtils.decrypt(entity.getApiKey());
		String key = getApiKeyCacheKey(originalKey);
		redisManager.delete(key);

		String idKey = getApiKeyCacheKey(context.getAccountId(), entity.getId());
		redisManager.delete(idKey);
	}

	/**
	 * Lists API keys for the current account with pagination.
	 * @param query Pagination query parameters
	 * @return Paginated list of API keys
	 */
	@Override
	public PagingList<ApiKey> listApiKeys(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApiKeyEntity::getAccountId, context.getAccountId());
		queryWrapper.ne(ApiKeyEntity::getStatus, CommonStatus.DELETED.getStatus());
		queryWrapper.orderByDesc(ApiKeyEntity::getId);

		Page<ApiKeyEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<ApiKeyEntity> pageResult = this.page(page, queryWrapper);

		List<ApiKey> accounts;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			accounts = new ArrayList<>();
		}
		else {
			accounts = pageResult.getRecords().stream().map(this::toApiKeyDO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), accounts);
	}

	/**
	 * Retrieves an API key by its ID.
	 * @param id API key ID
	 * @return API key details
	 */
	@Override
	public ApiKey getApiKey(Long id) {
		RequestContext context = RequestContextHolder.getRequestContext();
		ApiKeyEntity entity = getApiKeyById(context.getAccountId(), id);
		if (entity == null) {
			throw new BizException(ErrorCode.API_KEY_NOT_FOUND.toError());
		}

		return toApiKeyDO(entity, false);
	}

	/**
	 * Retrieves an API key by its key string.
	 * @param apiKey API key string
	 * @return API key details
	 */
	@Override
	public ApiKey getApiKey(String apiKey) {
		ApiKeyEntity entity = getApiKeyEntity(apiKey);
		if (entity == null) {
			throw new BizException(ErrorCode.API_KEY_NOT_FOUND.toError());
		}

		return toApiKeyDO(entity);
	}

	/**
	 * Gets the count of active API keys for an account.
	 * @param uid Account ID
	 * @return Number of active API keys
	 */
	private long getApiKeyCount(String uid) {
		LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApiKeyEntity::getAccountId, uid).ne(ApiKeyEntity::getStatus, CommonStatus.DELETED.getStatus());

		return apiKeyMapper.selectCount(queryWrapper);
	}

	/**
	 * Generates cache key for API key lookup.
	 * @param apiKey API key string
	 * @return Cache key
	 */
	public static String getApiKeyCacheKey(String apiKey) {
		return String.format(CACHE_API_KEY_PREFIX, apiKey);
	}

	/**
	 * Generates cache key for API key lookup by account ID and key ID.
	 * @param uid Account ID
	 * @param id API key ID
	 * @return Cache key
	 */
	public static String getApiKeyCacheKey(String uid, Long id) {
		return String.format(CACHE_API_KEY_ID_UID_PREFIX, uid, id);
	}

	/**
	 * Retrieves API key entity by ID with caching.
	 * @param uid Account ID
	 * @param id API key ID
	 * @return API key entity
	 */
	private ApiKeyEntity getApiKeyById(String uid, Long id) {
		RequestContext context = RequestContextHolder.getRequestContext();

		String key = getApiKeyCacheKey(context.getAccountId(), id);
		ApiKeyEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApiKeyEntity::getId, id)
			.eq(ApiKeyEntity::getAccountId, uid)
			.ne(ApiKeyEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<ApiKeyEntity> entityOptional = this.getOneOpt(queryWrapper);
		entity = entityOptional.orElse(null);
		if (entity == null) {
			entity = new ApiKeyEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Retrieves API key entity by key string with caching.
	 * @param apiKey API key string
	 * @return API key entity
	 */
	private ApiKeyEntity getApiKeyEntity(String apiKey) {
		String key = getApiKeyCacheKey(apiKey);
		ApiKeyEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		String encrypted = AESCryptUtils.encrypt(apiKey);
		LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ApiKeyEntity::getApiKey, encrypted)
			.ne(ApiKeyEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<ApiKeyEntity> entityOptional = this.getOneOpt(queryWrapper);
		entity = entityOptional.orElse(null);
		if (entity == null) {
			entity = new ApiKeyEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Converts API key entity to DTO with optional masking.
	 * @param entity API key entity
	 * @return API key DTO
	 */
	private ApiKey toApiKeyDO(ApiKeyEntity entity) {
		return toApiKeyDO(entity, true);
	}

	/**
	 * Converts API key entity to DTO with configurable masking.
	 * @param entity API key entity
	 * @param withMask Whether to mask the API key
	 * @return API key DTO
	 */
	private ApiKey toApiKeyDO(ApiKeyEntity entity, boolean withMask) {
		if (entity == null) {
			return null;
		}

		ApiKey apiKey = BeanCopierUtils.copy(entity, ApiKey.class);
		String originApiKey = AESCryptUtils.decrypt(apiKey.getApiKey());
		if (withMask) {
			apiKey.setApiKey(CryptoUtils.mask(originApiKey));
		}
		else {
			apiKey.setApiKey(originApiKey);
		}

		return apiKey;
	}

}
