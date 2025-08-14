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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.APIPluginValueSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppComponentTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponent;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.component.AppComponentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.component.CustomParam;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ApiParameter;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.InputSchema;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ParamSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AgentService;
import com.alibaba.cloud.ai.studio.core.base.service.AppComponentService;
import com.alibaba.cloud.ai.studio.core.base.service.PluginService;
import com.alibaba.cloud.ai.studio.core.base.service.WorkflowService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.EndExecuteProcessor;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.SYS_QUERY_KEY;

/**
 * Title: CreateDate: 2025/4/30 14:48
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/
@Slf4j
@Component
public class AppComponentManager {

	private final AppComponentService appComponentService;

	private final PluginService pluginService;

	private final AgentService agentService;

	private final WorkflowService workflowService;

	protected final static String OUTPUT_DECORATE_PARAM_KEY = "output";

	public AppComponentManager(AppComponentService appComponentService, PluginService pluginService,
			@Lazy AgentService agentService, WorkflowService workflowService) {
		this.appComponentService = appComponentService;
		this.pluginService = pluginService;
		this.agentService = agentService;
		this.workflowService = workflowService;
	}

	/**
	 * Get input configuration for the application.
	 * @param application Application object
	 * @param typeFilter Whether to apply type filtering
	 * @return App component configuration
	 */
	public AppComponentConfig getApplicationInputConfig(Application application, Boolean typeFilter) {
		RequestContext context = RequestContextHolder.getRequestContext();
		log.info("getApplicationInputConfig start,application:{},request:{}", application, context);
		if (application == null) {
			// can not find the application
			return null;
		}
		if (application.getType().equals(AppType.BASIC)) {
			AgentConfig agentConfig = JsonUtils.fromJson(application.getPubConfigStr(), AgentConfig.class);
			List<CustomParam> customParams = Lists.newArrayList();
			// get plugins
			List<AgentConfig.Tool> tools = agentConfig.getTools();
			if (CollectionUtils.isNotEmpty(tools)) {
				List<String> toolIds = tools.stream().map(AgentConfig.Tool::getId).toList();
				customParams.addAll(queryPluginParams(toolIds));
			}
			// get application components
			List<String> agentComponents = agentConfig.getAgentComponents();
			if (CollectionUtils.isNotEmpty(agentComponents)) {
				customParams.addAll(queryComponentParams(agentComponents, typeFilter));
			}
			List<String> flowComponents = agentConfig.getWorkflowComponents();
			if (CollectionUtils.isNotEmpty(flowComponents)) {
				customParams.addAll(queryComponentParams(flowComponents, typeFilter));
			}
			AppComponentConfig appComponentConfig = new AppComponentConfig();
			AppComponentConfig.Input input = new AppComponentConfig.Input();
			appComponentConfig.setInput(input);
			// construct appComponentConfig system input params
			constructSystemParams(appComponentConfig.getInput().getSystemParams());
			// construct appComponentConfig user input params
			for (CustomParam customParam : customParams) {
				AppComponentConfig.UserParams userParam = new AppComponentConfig.UserParams();
				appComponentConfig.getInput().getUserParams().add(userParam);
				userParam.setCode(customParam.getCode());
				userParam.setName(customParam.getName());
				userParam.setParams(constructor(customParam.getParams()));
			}
			// Construct appComponentConfig output params
			AppComponentConfig.Params params = new AppComponentConfig.Params();
			params.setField(OUTPUT_DECORATE_PARAM_KEY);
			params.setType("String");
			appComponentConfig.getOutput().add(params);
			return appComponentConfig;
		}
		else if (application.getType() == AppType.WORKFLOW) {

			WorkflowConfig appOrchestraConfig = JsonUtils.fromJson(application.getPubConfigStr(), WorkflowConfig.class);
			List<Node> start = appOrchestraConfig.getNodes()
				.stream()
				.filter(node -> node.getType().equals(NodeTypeEnum.START.getCode()))
				.toList();
			Node startNode = start.get(0);
			List<Node.OutputParam> startOutputParams = startNode.getConfig().getOutputParams();
			AppComponentConfig appComponentConfig = new AppComponentConfig();
			AppComponentConfig.Input input = new AppComponentConfig.Input();
			appComponentConfig.setInput(input);
			// Construct appComponentConfig system params
			constructSystemParams(appComponentConfig.getInput().getSystemParams());
			// Construct appComponentConfig user params
			AppComponentConfig.UserParams userParam = new AppComponentConfig.UserParams();
			appComponentConfig.getInput().getUserParams().add(userParam);
			userParam.setCode(application.getAppId());
			userParam.setName(application.getName());
			for (Node.OutputParam outputParam : startOutputParams) {
				AppComponentConfig.Params params = new AppComponentConfig.Params();
				params.setField(outputParam.getKey());
				params.setType(outputParam.getType());
				params.setDescription(outputParam.getDesc());
				params.setAlias(outputParam.getKey());
				userParam.getParams().add(params);
			}
			// construct appComponentConfig output params
			List<Node> end = appOrchestraConfig.getNodes()
				.stream()
				.filter(node -> node.getType().equals("End"))
				.toList();
			Node endNode = end.get(0);
			EndExecuteProcessor.NodeParam nodeParam = JsonUtils.fromMap(endNode.getConfig().getNodeParam(),
					EndExecuteProcessor.NodeParam.class);
			if (nodeParam.getOutputType().equals("json")) {
				for (Node.InputParam inputParam : nodeParam.getJsonParams()) {
					AppComponentConfig.Params params = new AppComponentConfig.Params();
					params.setField(inputParam.getKey());
					params.setType(inputParam.getType());
					appComponentConfig.getOutput().add(params);
				}
			}
			else {
				AppComponentConfig.Params params = new AppComponentConfig.Params();
				params.setField(OUTPUT_DECORATE_PARAM_KEY);
				params.setType("String");
				appComponentConfig.getOutput().add(params);
			}
			return appComponentConfig;
		}
		return null;
	}

	/**
	 * Construct parameter list from custom parameters.
	 * @param toolParams List of custom parameters
	 * @return List of app component config parameters
	 */
	private List<AppComponentConfig.Params> constructor(List<CustomParam.Param> toolParams) {
		List<AppComponentConfig.Params> resultList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(toolParams)) {
			for (CustomParam.Param param : toolParams) {
				AppComponentConfig.Params params = new AppComponentConfig.Params();
				params.setField(param.getField());
				params.setType(param.getType());
				params.setDescription(param.getDescription());
				params.setRequired(param.isRequired());
				params.setAlias(param.getField());
				resultList.add(params);
			}
		}
		return resultList;
	}

	/**
	 * Construct system parameters like query and imageList.
	 * @param systemParams List to store system parameters
	 */
	private void constructSystemParams(List<AppComponentConfig.Params> systemParams) {
		AppComponentConfig.Params queryParams = new AppComponentConfig.Params();
		queryParams.setField(SYS_QUERY_KEY);
		queryParams.setType("String");
		queryParams.setRequired(true);
		queryParams.setAlias(SYS_QUERY_KEY);
		systemParams.add(queryParams);

	}

	/**
	 * Get input configuration for an app component.
	 * @param appComponent App component object
	 * @return App component configuration
	 */
	public AppComponentConfig getAppComponentInputConfig(AppComponent appComponent) {
		if (appComponent == null) {
			return null;
		}
		String config = appComponent.getConfig();
		if (config != null) {
			return JsonUtils.fromJson(config, AppComponentConfig.class);
		}
		return null;

	}

	public HashMap<String, ToolCallSchema> getToolCallSchema(List<String> codes) {
		if (CollectionUtils.isEmpty(codes)) {
			return null;
		}
		HashMap<String, ToolCallSchema> toolCallSchemas = new HashMap<>();
		for (String code : codes) {
			AppComponent appComponent = appComponentService.getAppComponentByCode(code,
					AppComponentStatusEnum.Published.getCode());
			if (appComponent == null) {
				throw new BizException(ErrorCode.INVALID_PARAMS.toError("component_code",
						"can not find published component: " + code));
			}

			AppComponentConfig appComponentConfig = getAppComponentInputConfig(appComponent);
			ToolCallSchema toolCallSchema = new ToolCallSchema();
			toolCallSchema.setName(appComponent.getName());
			toolCallSchema.setDescription(appComponent.getDescription());
			AppComponentConfig.Input input = appComponentConfig.getInput();
			InputSchema inputSchema = new InputSchema();
			toolCallSchema.setInputSchema(inputSchema);
			Map<String, Object> properties = new HashMap<>();
			List<String> required = new ArrayList<>();
			inputSchema.setProperties(properties);
			inputSchema.setRequired(required);
			for (AppComponentConfig.UserParams userParam : input.getUserParams()) {
				if (CollectionUtils.isNotEmpty(userParam.getParams())) {
					for (AppComponentConfig.Params param : userParam.getParams()) {
						if (Objects.equals(param.getSource(), APIPluginValueSourceEnum.BIZ.getCode())) {
							continue;
						}
						if (param.getRequired()) {
							required.add(param.getField());
						}
						HashMap<String, Object> paramMap = new HashMap<>();
						paramMap.put("type", param.getType());
						paramMap.put("description", param.getDescription());
						properties.put(param.getAlias(), paramMap);
					}
				}
			}

			if (CollectionUtils.isNotEmpty(input.getSystemParams())) {
				for (AppComponentConfig.Params param : input.getSystemParams()) {
					if (Objects.equals(param.getSource(), APIPluginValueSourceEnum.BIZ.getCode())) {
						continue;
					}
					if (param.getRequired()) {
						required.add(param.getField());
					}
					HashMap<String, Object> paramMap = new HashMap<>();
					paramMap.put("type", param.getType());
					paramMap.put("description", param.getDescription());
					properties.put(param.getAlias(), paramMap);
				}
			}

			toolCallSchemas.put(code, toolCallSchema);
		}
		return toolCallSchemas;
	}

	/**
	 * Merge two configurations into one.
	 * @param applicationConfig Base configuration
	 * @param appComponentConfig Configuration to merge
	 * @return Merged configuration
	 */
	public AppComponentConfig mergeConfig(AppComponentConfig applicationConfig, AppComponentConfig appComponentConfig) {

		// merge userParams
		List<AppComponentConfig.UserParams> userParams1 = applicationConfig.getInput().getUserParams();
		List<AppComponentConfig.UserParams> userParams2 = appComponentConfig.getInput().getUserParams();
		HashMap<String, AppComponentConfig.UserParams> userParamsMap = new HashMap<>();
		for (AppComponentConfig.UserParams userParam2 : userParams2) {
			userParamsMap.put(userParam2.getCode(), userParam2);
		}
		for (AppComponentConfig.UserParams userParam1 : userParams1) {
			if (userParamsMap.containsKey(userParam1.getCode())) {
				AppComponentConfig.UserParams userParam2 = userParamsMap.get(userParam1.getCode());
				HashMap<String, AppComponentConfig.Params> paramsMap = new HashMap<>();
				if (CollectionUtils.isNotEmpty(userParam2.getParams())) {
					for (AppComponentConfig.Params param2 : userParam2.getParams()) {
						paramsMap.put(param2.getField(), param2);
					}
				}

				for (AppComponentConfig.Params param1 : userParam1.getParams()) {
					if (paramsMap.containsKey(param1.getField())) {
						AppComponentConfig.Params param2 = paramsMap.get(param1.getField());
						param1.setSource(param2.getSource());
						param1.setAlias(param2.getAlias());
						param1.setDefaultValue(param2.getDefaultValue());
						param1.setDescription(param2.getDescription());
						param1.setRequired(param2.getRequired());
						param1.setDisplay(param2.getDisplay());
						param1.setDescription(param2.getDescription());
					}
				}

			}
		}

		// merge systemParams
		List<AppComponentConfig.Params> systemParams2 = appComponentConfig.getInput().getSystemParams();
		List<AppComponentConfig.Params> systemParams1 = applicationConfig.getInput().getSystemParams();
		HashMap<String, AppComponentConfig.Params> systemParamsMap = new HashMap<>();
		for (AppComponentConfig.Params systemParam2 : systemParams2) {
			systemParamsMap.put(systemParam2.getField(), systemParam2);

		}

		for (AppComponentConfig.Params systemParam1 : systemParams1) {
			if (systemParamsMap.containsKey(systemParam1.getField())) {
				AppComponentConfig.Params systemParam2 = systemParamsMap.get(systemParam1.getField());
				systemParam1.setRequired(systemParam2.getRequired());
				systemParam1.setDisplay(systemParam2.getDisplay());
				systemParam1.setDefaultValue(systemParam2.getDefaultValue());
				systemParam1.setAlias(systemParam2.getAlias());
				systemParam1.setDescription(systemParam2.getDescription());
				systemParam1.setSource(systemParam2.getSource());
			}
		}

		return applicationConfig;

	}

	/**
	 * Convert a single application to an app component DTO.
	 * @param application Application object
	 * @return App component DTO
	 */
	private AppComponent toAppComponentDTO(Application application) {
		AppComponent appComponentDTO = new AppComponent();
		appComponentDTO.setAppId(application.getAppId());
		appComponentDTO.setAppName(application.getName());
		appComponentDTO.setType(application.getType().getValue());
		appComponentDTO.setDescription(application.getDescription());
		return appComponentDTO;

	}

	/**
	 * Query plugin parameters by plugin codes.
	 * @param pluginCodeList List of plugin codes
	 * @return List of custom parameters
	 */
	public List<CustomParam> queryPluginParams(List<String> pluginCodeList) {
		List<CustomParam> customParams = Lists.newArrayList();
		List<Tool> tools = pluginService.getTools(pluginCodeList);
		if (CollectionUtils.isEmpty(tools)) {
			return customParams;
		}
		tools.forEach(tool -> {
			Plugin plugin = pluginService.getPlugin(tool.getPluginId());
			if (plugin != null) {
				CustomParam customParam = new CustomParam();
				List<CustomParam.Param> params = new ArrayList<>();

				// user inputSchema check
				List<ApiParameter> inputParams = tool.getConfig().getInputParams();
				if (CollectionUtils.isNotEmpty(inputParams)) {
					for (ApiParameter inputParam : inputParams) {
						CustomParam.Param param = new CustomParam.Param();
						param.setField(inputParam.getKey());
						param.setType(inputParam.getType());
						param.setRequired(inputParam.isRequired());
						param.setDescription(inputParam.getDescription());
						params.add(param);
					}
				}
				if (CollectionUtils.isNotEmpty(params)) {
					customParam.setCode(tool.getToolId());
					customParam.setName(tool.getName());
					customParam.setDescription(tool.getDescription());
					customParam.setParams(params);
					customParam.setType(CustomParam.ParamType.plugin.name());
					customParams.add(customParam);
				}
			}
		});

		return customParams;
	}

	/**
	 * Fetch input and output schema of a component.
	 * @param appComponent App component object
	 * @return Map containing input and output parameters
	 */
	public Map<String, List<AppComponentConfig.Params>> fetchInputAndOutputParams(AppComponent appComponent) {
		Map<String, List<AppComponentConfig.Params>> resultMap = new HashMap<>();
		AppComponentConfig appComponentInputConfig = getAppComponentInputConfig(appComponent);
		AppComponentConfig.Input input = appComponentInputConfig.getInput();
		List<AppComponentConfig.Params> inputParams = Lists.newArrayList();
		if (input != null) {
			List<AppComponentConfig.Params> systemParams = input.getSystemParams();
			if (CollectionUtils.isNotEmpty(systemParams)) {
				// keep only display parameters
				inputParams
					.addAll(systemParams.stream().filter(param -> BooleanUtils.isTrue(param.getDisplay())).toList());
			}
			List<AppComponentConfig.UserParams> userParams = input.getUserParams();
			if (CollectionUtils.isNotEmpty(userParams)) {
				userParams.forEach(userParam -> {
					List<AppComponentConfig.Params> params = userParam.getParams();
					if (CollectionUtils.isNotEmpty(params)) {
						// keep only display parameters
						inputParams
							.addAll(params.stream().filter(param -> BooleanUtils.isTrue(param.getDisplay())).toList());
					}
				});
			}
		}
		resultMap.put("input", inputParams);
		List<AppComponentConfig.Params> output = appComponentInputConfig.getOutput();
		if (output != null) {
			resultMap.put("output", output);
		}

		return resultMap;
	}

	/**
	 * Query component parameters by component codes.
	 * @param ComponentCodeList List of component codes
	 * @param typeFilter Whether to apply type filtering
	 * @return List of custom parameters
	 */
	public List<CustomParam> queryComponentParams(List<String> ComponentCodeList, Boolean typeFilter) {

		List<CustomParam> customParamDTOS = Lists.newArrayList();
		List<AppComponent> appComponentDTOs = appComponentService.getAppComponentListByCodes(ComponentCodeList);
		if (appComponentDTOs == null) {
			return customParamDTOS;
		}
		// get component config
		for (AppComponent appComponent : appComponentDTOs) {
			Map<String, List<AppComponentConfig.Params>> AppComponentConfigMap = fetchInputAndOutputParams(
					appComponent);
			List<AppComponentConfig.Params> inputParams = AppComponentConfigMap.get("input");
			CustomParam customParam = new CustomParam();
			customParam.setParams(new ArrayList<>());
			// When creating an application component for the first time, filter out the
			// parameters of model identified
			if (inputParams != null && typeFilter) {
				inputParams = inputParams.stream().filter(inputParam -> {
					String valueSource = inputParam.getSource();
					return APIPluginValueSourceEnum.BIZ.getCode().equals(valueSource);
				}).collect(Collectors.toList());
			}
			if (CollectionUtils.isNotEmpty(inputParams)) {
				customParamDTOS.add(customParam);
				customParam.setName(appComponent.getName());
				customParam.setCode(appComponent.getCode());
				if (Objects.equals(appComponent.getType(), AppComponentTypeEnum.Agent.getValue())) {
					customParam.setType(CustomParam.ParamType.agentComponent.name());
				}
				else {
					customParam.setType(CustomParam.ParamType.flow.name());
				}
				for (AppComponentConfig.Params inputParam : inputParams) {
					CustomParam.Param param = new CustomParam.Param();
					param.setField(inputParam.getAlias());
					param.setType(inputParam.getType());
					param.setDescription(inputParam.getDescription());
					param.setRequired(inputParam.getRequired());
					customParam.getParams().add(param);
				}
			}
		}
		return customParamDTOS;
	}

	/**
	 * Execute agent component with given request.
	 * @param request App component request
	 * @return Stream of agent responses
	 */
	public Flux<AgentResponse> executeAgentComponentStream(AppComponentRequest request) {
		AgentRequest agentRequest = buildAgentRequest(request);
		if (agentRequest == null) {
			return null;
		}
		return agentService.streamCall(Flux.just(agentRequest));
	}

	/**
	 * Execute workflow component with given request.
	 * @param request App component request
	 * @return Stream of workflow responses
	 */
	public Flux<WorkflowResponse> executeWorkflowComponentStream(AppComponentRequest request) {
		WorkflowRequest workflowRequest = buildWorkflowRequest(request);
		if (workflowRequest == null) {
			return null;
		}
		return workflowService.streamCall(Flux.just(workflowRequest));
	}

	/**
	 * Execute agent component with given request.
	 * @param request App component request
	 * @return agent responses
	 */
	public AgentResponse executeAgentComponent(AppComponentRequest request) {
		AgentRequest agentRequest = buildAgentRequest(request);
		if (agentRequest == null) {
			return null;
		}
		agentRequest.setStream(false);
		return agentService.call(agentRequest);
	}

	/**
	 * Execute workflow component with given request.
	 * @param request App component request
	 * @return workflow responses
	 */
	public WorkflowResponse executeWorkflowComponent(AppComponentRequest request) {
		WorkflowRequest workflowRequest = buildWorkflowRequest(request);
		if (workflowRequest == null) {
			return null;
		}
		workflowRequest.setStream(false);
		return workflowService.call(workflowRequest);
	}

	/**
	 * Build agent request from app component request.
	 * @param request App component request
	 * @return Agent request object
	 */
	private AgentRequest buildAgentRequest(AppComponentRequest request) {

		String code = request.getCode();
		AppComponent appComponentByCode = appComponentService.getAppComponentByCode(code,
				AppComponentStatusEnum.Published.getCode());
		if (appComponentByCode == null) {
			// component not exist
			return null;
		}
		AgentRequest agentRequest = new AgentRequest();
		agentRequest.setAppId(appComponentByCode.getAppId());
		agentRequest.setMessages(request.getMessages());
		agentRequest.setStream(request.getStreamMode());
		Map<String, List<AppComponentConfig.Params>> AppComponentConfigMap = fetchInputAndOutputParams(
				appComponentByCode);
		List<AppComponentConfig.Params> input = AppComponentConfigMap.get("input");
		Map<String, Object> bizVars = request.getBizVars();
		if (CollectionUtils.isNotEmpty(input)) {
			HashMap<String, Object> userDefinedParamMap = new HashMap<>();
			List<ChatMessage> messages = new ArrayList<>();
			// handle system params
			for (AppComponentConfig.Params appComponentParam : input) {
				if (appComponentParam.getField().equals(SYS_QUERY_KEY)) {
					if (bizVars.containsKey(appComponentParam.getAlias())) {
						ChatMessage chatMessage = new ChatMessage();
						chatMessage.setRole(MessageRole.USER);
						chatMessage.setContent(bizVars.get(appComponentParam.getAlias()));
						messages.add(chatMessage);

					}
					else if (appComponentParam.getDefaultValue() != null) {
						ChatMessage chatMessage = new ChatMessage();
						chatMessage.setRole(MessageRole.USER);
						chatMessage.setContent(bizVars.get(appComponentParam.getDefaultValue().toString()));
						messages.add(chatMessage);
					}

					if (CollectionUtils.isNotEmpty(agentRequest.getMessages())) {
						agentRequest.getMessages().addAll(messages);
					}
					else {
						agentRequest.setMessages(messages);
					}
				}
			}
			// handle user params
			AppComponentConfig appComponentInputConfig = getAppComponentInputConfig(appComponentByCode);
			AppComponentConfig.Input input1 = appComponentInputConfig.getInput();
			for (AppComponentConfig.UserParams userParam : input1.getUserParams()) {
				HashMap<String, Object> paramMap = new HashMap<>();
				for (AppComponentConfig.Params userParamParam : userParam.getParams()) {
					if (bizVars.containsKey(userParamParam.getAlias())) {
						paramMap.put(userParamParam.getField(), bizVars.get(userParamParam.getAlias()));
					}
					else if (userParamParam.getDefaultValue() != null) {
						paramMap.put(userParamParam.getField(), userParamParam.getDefaultValue());
					}
					else if (userParamParam.getRequired()) {
						throw new RuntimeException("user define param:" + userParamParam.getField() + "is empty");
					}
				}
				if (!paramMap.isEmpty()) {
					userDefinedParamMap.put(userParam.getCode(), paramMap);
				}
			}
			agentRequest.setExtraPrams(userDefinedParamMap);
			return agentRequest;
		}
		return null;
	}

	/**
	 * Build workflow request from app component request.
	 * @param request App component request
	 * @return Workflow request object
	 */
	private WorkflowRequest buildWorkflowRequest(AppComponentRequest request) {

		String code = request.getCode();
		AppComponent appComponentByCode = appComponentService.getAppComponentByCode(code,
				AppComponentStatusEnum.Published.getCode());
		if (appComponentByCode == null) {
			// component not exist
			return null;
		}
		WorkflowRequest workflowRequest = new WorkflowRequest();
		workflowRequest.setAppId(appComponentByCode.getAppId());
		workflowRequest.setMessages(request.getMessages());
		workflowRequest.setStream(request.getStreamMode());
		Map<String, List<AppComponentConfig.Params>> AppComponentConfigMap = fetchInputAndOutputParams(
				appComponentByCode);
		List<AppComponentConfig.Params> input = AppComponentConfigMap.get("input");
		Map<String, Object> bizVars = request.getBizVars();
		if (CollectionUtils.isNotEmpty(input)) {
			List<ChatMessage> messages = new ArrayList<>();
			List<TaskRunParam> inputParams = Lists.newArrayList();

			for (AppComponentConfig.Params appComponentParam : input) {
				// handle system params
				if (appComponentParam.getField().equals(SYS_QUERY_KEY)) {
					if (bizVars.containsKey(appComponentParam.getAlias())) {
						TaskRunParam taskRunParam = new TaskRunParam();
						taskRunParam.setSource(ParamSourceEnum.sys.name());
						taskRunParam.setKey(appComponentParam.getField());
						taskRunParam.setValue(bizVars.get(appComponentParam.getAlias()));
						taskRunParam.setType(appComponentParam.getType());
						taskRunParam.setDesc(appComponentParam.getDescription());
						inputParams.add(taskRunParam);

					}
					else if (appComponentParam.getDefaultValue() != null) {
						TaskRunParam taskRunParam = new TaskRunParam();
						taskRunParam.setSource(ParamSourceEnum.sys.name());
						taskRunParam.setKey(appComponentParam.getField());
						taskRunParam.setValue(appComponentParam.getDefaultValue());
						taskRunParam.setType(appComponentParam.getType());
						taskRunParam.setDesc(appComponentParam.getDescription());
						inputParams.add(taskRunParam);
					}

					if (CollectionUtils.isNotEmpty(workflowRequest.getMessages())) {
						workflowRequest.getMessages().addAll(messages);
					}
					else {
						workflowRequest.setMessages(messages);
					}
					continue;
				}
				// handle user params
				if (bizVars.containsKey(appComponentParam.getAlias())) {
					TaskRunParam taskRunParam = new TaskRunParam();
					taskRunParam.setKey(appComponentParam.getField());
					taskRunParam.setValue(bizVars.get(appComponentParam.getAlias()));
					taskRunParam.setSource("user");
					taskRunParam.setType(appComponentParam.getType());
					taskRunParam.setDesc(appComponentParam.getDescription());
					inputParams.add(taskRunParam);
				}
				else if (appComponentParam.getDefaultValue() != null) {
					TaskRunParam taskRunParam = new TaskRunParam();
					taskRunParam.setKey(appComponentParam.getField());
					taskRunParam.setValue(appComponentParam.getDefaultValue());
					taskRunParam.setSource("user");
					taskRunParam.setType(appComponentParam.getType());
					taskRunParam.setDesc(appComponentParam.getDescription());
					inputParams.add(taskRunParam);
				}
			}
			workflowRequest.setInputParams(inputParams);
		}
		return workflowRequest;
	}

}
