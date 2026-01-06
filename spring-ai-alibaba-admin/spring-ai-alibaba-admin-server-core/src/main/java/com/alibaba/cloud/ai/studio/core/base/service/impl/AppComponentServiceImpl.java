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

import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentUpdateEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponent;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentQuery;
import com.alibaba.cloud.ai.studio.core.base.service.AppComponentService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.AppComponentEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.AppComponentMapper;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.APP_COMPONENT_ALREADY_EXISI_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.APP_COMPONENT_DELETE_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.APP_COMPONENT_NOT_FOUND_ERROR;
import static com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.APP_COMPONENT_UPDATE_ERROR;

/**
 * Service implementation for managing application components. Handles CRUD operations and
 * component lifecycle management.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Slf4j
@Service
public class AppComponentServiceImpl extends ServiceImpl<AppComponentMapper, AppComponentEntity>
		implements AppComponentService {

	/**
	 * Retrieves a paginated list of app components based on query parameters.
	 * @param request Query request object
	 * @return Paginated list of app components
	 */
	@Override
	public PagingList<AppComponent> getAppComponentList(AppComponentQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		Page<AppComponentEntity> page = new Page<>(request.getCurrent(), request.getSize());
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		if (request.getType() != null) {
			queryWrapper.eq(AppComponentEntity::getType, request.getType());
		}
		queryWrapper.eq(AppComponentEntity::getStatus, AppComponentStatusEnum.Published.getCode());
		if (!StringUtils.isBlank(request.getName())) {
			queryWrapper.like(AppComponentEntity::getName, request.getName());
		}
		if (request.getAppId() != null) {
			queryWrapper.ne(AppComponentEntity::getAppId, request.getAppId());
		}
		queryWrapper.orderByDesc(AppComponentEntity::getId);
		IPage<AppComponentEntity> pageResult = this.page(page, queryWrapper);
		List<AppComponent> appComponentDTOS = toAppComponentDTO(pageResult.getRecords());
		return new PagingList<>(request.getCurrent(), request.getSize(), pageResult.getTotal(), appComponentDTOS);
	}

	/**
	 * Retrieves all app components matching the specified type and status.
	 * @param type Component type filter
	 * @param status Component status filter
	 * @return List of app components
	 */
	@Override
	public List<AppComponent> getAppComponentListAll(String type, Integer status) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		if (type != null) {
			queryWrapper.eq(AppComponentEntity::getType, type);
		}
		if (status != null) {
			queryWrapper.eq(AppComponentEntity::getStatus, status);
		}
		List<AppComponentEntity> applicationComponentEntities = this.list(queryWrapper);
		return toAppComponentDTO(applicationComponentEntities);
	}

	/**
	 * Retrieves app components by their codes.
	 * @param codes List of component codes
	 * @return List of matched app components
	 */
	@Override
	public List<AppComponent> getAppComponentListByCodes(List<String> codes) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		if (!CollectionUtils.isEmpty(codes)) {
			queryWrapper.in(AppComponentEntity::getCode, codes);
		}
		queryWrapper.eq(AppComponentEntity::getStatus, AppComponentStatusEnum.Published.getCode());
		List<AppComponentEntity> applicationComponentEntities = this.list(queryWrapper);
		return toAppComponentDTO(applicationComponentEntities);
	}

	/**
	 * Retrieves an app component by application ID and status.
	 * @param appId Application code
	 * @param status Component status
	 * @return App component object
	 */
	@Override
	public AppComponent getAppComponentByAppId(String appId, Integer status) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.eq(AppComponentEntity::getAppId, appId);
		if (status != null) {
			queryWrapper.eq(AppComponentEntity::getStatus, status);
		}
		AppComponentEntity applicationComponentEntity = this.getOne(queryWrapper);
		return toAppComponentDTO(applicationComponentEntity);
	}

	/**
	 * Retrieves an app component by its code and status.
	 * @param code Internal component code
	 * @param status Component status
	 * @return App component object
	 */
	@Override
	public AppComponent getAppComponentByCode(String code, Integer status) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.eq(AppComponentEntity::getCode, code);
		if (status != null) {
			queryWrapper.eq(AppComponentEntity::getStatus, status);
		}
		AppComponentEntity applicationComponentEntity = this.getOne(queryWrapper);
		return toAppComponentDTO(applicationComponentEntity);
	}

	/**
	 * Creates a new app component.
	 * @param component App component to create
	 * @return Result indicating success or failure
	 */
	@Override
	public Result<String> createAppComponent(AppComponent component) {
		AppComponent appComponentByAppId = getAppComponentByAppId(component.getAppId(),
				AppComponentStatusEnum.Published.getCode());
		if (appComponentByAppId != null) {
			return Result.error(APP_COMPONENT_ALREADY_EXISI_ERROR);
		}
		String code = initAppComponent(component);
		if (StringUtils.isBlank(code)) {
			return Result.error(ErrorCode.APP_COMPONENT_PUBLISH_ERROR);
		}
		else {
			return Result.success(code);
		}
	}

	/**
	 * Updates an existing app component.
	 * @param component Updated app component data
	 * @return Result indicating success or failure
	 */
	@Override
	public Result<Integer> updateAppComponent(AppComponent component) {
		int uptade = 0;
		// check whether the component exist
		AppComponentEntity updateEntity = getByCode(component.getCode(), component.getStatus());
		if (updateEntity == null) {
			return Result.error(APP_COMPONENT_NOT_FOUND_ERROR);
		}
		if (!StringUtils.isBlank(component.getName())) {
			updateEntity.setName(component.getName());
		}
		if (!StringUtils.isBlank(component.getConfig())) {
			updateEntity.setConfig(component.getConfig());
		}
		if (component.getDescription() != null) {
			updateEntity.setDescription(component.getDescription());
		}
		updateEntity.setGmtModified(new Date());
		try {
			this.updateById(updateEntity);
		}
		catch (Exception e) {
			uptade = -1;
			log.error("AppComponent update fail ,request:{}", e);
		}
		if (uptade == -1) {
			return Result.error(APP_COMPONENT_UPDATE_ERROR);
		}
		return Result.success(uptade);
	}

	/**
	 * Marks an app component as deleted.
	 * @param code component code
	 * @return Result indicating success or failure
	 */
	@Override
	public Result<Void> deleteAppComponent(String code) {

		// check whether the component exist
		AppComponentEntity updateEntity = getByCode(code, AppComponentStatusEnum.Published.getCode());
		if (updateEntity == null) {
			return Result.error(APP_COMPONENT_NOT_FOUND_ERROR);
		}
		updateEntity.setStatus(AppComponentStatusEnum.Delete.getCode());
		boolean success = true;
		try {
			this.updateById(updateEntity);
		}
		catch (Exception e) {
			success = false;
			log.error("AppComponent delete fail ,request:{}", e);
		}
		if (!success) {
			return Result.error(APP_COMPONENT_DELETE_ERROR);
		}
		return Result.success(null);
	}

	/**
	 * Retrieves an app component entity by code and status.
	 * @param code Internal component code
	 * @param status Component status
	 * @return App component entity
	 */
	public AppComponentEntity getByCode(String code, Integer status) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<AppComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(AppComponentEntity::getWorkspaceId, context.getWorkspaceId());
		queryWrapper.eq(AppComponentEntity::getCode, code);
		if (status != null) {
			queryWrapper.eq(AppComponentEntity::getStatus, status);
		}
		return this.getOne(queryWrapper);
	}

	/**
	 * Converts a list of entity objects to DTOs.
	 * @param applicationComponentEntities List of entity objects
	 * @return List of DTO objects
	 */
	private List<AppComponent> toAppComponentDTO(List<AppComponentEntity> applicationComponentEntities) {
		List<AppComponent> appComponentDTOS = new ArrayList<>();
		if (applicationComponentEntities != null) {
			for (AppComponentEntity entity : applicationComponentEntities) {
				AppComponent appComponentDTO = toAppComponentDTO(entity);
				appComponentDTOS.add(appComponentDTO);
			}
		}

		return appComponentDTOS;
	}

	/**
	 * Converts a single entity object to DTO.
	 * @param entity Entity object
	 * @return DTO object
	 */
	private AppComponent toAppComponentDTO(AppComponentEntity entity) {
		if (entity == null) {
			return null;
		}
		AppComponent appComponent = new AppComponent();
		appComponent.setCode(entity.getCode());
		appComponent.setName(entity.getName());
		appComponent.setAppId(entity.getAppId());
		appComponent.setConfig(entity.getConfig());
		appComponent.setDescription(entity.getDescription());
		appComponent.setStatus(entity.getStatus());
		appComponent.setNeedUpdate(entity.getNeedUpdate());
		appComponent.setType(entity.getType());
		appComponent.setGmtModified(entity.getGmtModified());
		appComponent.setGmtCreate(entity.getGmtCreate());
		return appComponent;
	}

	/**
	 * Initializes and persists a new app component.
	 * @param component App component to initialize
	 * @return Generated component code
	 */
	public String initAppComponent(AppComponent component) {
		RequestContext context = RequestContextHolder.getRequestContext();

		String code = IdGenerator.idStr();
		AppComponentEntity insertEntity = new AppComponentEntity();
		insertEntity.setGmtCreate(new Date());
		insertEntity.setGmtModified(new Date());
		insertEntity.setAppId(component.getAppId());
		insertEntity.setCode(code);
		insertEntity.setName(component.getName());
		insertEntity.setType(component.getType());
		insertEntity.setWorkspaceId(context.getWorkspaceId());
		insertEntity.setCreator(context.getAccountId());
		insertEntity.setModifier(context.getAccountId());
		insertEntity.setConfig(component.getConfig());
		insertEntity.setDescription(component.getDescription());
		insertEntity.setNeedUpdate(AppComponentUpdateEnum.Unnecessary.getCode());
		insertEntity.setStatus(AppComponentStatusEnum.Published.getCode());

		this.save(insertEntity);

		return code;

	}

}
