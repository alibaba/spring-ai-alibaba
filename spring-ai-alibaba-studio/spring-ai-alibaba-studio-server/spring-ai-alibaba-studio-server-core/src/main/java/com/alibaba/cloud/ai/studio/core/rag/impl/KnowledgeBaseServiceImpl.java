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

package com.alibaba.cloud.ai.studio.core.rag.impl;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.KnowledgeBaseType;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.ProcessConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.KnowledgeBaseEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.KnowledgeBaseMapper;
import com.alibaba.cloud.ai.studio.core.rag.KnowledgeBaseService;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_EMPTY_ID;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.CACHE_KB_WORKSPACE_ID_PREFIX;

/**
 * Implementation of the knowledge base service. Handles CRUD operations for knowledge
 * bases including creation, retrieval, updating, and deletion. Manages caching and vector
 * store operations for knowledge bases.
 *
 * @since 1.0.0.3
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBaseEntity>
		implements KnowledgeBaseService {

	/** Redis manager for caching operations */
	private final RedisManager redisManager;

	/** Factory for creating vector store services */
	private final VectorStoreFactory vectorStoreFactory;

	public KnowledgeBaseServiceImpl(RedisManager redisManager, VectorStoreFactory vectorStoreFactory) {
		this.redisManager = redisManager;
		this.vectorStoreFactory = vectorStoreFactory;
	}

	/**
	 * Creates a new knowledge base with the given configuration
	 * @param kb Knowledge base configuration
	 * @return ID of the created knowledge base
	 */
	@Override
	public String createKnowledgeBase(KnowledgeBase kb) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		String kbId = IdGenerator.idStr();

		// check if knowledge base name exists
		KnowledgeBaseEntity entity = getKnowledgeBaseByName(context.getWorkspaceId(), kb.getName());
		if (entity != null) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NAME_EXISTS.toError());
		}

		entity = BeanCopierUtils.copy(kb, KnowledgeBaseEntity.class);
		entity.setKbId(kbId);
		entity.setWorkspaceId(workspaceId);
		entity.setType(KnowledgeBaseType.UNSTRUCTURED);
		entity.setStatus(CommonStatus.NORMAL);

		if (kb.getProcessConfig() != null) {
			entity.setProcessConfig(JsonUtils.toJson(kb.getProcessConfig()));
		}

		if (kb.getIndexConfig() != null) {
			kb.getIndexConfig().setName(kbId);
			entity.setIndexConfig(JsonUtils.toJson(kb.getIndexConfig()));
		}

		if (kb.getSearchConfig() != null) {
			entity.setSearchConfig(JsonUtils.toJson(kb.getSearchConfig()));
		}

		entity.setGmtCreate(new Date());
		entity.setGmtModified(new Date());
		entity.setCreator(context.getAccountId());
		entity.setModifier(context.getAccountId());

		// create vector store first
		vectorStoreFactory.getVectorStoreService().createIndex(kb.getIndexConfig());

		// save to db
		this.save(entity);

		// cache it
		String key = getKnowledgeBaseCacheKey(workspaceId, kbId);
		redisManager.put(key, entity);

		String nonWorkspaceKey = getKnowledgeBaseCacheKey(null, kbId);
		redisManager.put(nonWorkspaceKey, entity);

		return kbId;
	}

	/**
	 * Retrieves a knowledge base by its ID
	 * @param kbId Knowledge base ID
	 * @return Knowledge base details
	 */
	@Override
	public KnowledgeBase getKnowledgeBase(String kbId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context == null ? null : context.getWorkspaceId();
		KnowledgeBaseEntity entity = getKnowledgeBaseById(workspaceId, kbId);
		if (entity == null) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.toError());
		}

		return toKnowledgeBaseDTO(entity);
	}

	/**
	 * Lists knowledge bases with pagination and filtering
	 * @param query Query parameters including pagination and filters
	 * @return Paginated list of knowledge bases
	 */
	@Override
	public PagingList<KnowledgeBase> listKnowledgeBases(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		Page<KnowledgeBaseEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(KnowledgeBaseEntity::getWorkspaceId, workspaceId);
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(KnowledgeBaseEntity::getName, query.getName());
		}
		queryWrapper.ne(KnowledgeBaseEntity::getStatus, CommonStatus.DELETED.getStatus());
		queryWrapper.orderByDesc(KnowledgeBaseEntity::getId);

		IPage<KnowledgeBaseEntity> pageResult = this.page(page, queryWrapper);

		List<KnowledgeBase> knowledgeBases;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			knowledgeBases = new ArrayList<>();
		}
		else {
			knowledgeBases = pageResult.getRecords().stream().map(this::toKnowledgeBaseDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), knowledgeBases);
	}

	/**
	 * Updates an existing knowledge base
	 * @param kb Updated knowledge base configuration
	 */
	@Override
	public void updateKnowledgeBase(KnowledgeBase kb) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		KnowledgeBaseEntity entity = getKnowledgeBaseById(workspaceId, kb.getKbId());
		if (entity == null) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.toError());
		}

		// check if workspace name exists
		KnowledgeBaseEntity kbEntity = getKnowledgeBaseByName(workspaceId, kb.getName());
		if (kbEntity != null && !kbEntity.getId().equals(entity.getId())) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NAME_EXISTS.toError());
		}

		entity.setName(kb.getName());
		entity.setTotalDocs(kb.getTotalDocs());
		entity.setDescription(kb.getDescription());
		// TODO now do not support changing process config as it needs to re-index for all
		// documents
		// if (kb.getProcessConfig() != null) {
		// entity.setProcessConfig(JsonUtils.toJson(kb.getProcessConfig()));
		// }

		IndexConfig config = kb.getIndexConfig();
		if (config != null) {
			IndexConfig oldConfig = JsonUtils.fromJson(entity.getIndexConfig(), IndexConfig.class);
			config.setName(oldConfig.getName());
			entity.setIndexConfig(JsonUtils.toJson(config));
		}

		if (kb.getSearchConfig() != null) {
			entity.setSearchConfig(JsonUtils.toJson(kb.getSearchConfig()));
		}

		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());

		this.updateById(entity);

		// cache it
		String key = getKnowledgeBaseCacheKey(workspaceId, entity.getKbId());
		redisManager.put(key, entity);

		String nonWorkspaceKey = getKnowledgeBaseCacheKey(null, entity.getKbId());
		redisManager.put(nonWorkspaceKey, entity);
	}

	/**
	 * Deletes a knowledge base and its associated resources
	 * @param kbId ID of the knowledge base to delete
	 */
	@Override
	public void deleteKnowledgeBase(String kbId) {
		// TODO delete all documents and chunks first?
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		KnowledgeBaseEntity entity = getKnowledgeBaseById(workspaceId, kbId);
		if (entity == null) {
			throw new BizException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.toError());
		}

		// delete vector store
		KnowledgeBase kb = toKnowledgeBaseDTO(entity);
		vectorStoreFactory.getVectorStoreService().deleteIndex(kb.getIndexConfig());

		// delete from db
		entity.setStatus(CommonStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);

		// delete from cache
		String cacheKey = getKnowledgeBaseCacheKey(workspaceId, kbId);
		redisManager.delete(cacheKey);

		String nonWorkspaceKey = getKnowledgeBaseCacheKey(null, kbId);
		redisManager.delete(nonWorkspaceKey);
	}

	/**
	 * Retrieves multiple knowledge bases by their IDs
	 * @param kbIds List of knowledge base IDs
	 * @return List of knowledge base details
	 */
	@Override
	public List<KnowledgeBase> listKnowledgeBases(List<String> kbIds) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		List<String> needQueryKbIds = new ArrayList<>();
		List<KnowledgeBase> knowledgeBases = new ArrayList<>();

		// from cache first
		for (String kbId : kbIds) {
			KnowledgeBaseEntity entity = getKnowledgeBaseById(workspaceId, kbId);
			if (entity != null && !CacheConstants.CACHE_EMPTY_ID.equals(entity.getId())) {
				KnowledgeBase knowledgeBase = toKnowledgeBaseDTO(entity);
				knowledgeBases.add(knowledgeBase);
			}
			else {
				needQueryKbIds.add(kbId);
			}
		}

		if (needQueryKbIds.isEmpty()) {
			return knowledgeBases;
		}

		LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(KnowledgeBaseEntity::getWorkspaceId, workspaceId);
		queryWrapper.in(KnowledgeBaseEntity::getKbId, needQueryKbIds);
		queryWrapper.ne(KnowledgeBaseEntity::getStatus, CommonStatus.DELETED.getStatus());

		List<KnowledgeBaseEntity> entities = this.list(queryWrapper);
		for (KnowledgeBaseEntity entity : entities) {
			KnowledgeBase knowledgeBase = toKnowledgeBaseDTO(entity);
			knowledgeBases.add(knowledgeBase);
		}

		return knowledgeBases;
	}

	/**
	 * Retrieves a knowledge base by its name
	 * @param workspaceId Workspace ID
	 * @param name Knowledge base name
	 * @return Knowledge base entity if found, null otherwise
	 */
	private KnowledgeBaseEntity getKnowledgeBaseByName(String workspaceId, String name) {
		LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(KnowledgeBaseEntity::getWorkspaceId, workspaceId)
			.eq(KnowledgeBaseEntity::getName, name)
			.ne(KnowledgeBaseEntity::getStatus, CommonStatus.DELETED.getStatus())
			.last("limit 1");
		Optional<KnowledgeBaseEntity> entity = this.getOneOpt(queryWrapper);
		return entity.orElse(null);
	}

	/**
	 * Generates cache key for knowledge base
	 * @param workspaceId Workspace ID
	 * @param kbId Knowledge base ID
	 * @return Cache key string
	 */
	public static String getKnowledgeBaseCacheKey(String workspaceId, String kbId) {
		if (workspaceId == null) {
			workspaceId = "-";
		}

		return String.format(CACHE_KB_WORKSPACE_ID_PREFIX, workspaceId, kbId);
	}

	/**
	 * Retrieves a knowledge base by ID with caching
	 * @param workspaceId Workspace ID
	 * @param kbId Knowledge base ID
	 * @return Knowledge base entity if found, null otherwise
	 */
	private KnowledgeBaseEntity getKnowledgeBaseById(String workspaceId, String kbId) {
		String key = getKnowledgeBaseCacheKey(workspaceId, kbId);

		KnowledgeBaseEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(KnowledgeBaseEntity::getKbId, kbId)
			.eq(workspaceId != null, KnowledgeBaseEntity::getWorkspaceId, workspaceId)
			.ne(KnowledgeBaseEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<KnowledgeBaseEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new KnowledgeBaseEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		entity = entityOptional.get();
		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Converts knowledge base entity to DTO
	 * @param entity Knowledge base entity
	 * @return Knowledge base DTO
	 */
	private KnowledgeBase toKnowledgeBaseDTO(KnowledgeBaseEntity entity) {
		if (entity == null) {
			return null;
		}

		KnowledgeBase knowledgeBase = BeanCopierUtils.copy(entity, KnowledgeBase.class);
		String processConfig = entity.getProcessConfig();
		if (StringUtils.isNotBlank(processConfig)) {
			knowledgeBase.setProcessConfig(JsonUtils.fromJson(processConfig, ProcessConfig.class));
		}

		String indexConfig = entity.getIndexConfig();
		if (StringUtils.isNotBlank(indexConfig)) {
			knowledgeBase.setIndexConfig(JsonUtils.fromJson(indexConfig, IndexConfig.class));
		}

		String searchConfig = entity.getSearchConfig();
		if (StringUtils.isNotBlank(searchConfig)) {
			knowledgeBase.setSearchConfig(JsonUtils.fromJson(searchConfig, FileSearchOptions.class));
		}

		return knowledgeBase;
	}

}
