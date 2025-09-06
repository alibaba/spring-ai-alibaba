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

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.ReferTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AppQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentQuery;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.base.service.ReferService;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.AppEntity;
import com.alibaba.cloud.ai.studio.core.base.entity.AppVersionEntity;
import com.alibaba.cloud.ai.studio.core.base.entity.ReferEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.AppMapper;
import com.alibaba.cloud.ai.studio.core.base.mapper.AppVersionMapper;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.*;

/**
 * Implementation of the application service. Handles CRUD operations and version
 * management for applications.
 *
 * @since 1.0.0.3
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, AppEntity> implements AppService {

	/** Initial version number for applications */
	private static final String APP_INIT_VERSION = "1";

	/** Mapper for application version operations */
	private final AppVersionMapper appVersionMapper;

	/** Manager for Redis cache operations */
	private final RedisManager redisManager;

	/** Service for handling references */
	private final ReferService referService;

	public AppServiceImpl(AppVersionMapper appVersionMapper, RedisManager redisManager, ReferService referService) {
		this.appVersionMapper = appVersionMapper;
		this.redisManager = redisManager;
		this.referService = referService;
	}

	/**
	 * Creates a new application with initial version
	 * @param application Application details to create
	 * @return ID of the created application
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public String createApp(Application application) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();

			// check if app name exists
			AppEntity entity = getAppByName(context.getWorkspaceId(), application.getName());
			if (entity != null) {
				throw new BizException(ErrorCode.APP_NAME_EXISTS.toError());
			}

			String appId = IdGenerator.idStr();
			// insert application
			entity = BeanCopierUtils.copy(application, AppEntity.class);
			entity.setAppId(appId);
			entity.setStatus(AppStatus.DRAFT);
			entity.setWorkspaceId(context.getWorkspaceId());
			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			entity.setCreator(context.getAccountId());
			entity.setModifier(context.getAccountId());
			this.save(entity);

			// insert application version
			AppVersionEntity versionEntity = new AppVersionEntity();
			versionEntity.setAppId(appId);
			versionEntity.setWorkspaceId(context.getWorkspaceId());
			versionEntity.setStatus(AppStatus.DRAFT);
			versionEntity.setVersion(APP_INIT_VERSION);
			versionEntity.setGmtCreate(new Date());
			versionEntity.setGmtModified(new Date());
			versionEntity.setCreator(context.getAccountId());
			versionEntity.setModifier(context.getAccountId());

			String config = JsonUtils.toJson(application.getConfig());
			versionEntity.setConfig(config);
			appVersionMapper.insert(versionEntity);

			// update application version
			entity.setLatestVersion(versionEntity);

			String key = getApplicationCacheKey(entity.getWorkspaceId(), entity.getAppId());
			redisManager.put(key, entity);

			return appId;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.CREATE_APP_ERROR.toError(), e);
		}
	}

	/**
	 * Updates an existing application. Creates new version if application is published
	 * @param application Updated application details
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateApp(Application application) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();

			AppEntity entity = getAppById(context.getWorkspaceId(), application.getAppId());
			if (entity == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}

			// check if app name exists
			AppEntity appEntity = getAppByName(context.getWorkspaceId(), application.getName());
			if (appEntity != null && !appEntity.getId().equals(entity.getId())) {
				throw new BizException(ErrorCode.APP_NAME_EXISTS.toError());
			}

			// update app
			AppStatus status = entity.getStatus();

			// add new version only if app status is published
			if (entity.getStatus() == AppStatus.PUBLISHED) {
				status = AppStatus.PUBLISHED_EDITING;
				// copy and create new version
				AppVersionEntity versionEntity = entity.getPublishedVersion();
				AppVersionEntity newVersion = BeanCopierUtils.copy(versionEntity, AppVersionEntity.class);
				newVersion.setId(null);

				String newVersionId = String.valueOf(Integer.parseInt(versionEntity.getVersion()) + 1);
				newVersion.setVersion(newVersionId);
				newVersion.setStatus(AppStatus.DRAFT);
				newVersion.setGmtCreate(new Date());
				newVersion.setGmtModified(new Date());
				if (application.getConfig() != null) {
					String config = JsonUtils.toJson(application.getConfig());
					newVersion.setConfig(config);
				}
				appVersionMapper.insert(newVersion);

				entity.setLatestVersion(newVersion);
			}
			else {
				AppVersionEntity latestVersion = entity.getLatestVersion();
				if (application.getConfig() != null) {
					String config = JsonUtils.toJson(application.getConfig());
					latestVersion.setConfig(config);
				}

				latestVersion.setGmtModified(new Date());
				latestVersion.setModifier(context.getAccountId());
				appVersionMapper.updateById(latestVersion);

				entity.setLatestVersion(latestVersion);
			}

			// update app
			entity.setStatus(status);
			if (StringUtils.isNotBlank(application.getName())) {
				entity.setName(application.getName());
			}
			if (StringUtils.isNotBlank(application.getDescription())) {
				entity.setDescription(application.getDescription());
			}

			entity.setGmtModified(new Date());
			entity.setModifier(context.getAccountId());
			this.updateById(entity);

			String key = getApplicationCacheKey(entity.getWorkspaceId(), entity.getAppId());
			redisManager.put(key, entity);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.UPDATE_AGENT_ERROR.toError(), e);
		}
	}

	/**
	 * Deletes an application and its versions
	 * @param appId ID of the application to delete
	 */
	@Override
	public void deleteApp(String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// delete from db
		AppEntity entity = getAppById(context.getWorkspaceId(), appId);
		if (entity == null) {
			return;
		}

		// delete all versions first
		LambdaUpdateWrapper<AppVersionEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(AppVersionEntity::getAppId, appId)
			.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId())
			.set(AppVersionEntity::getStatus, CommonStatus.DELETED.getStatus())
			.set(AppVersionEntity::getGmtModified, new Date())
			.set(AppVersionEntity::getModifier, context.getAccountId());
		appVersionMapper.update(updateWrapper);

		// delete app
		entity.setStatus(AppStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);

		// delete from cache
		String cacheKey = getApplicationCacheKey(context.getWorkspaceId(), entity.getAppId());
		redisManager.delete(cacheKey);
	}

	/**
	 * Retrieves application details by ID
	 * @param appId ID of the application
	 * @return Application details
	 */
	@Override
	public Application getApp(String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		AppEntity entity = getAppById(context.getWorkspaceId(), appId);
		if (entity == null) {
			throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
		}

		return toApplicationDTO(entity);
	}

	/**
	 * Lists applications based on query criteria
	 * @param query Search criteria
	 * @return Paginated list of applications
	 */
	@Override
	public PagingList<Application> listApps(AppQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		Page<AppEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<AppEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppEntity::getWorkspaceId, context.getWorkspaceId());
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(AppEntity::getName, query.getName());
		}
		if (StringUtils.isNotBlank(query.getType())) {
			queryWrapper.eq(AppEntity::getType, query.getType());
		}
		if (query.getStatus() == null || query.getStatus() == AppStatus.DELETED) {
			queryWrapper.ne(AppEntity::getStatus, CommonStatus.DELETED.getStatus());
		}
		else {
			queryWrapper.eq(AppEntity::getStatus, query.getStatus().getStatus());
		}
		queryWrapper.orderByDesc(AppEntity::getId);

		IPage<AppEntity> pageResult = this.page(page, queryWrapper);

		List<Application> apps;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			apps = new ArrayList<>();
		}
		else {
			apps = pageResult.getRecords().stream().map(this::toApplicationDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), apps);
	}

	/**
	 * Publishes an application version. Validates configuration and updates status
	 * @param appId ID of the application to publish
	 */
	@Override
	public void publishApp(String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		AppEntity entity = getAppById(context.getWorkspaceId(), appId);
		if (entity == null) {
			throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
		}

		LambdaQueryWrapper<AppVersionEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppVersionEntity::getAppId, appId)
			.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId())
			.ne(AppVersionEntity::getStatus, AppStatus.DELETED.getStatus())
			.orderByDesc(AppVersionEntity::getId)
			.last("limit 1");
		AppVersionEntity versionEntity = appVersionMapper.selectOne(queryWrapper);
		if (versionEntity == null) {
			throw new BizException(ErrorCode.APP_VERSION_NOT_FOUND.toError());
		}

		if (entity.getType() == AppType.BASIC) {
			AgentConfig config = JsonUtils.fromJson(versionEntity.getConfig(), AgentConfig.class);
			if (StringUtils.isBlank(config.getModelProvider())) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("model_provider"));
			}

			if (StringUtils.isBlank(config.getModel())) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("model"));
			}
		}

		versionEntity.setStatus(AppStatus.PUBLISHED);
		versionEntity.setGmtModified(new Date());
		versionEntity.setModifier(context.getAccountId());
		appVersionMapper.updateById(versionEntity);

		entity.setStatus(AppStatus.PUBLISHED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);

		entity.setPublishedVersion(versionEntity);
		entity.setLatestVersion(versionEntity);

		// update cache
		String key = getApplicationCacheKey(context.getWorkspaceId(), appId);
		redisManager.put(key, entity);
		Application app = getApp(appId);

		Integer referType = app.getType() == AppType.BASIC ? ReferTypeEnum.MAIN_TYPE_AGENT.getType()
				: ReferTypeEnum.MAIN_TYPE_FLOW.getType();
		referService.deleteReferList(appId, referType);
		List<ReferEntity> refers = referService.constructRefers(app);
		referService.saveReferList(refers);
	}

	/**
	 * Lists versions of an application
	 * @param query Search criteria
	 * @return Paginated list of application versions
	 */
	@Override
	public PagingList<ApplicationVersion> listAppVersions(AppQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		Page<AppVersionEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<AppVersionEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.eq(AppVersionEntity::getAppId, query.getAppId());
		if (query.getStatus() == null || query.getStatus() == AppStatus.DELETED) {
			queryWrapper.ne(AppVersionEntity::getStatus, CommonStatus.DELETED.getStatus());
		}
		else {
			queryWrapper.eq(AppVersionEntity::getStatus, query.getStatus().getStatus());
		}
		queryWrapper.orderByDesc(AppVersionEntity::getId);

		Page<AppVersionEntity> pageResult = appVersionMapper.selectPage(page, queryWrapper);

		List<ApplicationVersion> appVersions;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			appVersions = new ArrayList<>();
		}
		else {
			appVersions = pageResult.getRecords().stream().map(this::toApplicationVersionDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), appVersions);
	}

	/**
	 * Gets specific version of an application. Supports 'latest' and 'lastPublished'
	 * special version IDs
	 * @param appId Application ID
	 * @param versionId Version ID or special identifier
	 * @return Application version details
	 */
	@Override
	public ApplicationVersion getAppVersion(String appId, String versionId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		// If versionId is 'latest', query the most recent version configuration
		if ("latest".equals(versionId)) {
			LambdaQueryWrapper<AppVersionEntity> latestQueryWrapper = new LambdaQueryWrapper<>();
			latestQueryWrapper.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId())
				.eq(AppVersionEntity::getAppId, appId)
				.ne(AppVersionEntity::getStatus, AppStatus.DELETED.getStatus())
				.orderByDesc(AppVersionEntity::getId)
				.last("limit 1");
			AppVersionEntity latestVersion = appVersionMapper.selectOne(latestQueryWrapper);
			return toApplicationVersionDTO(latestVersion);
		}

		// If versionId is 'lastPublished', query the most recently published version
		// configuration
		if ("lastPublished".equals(versionId)) {
			LambdaQueryWrapper<AppVersionEntity> publishedQueryWrapper = new LambdaQueryWrapper<>();
			publishedQueryWrapper.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId())
				.eq(AppVersionEntity::getAppId, appId)
				.eq(AppVersionEntity::getStatus, AppStatus.PUBLISHED)
				.orderByDesc(AppVersionEntity::getId)
				.last("limit 1");
			AppVersionEntity publishedVersion = appVersionMapper.selectOne(publishedQueryWrapper);
			return toApplicationVersionDTO(publishedVersion);
		}

		// Query by specific version ID
		LambdaQueryWrapper<AppVersionEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppVersionEntity::getWorkspaceId, context.getWorkspaceId())
			.eq(AppVersionEntity::getAppId, appId)
			.eq(AppVersionEntity::getVersion, versionId)
			.ne(AppVersionEntity::getStatus, AppStatus.DELETED.getStatus());
		AppVersionEntity appVersionEntity = appVersionMapper.selectOne(queryWrapper);
		return toApplicationVersionDTO(appVersionEntity);
	}

	/**
	 * Gets list of published application IDs
	 * @param type Application type filter
	 * @param appName Application name filter
	 * @param codes List of application codes to exclude
	 * @return List of application IDs
	 */
	@Override
	public List<Long> getApplicationPublished(String type, String appName, List<String> codes) {
		RequestContext context = RequestContextHolder.getRequestContext();

		LambdaQueryWrapper<AppEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppEntity::getWorkspaceId, context.getWorkspaceId());
		if (!CollectionUtils.isEmpty(codes)) {
			queryWrapper.notIn(AppEntity::getAppId, codes);
		}
		if (type != null) {
			queryWrapper.eq(AppEntity::getType, type);
		}
		ArrayList<Integer> statusList = new ArrayList<>();
		statusList.add(AppStatus.PUBLISHED.getStatus());
		statusList.add(AppStatus.PUBLISHED_EDITING.getStatus());
		queryWrapper.in(AppEntity::getStatus, statusList);

		if (!StringUtils.isBlank(appName)) {
			queryWrapper.like(AppEntity::getName, appName);
		}
		List<AppEntity> applicationEntities = this.list(queryWrapper);

		HashMap<String, Long> map = new HashMap<>();
		for (AppEntity entity : applicationEntities) {
			if (map.containsKey(entity.getAppId())) {
				if (entity.getId() > map.get(entity.getAppId())) {
					map.put(entity.getAppId(), entity.getId());
				}
			}
			else {
				map.put(entity.getAppId(), entity.getId());
			}
		}
		List<Long> ids = new ArrayList<>();
		for (String code : map.keySet()) {
			ids.add(map.get(code));
		}

		return ids;
	}

	/**
	 * Gets list of published applications that are not components
	 * @param request Query parameters
	 * @param codes List of application codes to exclude
	 * @param ids List of application IDs to include
	 * @return Paginated list of applications
	 */
	@Override
	public PagingList<Application> getApplicationPublishedAndNotComponentList(AppComponentQuery request,
			List<String> codes, List<Long> ids) {
		RequestContext context = RequestContextHolder.getRequestContext();

		Page<AppEntity> page = new Page<>(request.getCurrent(), request.getSize());
		LambdaQueryWrapper<AppEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppEntity::getWorkspaceId, context.getWorkspaceId());
		if (!CollectionUtils.isEmpty(codes)) {
			queryWrapper.notIn(AppEntity::getAppId, codes);
		}
		if (request.getType() != null) {
			queryWrapper.eq(AppEntity::getType, request.getType());
		}
		ArrayList<Integer> statusList = new ArrayList<>();
		statusList.add(AppStatus.PUBLISHED.getStatus());
		statusList.add(AppStatus.PUBLISHED_EDITING.getStatus());
		queryWrapper.in(AppEntity::getStatus, statusList);
		if (!StringUtils.isBlank(request.getAppName())) {
			queryWrapper.like(AppEntity::getName, request.getAppName());
		}
		if (!CollectionUtils.isEmpty(ids)) {
			queryWrapper.in(AppEntity::getId, ids);
		}
		queryWrapper.orderByDesc(AppEntity::getId);

		IPage<AppEntity> pageResult = this.page(page, queryWrapper);

		List<AppEntity> applicationEntities = pageResult.getRecords();
		List<Application> applicationDTOS = new ArrayList<>();
		applicationEntities.forEach(applicationEntity -> applicationDTOS.add(toApplicationDTO(applicationEntity)));
		return new PagingList<>(request.getCurrent(), request.getSize(), pageResult.getTotal(), applicationDTOS);
	}

	/**
	 * Creates a copy of an existing application
	 * @param appId ID of the application to copy
	 * @return ID of the new application
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public String copyApp(String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		// get current app info
		AppEntity sourceEntity = getAppById(context.getWorkspaceId(), appId);
		if (sourceEntity == null) {
			throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
		}

		// gen new app id
		String newAppId = IdGenerator.idStr();

		// copy app info
		AppEntity newEntity = BeanCopierUtils.copy(sourceEntity, AppEntity.class);
		newEntity.setId(null);
		newEntity.setAppId(newAppId);
		newEntity.setName(sourceEntity.getName() + "_copy_"
				+ LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
		newEntity.setStatus(AppStatus.DRAFT);
		newEntity.setGmtCreate(new Date());
		newEntity.setGmtModified(new Date());
		newEntity.setCreator(context.getAccountId());
		newEntity.setModifier(context.getAccountId());
		this.save(newEntity);

		// copy version info
		AppVersionEntity sourceVersion = sourceEntity.getLatestVersion();
		if (sourceVersion != null) {
			AppVersionEntity newVersion = BeanCopierUtils.copy(sourceVersion, AppVersionEntity.class);
			newVersion.setId(null);
			newVersion.setAppId(newAppId);
			newVersion.setStatus(AppStatus.DRAFT);
			newVersion.setVersion("1");
			newVersion.setGmtCreate(new Date());
			newVersion.setGmtModified(new Date());
			newVersion.setCreator(context.getAccountId());
			newVersion.setModifier(context.getAccountId());
			appVersionMapper.insert(newVersion);

			newEntity.setLatestVersion(newVersion);
		}

		// update cache
		String key = getApplicationCacheKey(context.getWorkspaceId(), newAppId);
		redisManager.put(key, newEntity);

		return newAppId;
	}

	/**
	 * Generates cache key for application
	 * @param workspaceId Workspace ID
	 * @param appId Application ID
	 * @return Cache key string
	 */
	public static String getApplicationCacheKey(String workspaceId, String appId) {
		return String.format(CACHE_APP_WORKSPACE_ID_PREFIX, workspaceId, appId);
	}

	/**
	 * Retrieves application entity by ID with caching
	 * @param workspaceId Workspace ID
	 * @param appId Application ID
	 * @return Application entity or null if not found
	 */
	private AppEntity getAppById(String workspaceId, String appId) {
		String key = getApplicationCacheKey(workspaceId, appId);
		AppEntity entity = redisManager.get(key);
		if (entity != null) {
			if (CACHE_EMPTY_ID.equals(entity.getId())) {
				return null;
			}

			return entity;
		}

		// get app info
		LambdaQueryWrapper<AppEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppEntity::getAppId, appId)
			.eq(AppEntity::getWorkspaceId, workspaceId)
			.ne(AppEntity::getStatus, AppStatus.DELETED.getStatus());
		Optional<AppEntity> entityOptional = this.getOneOpt(queryWrapper);
		if (entityOptional.isEmpty()) {
			entity = new AppEntity();
			entity.setId(CACHE_EMPTY_ID);
			redisManager.put(key, entity, CacheConstants.CACHE_EMPTY_TTL);
			return null;
		}

		// get latest version and latest published version
		entity = entityOptional.get();
		LambdaQueryWrapper<AppVersionEntity> versionQueryWrapper = new LambdaQueryWrapper<>();
		versionQueryWrapper.eq(AppVersionEntity::getWorkspaceId, workspaceId)
			.eq(AppVersionEntity::getAppId, appId)
			.ne(AppVersionEntity::getStatus, CommonStatus.DELETED.getStatus())
			.orderByDesc(AppVersionEntity::getId);
		List<AppVersionEntity> versionEntities = appVersionMapper.selectList(versionQueryWrapper);
		if (!CollectionUtils.isEmpty(versionEntities)) {
			entity.setLatestVersion(versionEntities.get(0));
			for (AppVersionEntity versionEntity : versionEntities) {
				if (versionEntity.getStatus() == AppStatus.PUBLISHED) {
					entity.setPublishedVersion(versionEntity);
					break;
				}
			}
		}

		redisManager.put(key, entity);
		return entity;
	}

	/**
	 * Retrieves application entity by name
	 * @param workspaceId Workspace ID
	 * @param appName Application name
	 * @return Application entity or null if not found
	 */
	private AppEntity getAppByName(String workspaceId, String appName) {
		LambdaQueryWrapper<AppEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppEntity::getWorkspaceId, workspaceId)
			.eq(AppEntity::getName, appName)
			.ne(AppEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<AppEntity> entityOptional = this.getOneOpt(queryWrapper);
		return entityOptional.orElse(null);
	}

	/**
	 * Converts version entity to DTO
	 * @param entity Version entity
	 * @return Version DTO
	 */
	private ApplicationVersion toApplicationVersionDTO(AppVersionEntity entity) {
		if (entity == null) {
			return null;
		}
		return BeanCopierUtils.copy(entity, ApplicationVersion.class);
	}

	/**
	 * Converts application entity to DTO
	 * @param entity Application entity
	 * @return Application DTO
	 */
	private Application toApplicationDTO(AppEntity entity) {
		if (entity == null) {
			return null;
		}

		Application application = BeanCopierUtils.copy(entity, Application.class);
		AppVersionEntity latestVersion = entity.getLatestVersion();
		if (latestVersion != null && StringUtils.isNotBlank(latestVersion.getConfig())) {
			application.setConfigStr(latestVersion.getConfig());
			application.setConfig(JsonUtils.fromJsonToMap(latestVersion.getConfig()));
		}

		AppVersionEntity publishedVersion = entity.getPublishedVersion();
		if (publishedVersion != null && StringUtils.isNotBlank(publishedVersion.getConfig())) {
			application.setPubConfigStr(publishedVersion.getConfig());
			application.setPubConfig(JsonUtils.fromJsonToMap(publishedVersion.getConfig()));
		}

		return application;
	}

}
