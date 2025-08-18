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

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponent;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.refer.Refer;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AppComponentService;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.base.service.ReferService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.EndExecuteProcessor;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller for managing application components in the SAA Studio platform. This
 * controller provides endpoints for: 1. CRUD operations on application components 2.
 * Component configuration management 3. Component reference relationship queries 4.
 * Component schema queries 5. Publishable application queries
 * <p>
 * The component system allows applications to be published as reusable components that
 * can be referenced by other applications, enabling modular application development.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "app_component")
@RequestMapping("/console/v1/component-servers")
public class AppComponentController {

	private final AppComponentService appComponentService;

	private final AppService appService;

	private final AppComponentManager appComponentManager;

	private final ReferService referService;

	public AppComponentController(AppComponentService appComponentService, AppService appService,
			AppComponentManager appComponentManager, ReferService referService) {
		this.appComponentService = appComponentService;
		this.appService = appService;
		this.appComponentManager = appComponentManager;
		this.referService = referService;
	}

	/**
	 * Retrieves a paginated list of application components based on query parameters.
	 * @param request Query parameters for filtering components including: - type:
	 * Component type - name: Component name - appId: Associated application ID - status:
	 * Component status - pageSize: Number of items per page - pageNum: Page number
	 * @return Result containing a PagingList of AppComponent objects
	 */
	@GetMapping()
	public Result<PagingList<AppComponent>> getAppComponentPageList(@ApiModelAttribute AppComponentQuery request) {

		RequestContext context = RequestContextHolder.getRequestContext();
		try {
			PagingList<AppComponent> appComponentList = appComponentService.getAppComponentList(request);
			return Result.success(context.getRequestId(), appComponentList);
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.APP_COMPONENT_LIST_ERROR.toError());
		}
	}

	/**
	 * Retrieves a paginated list of applications that can be published as components.
	 * This endpoint filters out applications that are already published as components.
	 * @param request Query parameters including: - type: Application type - appName:
	 * Application name
	 * @return Result containing a PagingList of Application objects that can be published
	 * as components
	 */
	@GetMapping("/app-publishable")
	public Result<PagingList<Application>> getAppPublishablePageList(@ApiModelAttribute AppComponentQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(request.getType())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("type"));
		}

		// component published
		List<AppComponent> appComponentList = appComponentService.getAppComponentListAll(request.getType(),
				AppComponentStatusEnum.Published.getCode());
		List<String> codes = new ArrayList<>();
		if (appComponentList != null && !appComponentList.isEmpty()) {
			codes = appComponentList.stream().map(AppComponent::getAppId).collect(Collectors.toList());
		}
		// application can released to component
		List<Long> ids = appService.getApplicationPublished(request.getType(), request.getAppName(), codes);

		PagingList<Application> componentList = appService.getApplicationPublishedAndNotComponentList(request, codes,
				ids);
		return Result.success(context.getRequestId(), componentList);
	}

	/**
	 * Publishes a new application component. Creates a new component from an existing
	 * application with specified configuration.
	 * @param request Component details including: - type: Component type - name:
	 * Component name - config: Component configuration - appId: Source application ID -
	 * description: Component description
	 * @return Result containing the unique code of the published component
	 */
	@PostMapping()
	public Result<String> publishComponent(@RequestBody AppComponentQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(request.getType())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("type"));
		}
		if (StringUtils.isBlank(request.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}
		if (StringUtils.isBlank(request.getConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("config"));
		}
		if (StringUtils.isBlank(request.getAppId())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}
		if (StringUtils.isBlank(request.getDescription())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("description"));
		}

		// create component
		AppComponent component = initComponent(request);
		Result<String> appComponent = appComponentService.createAppComponent(component);
		if (appComponent.isSuccess()) {
			return Result.success(context.getRequestId(), appComponent.getData());
		}
		else {
			throw new BizException(ErrorCode.APP_COMPONENT_PUBLISH_ERROR.toError());
		}
	}

	/**
	 * Updates an existing application component. Modifies the configuration and details
	 * of a published component.
	 * @param code Unique code of the component to update
	 * @param request Updated component details including: - type: Component type - name:
	 * Component name - config: Updated configuration - appId: Associated application ID -
	 * description: Updated description
	 * @return Result indicating successful update
	 */
	@PutMapping("/{code}")
	public Result<String> updateComponent(@PathVariable("code") String code, @RequestBody AppComponentQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		request.setCode(code);
		if (Objects.isNull(request.getType())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("type"));
		}
		if (StringUtils.isBlank(request.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}
		if (StringUtils.isBlank(request.getConfig())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("config"));
		}
		if (StringUtils.isBlank(request.getAppId())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}
		if (StringUtils.isBlank(request.getDescription())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("description"));
		}
		if (StringUtils.isBlank(request.getCode())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("code"));
		}

		// update component config
		AppComponent component = initComponent(request);
		Result<Integer> integerResult = appComponentService.updateAppComponent(component);
		if (integerResult.isSuccess()) {
			return Result.success(context.getRequestId(), "update success");
		}
		else {
			throw new BizException(ErrorCode.APP_COMPONENT_UPDATE_ERROR.toError());
		}

	}

	/**
	 * Deletes an application component. Removes a component from the system by its unique
	 * code.
	 * @param code Unique code of the component to delete
	 * @return Result containing boolean indicating successful deletion
	 */
	@DeleteMapping("/{code}")
	public Result<Boolean> deleteComponent(@PathVariable("code") String code) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(code)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("code"));
		}
		Result<Void> voidResult = appComponentService.deleteAppComponent(code);
		if (voidResult.isSuccess()) {
			return Result.success(context.getRequestId(), true);
		}
		else {
			throw new BizException(ErrorCode.APP_COMPONENT_DELETE_ERROR.toError());
		}
	}

	/**
	 * Retrieves detailed information about a component by its unique code. Includes
	 * merged configuration from both component and source application.
	 * @param code Unique code of the component
	 * @return Result containing detailed AppComponent information with merged
	 * configuration
	 */
	@GetMapping("/{code}/detail-by-code")
	public Result<AppComponent> detailByCode(@PathVariable("code") String code) {

		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(code)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("code"));
		}
		try {
			AppComponent appComponentByCode = appComponentService.getAppComponentByCode(code,
					AppComponentStatusEnum.Published.getCode());
			if (appComponentByCode == null) {
				return Result.success(context.getRequestId(), null);
			}
			// get component inputScheme
			AppComponentConfig appComponentConfig = appComponentManager.getAppComponentInputConfig(appComponentByCode);
			String appId = appComponentByCode.getAppId();
			Application application = appService.getApp(appId);
			if (application == null) {
				return Result.error(context.getRequestId(), ErrorCode.APP_NOT_FOUND);
			}
			// get application inputScheme
			AppComponentConfig applicationInputConfig = appComponentManager.getApplicationInputConfig(application,
					false);
			// merge component config
			applicationInputConfig = appComponentManager.mergeConfig(applicationInputConfig, appComponentConfig);
			appComponentByCode.setConfig(JsonUtils.toJson(applicationInputConfig));
			return Result.success(context.getRequestId(), appComponentByCode);
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.APP_COMPONENT_DETAIL_ERROR.toError());
		}

	}

	/**
	 * Retrieves detailed information about a component by its application ID. Includes
	 * merged configuration from both component and source application.
	 * @param appId Application ID associated with the component
	 * @return Result containing detailed AppComponent information with merged
	 * configuration
	 */
	@GetMapping("/{appId}/detail-by-appid")
	public Result<AppComponent> detailByAppId(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}

		try {
			AppComponent appComponentByAppId = appComponentService.getAppComponentByAppId(appId,
					AppComponentStatusEnum.Published.getCode());
			if (appComponentByAppId == null) {
				return Result.success(context.getRequestId(), null);
			}
			// get component inputScheme
			AppComponentConfig appComponentConfig = appComponentManager.getAppComponentInputConfig(appComponentByAppId);

			Application application = appService.getApp(appId);
			if (application == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}
			// get application inputScheme
			AppComponentConfig applicationInputConfig = appComponentManager.getApplicationInputConfig(application,
					false);
			// merge component config
			applicationInputConfig = appComponentManager.mergeConfig(applicationInputConfig, appComponentConfig);
			appComponentByAppId.setConfig(JsonUtils.toJson(applicationInputConfig));
			return Result.success(appComponentByAppId);

		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_DETAIL_ERROR.toError());
		}

	}

	/**
	 * Queries components that reference a specific component.
	 * @param code Unique code of the component to find references for
	 * @return Result containing a list of components that reference the specified
	 * component
	 */
	@GetMapping("/{code}/query-refer")
	public Result<List<AppComponent>> queryReferInfo(@PathVariable("code") String code) {

		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(code)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("code"));
		}
		try {
			List<AppComponent> appComponentDTOList = new ArrayList<>();
			List<Refer> refers = referService.getReferListByReferCode(code);
			if (!CollectionUtils.isEmpty(refers)) {
				for (Refer refer : refers) {
					Application application = appService.getApp(refer.getMainCode());
					if (application == null || application.getStatus() == AppStatus.DELETED) {
						continue;
					}
					AppComponent appComponent = new AppComponent();
					appComponent.setAppId(application.getAppId());
					appComponent.setName(application.getName());
					AppType type = application.getType();
					appComponent.setType(type.getValue());
					appComponentDTOList.add(appComponent);
				}
			}
			return Result.success(context.getRequestId(), appComponentDTOList);
		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_REFER_ERROR.toError());
		}

	}

	/**
	 * Queries the configuration of a component by its application ID.
	 * @param appId Application ID associated with the component
	 * @return Result containing the component's configuration details
	 */
	@GetMapping("/{appId}/query-config")
	public Result<AppComponent> queryConfig(@PathVariable("appId") String appId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
		}
		try {
			Application application = appService.getApp(appId);
			if (application == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}
			AppComponentConfig applicationInputConfig = appComponentManager.getApplicationInputConfig(application,
					true);
			if (applicationInputConfig != null) {
				AppComponent appComponentDTO = new AppComponent();
				appComponentDTO.setConfig(JsonUtils.toJson(applicationInputConfig));
				appComponentDTO.setAppName(application.getName());
				appComponentDTO.setAppId(application.getAppId());
				return Result.success(context.getRequestId(), appComponentDTO);
			}
			else {
				throw new BizException(ErrorCode.APP_COMPONENT_QUERYCONFIG_ERROR.toError());
			}
		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_QUERYCONFIG_ERROR.toError());
		}

	}

	/**
	 * Retrieves a list of components by their unique codes.
	 * @param request Query containing a list of component codes to retrieve
	 * @return Result containing a list of matching AppComponent objects
	 */
	@PostMapping("/query-by-codes")
	public Result<List<AppComponent>> listByCodes(@RequestBody AppComponentQuery request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (CollectionUtils.isEmpty(request.getCodes())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("codes"));
		}
		try {
			List<AppComponent> appComponentListByCodes = appComponentService
				.getAppComponentListByCodes(request.getCodes());
			return Result.success(context.getRequestId(), appComponentListByCodes);

		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_QUERYCONFIG_ERROR.toError());
		}
	}

	/**
	 * Retrieves the schema of a component by its unique code. The schema includes
	 * input/output parameters and output type information.
	 * @param code Unique code of the component
	 * @return Result containing a map of schema information including: - input: Input
	 * parameter definitions - output: Output parameter definitions - output_type: Type of
	 * output (text/json)
	 */
	@GetMapping("/{code}/query-schema")
	public Result<Map<String, Object>> getComponentSchema(@PathVariable("code") String code) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(code)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("code"));
		}
		try {
			Map<String, Object> objectObjectMap = getSchemas(code);
			return Result.success(context.getRequestId(), objectObjectMap);
		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_QUERYCONFIG_ERROR.toError());
		}

	}

	/**
	 * Retrieves schemas for multiple components by their unique codes.
	 * @param request Query containing a list of component codes
	 * @return Result containing a map of component codes to their respective schemas
	 */
	@PostMapping("/schema-by-codes")
	public Result<Map<String, Object>> getComponentSchemaByCodes(@RequestBody AppComponentQuery request) {
		List<String> codes = request.getCodes();
		RequestContext context = RequestContextHolder.getRequestContext();
		if (CollectionUtils.isEmpty(codes)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("codes"));
		}
		try {
			HashMap<String, Object> schemas = new HashMap<>();
			for (String code : codes) {
				Map<String, Object> objectObjectMap = getSchemas(code);
				schemas.put(code, objectObjectMap);
			}
			return Result.success(context.getRequestId(), schemas);
		}
		catch (Exception exception) {
			throw new BizException(ErrorCode.APP_COMPONENT_QUERYCONFIG_ERROR.toError());
		}
	}

	private AppComponent initComponent(AppComponentQuery component) {
		AppComponent appComponent = new AppComponent();
		if (component.getAppName() != null) {
			appComponent.setAppName(component.getAppName());
		}
		if (component.getAppId() != null) {
			appComponent.setAppId(component.getAppId());
		}
		if (component.getCode() != null) {
			appComponent.setCode(component.getCode());
		}
		if (component.getName() != null) {
			appComponent.setName(component.getName());
		}
		if (component.getType() != null) {
			appComponent.setType(component.getType());
		}
		if (component.getDescription() != null) {
			appComponent.setDescription(component.getDescription());
		}
		if (component.getConfig() != null) {
			appComponent.setConfig(component.getConfig());
		}
		appComponent.setStatus(AppComponentStatusEnum.Published.getCode());
		return appComponent;
	}

	private Map<String, Object> getSchemas(String code) {
		AppComponent appComponentByCode = appComponentService.getAppComponentByCode(code,
				AppComponentStatusEnum.Published.getCode());
		Map<String, List<AppComponentConfig.Params>> stringListMap = appComponentManager
			.fetchInputAndOutputParams(appComponentByCode);
		List<AppComponentConfig.Params> input = stringListMap.get("input");
		for (AppComponentConfig.Params appComponentParam : input) {
			appComponentParam.setField(appComponentParam.getAlias());
		}
		String json = JsonUtils.toJson(stringListMap);
		Map<String, Object> objectObjectMap = JsonUtils.fromJsonToMap(json);
		objectObjectMap.put("output_type", "text");
		if (appComponentByCode.getType().equals(AppType.WORKFLOW.getValue())) {
			ApplicationVersion appVersion = appService.getAppVersion(appComponentByCode.getAppId(), "lastPublished");
			if (appVersion == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}
			WorkflowConfig appOrchestraConfig = JsonUtils.fromJson(appVersion.getConfig(), WorkflowConfig.class);
			List<Node> nodes = appOrchestraConfig.getNodes()
				.stream()
				.filter(nodeTmp -> nodeTmp.getType().equals(NodeTypeEnum.END.getCode()))
				.toList();
			Node endNode = nodes.get(0);
			EndExecuteProcessor.NodeParam endNodeParam = JsonUtils.fromMap(endNode.getConfig().getNodeParam(),
					EndExecuteProcessor.NodeParam.class);

			if ("json".equals(endNodeParam.getOutputType())) {
				objectObjectMap.put("output_type", "json");
			}
		}
		return objectObjectMap;
	}

}
