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

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.InvokeSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ParamSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.ApiTaskMsg;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.ApiTaskRunRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.InitRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.ProcessGetRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.ProcessGetResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskPartGraphRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskPartGraphResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskResumeRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskResumeResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskStopRequest;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.IteratorExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.runtime.WorkflowExecuteManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_TASK_CONTEXT_PREFIX;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.SYS_QUERY_KEY;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;

/**
 * REST controller for workflow operations in the SAA Studio platform. This controller
 * provides comprehensive workflow management capabilities including: 1. Workflow Debug
 * Operations: - Execute workflow tasks in debug mode - Monitor task execution progress -
 * Initialize debug parameters - Resume paused tasks - Execute partial workflow graphs 2.
 * Workflow Execution Operations: - Stream workflow execution events using SSE - Execute
 * workflow tasks with real-time monitoring
 *
 * The controller supports both synchronous and asynchronous (streaming) execution modes,
 * providing flexibility for different use cases. It integrates with Redis for context
 * management and supports workspace-based isolation.
 *
 * @since 1.0.0.3
 */
@Slf4j
@RestController
@Tag(name = "workflow")
@RequestMapping("/console/v1/apps")
public class WorkflowController {

	private final RedisManager redisManager;

	private final AppService appService;

	private final WorkflowExecuteManager workflowExecuteManager;

	private final WorkflowInnerService workflowInnerService;

	public WorkflowController(RedisManager redisManager, AppService appService,
			WorkflowExecuteManager workflowExecuteManager, WorkflowInnerService workflowInnerService) {
		this.redisManager = redisManager;
		this.appService = appService;
		this.workflowExecuteManager = workflowExecuteManager;
		this.workflowInnerService = workflowInnerService;
	}

	/**
	 * Executes a workflow task in debug mode. This endpoint allows running workflow tasks
	 * with debug capabilities, enabling step-by-step execution and detailed monitoring.
	 * @param request Task execution request containing: - appId: Application identifier -
	 * version: Application version (defaults to "latest") - inputs: Input parameters for
	 * the workflow - conversationId: Session identifier for tracking
	 * @return TaskRunResponse containing: - taskId: Unique identifier for the executed
	 * task - conversationId: Session identifier - requestId: Request tracking identifier
	 */
	@PostMapping(value = { "/workflow/debug/run-task" })
	public Result<TaskRunResponse> runDebugTask(@RequestBody TaskRunRequest request) {
		if (request == null || StringUtils.isBlank(request.getAppId())) {
			LogUtils.monitor("WorkflowService", "runTask", System.currentTimeMillis(), FAIL, request,
					"request or appId is null");
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("request or appId is null"));
		}
		String version = request.getVersion();
		if (version == null) {
			version = "latest";
		}
		ApplicationVersion appVersion = appService.getAppVersion(request.getAppId(), version);
		WorkflowContext workflowContext = new WorkflowContext();
		workflowContext.setInvokeSource(InvokeSourceEnum.console.getCode());
		TaskRunResponse response = workflowExecuteManager.runTask(appVersion, request.getInputs(),
				request.getConversationId(), workflowContext);
		return Result.success(response);
	}

	/**
	 * Retrieves the current execution status of a workflow task. This endpoint provides
	 * detailed information about the task's progress, including node execution status and
	 * results.
	 * @param request Process status request containing: - taskId: Task identifier to
	 * query
	 * @return ProcessGetResponse containing: - taskStatus: Current status of the task -
	 * nodeResults: List of node execution results - taskResults: Overall task execution
	 * results - errorInfo: Error information if task failed
	 */
	@PostMapping(value = { "/workflow/debug/get-task-process" })
	public Result<ProcessGetResponse> getDebugProcess(@RequestBody ProcessGetRequest request) {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		try {
			WorkflowContext wfContext = redisManager
				.get(WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + request.getTaskId());
			if (wfContext == null) {
				log.info(
						"log used for query debug task result, taskId:{}, RequestId:{}, costTime:{}, getProcessStatus:{}",
						request.getTaskId(), context.getRequestId(), System.currentTimeMillis() - start, "error");
				return Result.error(ErrorCode.WORKFLOW_NODE_DEBUG_FAIL);
			}
			ProcessGetResponse response = constructProcessGetResponse(request.getTaskId(), wfContext);
			List<NodeResult> filterNodeResult = response.getNodeResults()
				.stream()
				.filter(nodeResult -> nodeResult != null
						&& !nodeResult.getNodeStatus().equals(NodeStatusEnum.SKIP.getCode()))
				.collect(Collectors.toList());
			response.setNodeResults(filterNodeResult);
			return Result.success(response);
		}
		catch (Exception e) {
			log.error("WorkflowController getProcess error requestId:{}, costTime:{}", context.getRequestId(),
					System.currentTimeMillis() - start, e);
		}
		return Result.error(ErrorCode.WORKFLOW_DEBUG_GET_PROCESS_FAIL);
	}

	/**
	 * Initializes workflow debug parameters. This endpoint prepares the necessary
	 * parameters for workflow debugging, including system and user-defined parameters.
	 * @param request Initialization request containing: - appId: Application identifier -
	 * version: Application version (defaults to "latest")
	 * @return List of TaskRunParam containing: - key: Parameter key - type: Parameter
	 * type - source: Parameter source (system/user) - required: Whether parameter is
	 * required
	 */
	@PostMapping(value = { "/workflow/debug/init" })
	public Result<List<TaskRunParam>> debugInit(@RequestBody InitRequest request) {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());
		List<TaskRunParam> initTaskRunParams = Lists.newArrayList();
		try {
			String version = request.getVersion();
			if (version == null) {
				version = "latest";
			}
			// Get application details
			ApplicationVersion appVersion = appService.getAppVersion(request.getAppId(), version);
			WorkflowConfig config = JsonUtils.fromJson(appVersion.getConfig(), WorkflowConfig.class);
			List<Node> nodes = config.getNodes();
			if (CollectionUtils.isNotEmpty(nodes)) {
				Optional<Node> startOptional = nodes.stream()
					.filter(node -> node.getType().equals(NodeTypeEnum.START.getCode()))
					.findAny();
				if (startOptional.isPresent()) {
					Node startNode = startOptional.get();
					List<Node.OutputParam> outputParams = startNode.getConfig().getOutputParams();
					if (CollectionUtils.isEmpty(outputParams)) {
						initTaskRunParams = Lists.newArrayList();
					}
					else {
						initTaskRunParams.addAll(outputParams.stream().map(outputParam -> {
							TaskRunParam taskRunParam = BeanCopierUtils.copy(outputParam, TaskRunParam.class);
							taskRunParam.setSource(ParamSourceEnum.user.name());
							taskRunParam.setRequired(false);
							return taskRunParam;
						}).collect(Collectors.toList()));
					}
					// Add system parameters
					TaskRunParam queryParam = new TaskRunParam();
					queryParam.setKey(SYS_QUERY_KEY);
					queryParam.setDesc("User query");
					queryParam.setType("String");
					queryParam.setRequired(false);
					queryParam.setSource(ParamSourceEnum.sys.name());
					initTaskRunParams.add(queryParam);
				}
			}
			return Result.success(initTaskRunParams);
		}
		catch (Exception e) {
			log.error("WorkflowController debugInit error requestId:{}, costTime:{}", context.getRequestId(),
					System.currentTimeMillis() - start, e);
		}
		return Result.error(ErrorCode.WORKFLOW_DEBUG_INIT_FAIL);
	}

	/**
	 * Constructs process status response from workflow context.
	 * @param taskId Task ID
	 * @param context Workflow context
	 * @return Process status response
	 */
	private ProcessGetResponse constructProcessGetResponse(String taskId, WorkflowContext context) {
		ProcessGetResponse response = new ProcessGetResponse();
		// Construct input parameters
		response.setTaskStatus(context.getTaskStatus());
		response.setTaskId(taskId);
		response.setRequestId(context.getRequestId());
		response.setConversationId(context.getConversationId());
		ConcurrentHashMap<String, NodeResult> nodeResultMap = context.getNodeResultMap();
		List<NodeResult> list = Lists.newArrayList();
		CopyOnWriteArrayList<String> executeOrderList = context.getExecuteOrderList();
		List<ProcessGetResponse.ProcessOutput> processOutputList = Lists.newArrayList();
		Set<String> nodeIdSet = Sets.newHashSet();
		executeOrderList.stream().forEach(nodeId -> {
			if (!nodeIdSet.contains(nodeId)) {
				NodeResult currentNodeResult = nodeResultMap.get(nodeId);
				if (currentNodeResult == null) {
					return;
				}
				list.add(currentNodeResult);
				if ((NodeTypeEnum.OUTPUT.getCode().equals(currentNodeResult.getNodeType())
						|| NodeTypeEnum.END.getCode().equals(currentNodeResult.getNodeType()))
						&& (NodeStatusEnum.EXECUTING.getCode().equals(currentNodeResult.getNodeStatus())
								|| NodeStatusEnum.SUCCESS.getCode().equals(currentNodeResult.getNodeStatus()))) {
					// For output nodes, directly concatenate the content
					ProcessGetResponse.ProcessOutput processOutput = new ProcessGetResponse.ProcessOutput();
					processOutput.setNodeId(currentNodeResult.getNodeId());
					processOutput.setNodeName(currentNodeResult.getNodeName());
					processOutput.setNodeType(currentNodeResult.getNodeType());
					processOutput.setNodeStatus(currentNodeResult.getNodeStatus());
					processOutput.setNodeContent(currentNodeResult.getOutput());
					processOutputList.add(processOutput);
				}
				else if (NodeTypeEnum.INPUT.getCode().equals(currentNodeResult.getNodeType())) {
					if (StringUtils.isNotBlank(currentNodeResult.getParentNodeId())) {
						int index = currentNodeResult.getIndex();
						for (int i = 0; i < index; i++) {
							ProcessGetResponse.ProcessOutput processOutput = fetchProcessOutput(currentNodeResult);
							processOutput.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
							processOutput.setIndex(i);
							processOutputList.add(processOutput);
						}
						ProcessGetResponse.ProcessOutput processOutput = fetchProcessOutput(currentNodeResult);
						processOutput.setIndex(index);
						processOutput.setNodeStatus(currentNodeResult.getNodeStatus());
						processOutputList.add(processOutput);
					}
					else {
						ProcessGetResponse.ProcessOutput processOutput = fetchProcessOutput(currentNodeResult);
						processOutputList.add(processOutput);
					}
				}
			}
			nodeIdSet.add(nodeId);
		});
		response.setNodeResults(list);
		// Add task results
		response.setTaskResults(processOutputList);
		if (NodeStatusEnum.SUCCESS.getCode().equals(context.getTaskStatus())) {
			response.setTaskExecTime((System.currentTimeMillis() - context.getStartTime()) + "ms");
		}
		if (NodeStatusEnum.FAIL.getCode().equals(context.getTaskStatus())) {
			response.setTaskExecTime((System.currentTimeMillis() - context.getStartTime()) + "ms");
			response.setErrorCode(context.getErrorCode());
			response.setErrorInfo(context.getErrorInfo());
		}
		return response;
	}

	/**
	 * Fetches process output from node result.
	 * @param currentNodeResult Current node execution result
	 * @return Process output details
	 */
	private ProcessGetResponse.ProcessOutput fetchProcessOutput(NodeResult currentNodeResult) {
		ProcessGetResponse.ProcessOutput processOutput = new ProcessGetResponse.ProcessOutput();
		processOutput.setNodeId(currentNodeResult.getNodeId());
		processOutput.setNodeName(currentNodeResult.getNodeName());
		processOutput.setNodeType(currentNodeResult.getNodeType());
		processOutput.setNodeStatus(currentNodeResult.getNodeStatus());
		processOutput.setNodeContent(Lists.newArrayList());
		processOutput.setParentNodeId(currentNodeResult.getParentNodeId());
		if (currentNodeResult.getInput() != null) {
			List<Node.OutputParam> outputParams = JsonUtils.fromJsonToList(currentNodeResult.getInput(),
					Node.OutputParam.class);
			if (currentNodeResult.getOutput() != null) {
				Map<Object, Object> outputMap = JsonUtils.fromJsonToMap(currentNodeResult.getOutput());
				outputParams.stream().forEach(outputParam -> {
					outputParam.setValue(outputMap.get(outputParam.getKey()));
				});
			}
			processOutput.setNodeContent(outputParams);
		}
		return processOutput;
	}

	/**
	 * Resumes a paused workflow task. This endpoint allows continuing execution of a
	 * previously paused workflow task, with the ability to provide new input parameters.
	 * @param request Task resume request containing: - taskId: Task identifier to resume
	 * - resumeNodeId: Node identifier to resume from - resumeParentId: Parent node
	 * identifier (for nested nodes) - inputParams: New input parameters for the resumed
	 * execution
	 * @return TaskResumeResponse containing: - taskId: Task identifier - requestId:
	 * Request tracking identifier
	 */
	@PostMapping(value = { "/workflow/debug/resume-task" })
	public Result<TaskResumeResponse> resumeDebugTask(@RequestBody TaskResumeRequest request) {
		long start = System.currentTimeMillis();
		RequestContext context = RequestContextHolder.getRequestContext();
		try {
			if (request == null || request.getTaskId() == null || request.getResumeNodeId() == null) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("request or appId is null"));
			}
			WorkflowContext wfContext = redisManager
				.get(WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + request.getTaskId());
			if (wfContext == null) {
				return Result.error(IdGenerator.uuid(), ErrorCode.WORKFLOW_CONFIG_INVALID.toError("taskId not exists"));
			}
			Node inputNode = null;
			// 组装输入节点内容
			if (StringUtils.isBlank(request.getResumeParentId())) {
				Optional<Node> inputNodeOptional = wfContext.getWorkflowConfig()
					.getNodes()
					.stream()
					.filter(node -> node.getId().equals(request.getResumeNodeId()))
					.findFirst();
				if (inputNodeOptional.isPresent()) {
					inputNode = inputNodeOptional.get();
				}
			}
			else {
				Optional<Node> inputNodeOptional = wfContext.getWorkflowConfig()
					.getNodes()
					.stream()
					.filter(node -> node.getId().equals(request.getResumeParentId()))
					.findFirst();
				if (inputNodeOptional.isPresent()) {
					Node parentNode = inputNodeOptional.get();
					IteratorExecuteProcessor.NodeParam nodeParam = JsonUtils
						.fromMap(parentNode.getConfig().getNodeParam(), IteratorExecuteProcessor.NodeParam.class);
					Optional<Node> subInputNodeOptional = nodeParam.getBlock()
						.getNodes()
						.stream()
						.filter(node -> node.getId().equals(request.getResumeNodeId()))
						.findFirst();
					if (subInputNodeOptional.isPresent()) {
						inputNode = subInputNodeOptional.get();
					}
				}

			}

			if (inputNode == null) {
				return Result.error(IdGenerator.uuid(),
						ErrorCode.WORKFLOW_CONFIG_INVALID.toError("resume_node_id not exists"));
			}
			NodeResult nodeResultFront = wfContext.getNodeResultMap().get(request.getResumeNodeId());
			if (nodeResultFront == null || nodeResultFront.getNodeStatus().equals(NodeStatusEnum.PAUSE.getCode())) {
				NodeResult nodeResult = new NodeResult();
				nodeResult.setNodeId(inputNode.getId());
				nodeResult.setNodeName(inputNode.getName());
				nodeResult.setNodeType(inputNode.getType());
				nodeResult.setUsages(null);
				if (nodeResultFront != null) {
					nodeResult.setBatches(nodeResultFront.getBatches());
				}
				List<Node.OutputParam> outputParams = inputNode.getConfig().getOutputParams();
				nodeResult.setInput(JsonUtils.toJson(outputParams));
				if (CollectionUtils.isNotEmpty(outputParams)) {
					List<CommonParam> inputParams = request.getInputParams();
					Map<String, Object> inputMap = Maps.newHashMap();
					if (CollectionUtils.isNotEmpty(inputParams)) {
						inputParams.stream().forEach(param -> inputMap.put(param.getKey(), param.getValue()));
					}
					Map<String, Object> outputMap = Maps.newHashMap();
					outputParams.stream().forEach(outputParam -> {
						outputMap.put(outputParam.getKey(), inputMap.get(outputParam.getKey()));
					});
					nodeResult.setOutput(JsonUtils.toJson(outputMap));
				}
				else {
					nodeResult.setOutput(null);
				}
				nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
				wfContext.getNodeResultMap().put(nodeResult.getNodeId(), nodeResult);
				wfContext.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
				workflowInnerService.forceRefreshContextCache(wfContext);
			}
			TaskResumeResponse response = new TaskResumeResponse();
			response.setTaskId(request.getTaskId());
			response.setRequestId(IdGenerator.uuid());
			return Result.success(response);
		}
		catch (Exception e) {
			log.error("WorkflowController resumeDebugTask error taskId:{}, costTime:{}", request.getTaskId(),
					System.currentTimeMillis() - start, e);
			return Result.error(ErrorCode.WORKFLOW_DEBUG_FAIL);
		}
	}

	/**
	 * Executes a partial workflow graph for testing. This endpoint allows running a
	 * subset of the workflow graph, useful for testing specific workflow components.
	 * @param request Partial graph execution request containing: - nodes: List of nodes
	 * to execute - edges: List of edges connecting the nodes - inputParams: Input
	 * parameters for the execution - appId: Application identifier
	 * @return TaskPartGraphResponse containing: - taskId: Task identifier - requestId:
	 * Request tracking identifier
	 */
	@PostMapping(value = { "/workflow/debug/part-graph/run-task" })
	public Result<TaskPartGraphResponse> partGraphRunTask(@RequestBody TaskPartGraphRequest request) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (request == null || CollectionUtils.isEmpty(request.getNodes())) {
			return Result.error(requestContext.getRequestId(), ErrorCode.MISSING_PARAMS.toError("nodes is null"));
		}

		List<Edge> edges = request.getEdges() == null ? Lists.newArrayList() : request.getEdges();
		WorkflowConfig workflowConfig = workflowExecuteManager.constructDebugConfig4Fragment(request.getNodes(), edges);
		Map<String, Object> inputMap = Maps.newHashMap();
		if (CollectionUtils.isNotEmpty(request.getInputParams())) {
			request.getInputParams().stream().forEach(inputParam -> {
				inputMap.put(inputParam.getKey(), VariableUtils.convertValueByType(inputParam.getKey(),
						inputParam.getType(), inputParam.getValue()));
			});
		}
		Map<String, Object> variableMap = workflowExecuteManager.constructDebugVariables(inputMap);
		WorkflowContext context = new WorkflowContext();
		context.setWorkflowConfig(workflowConfig);
		context.getVariablesMap().putAll(variableMap);
		context.setAppId(request.getAppId());
		context.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
		context.setRequestId(requestContext.getRequestId());
		context.setInvokeSource(InvokeSourceEnum.console.getCode());
		context.setWorkspaceId(requestContext.getWorkspaceId());
		context.setConversationId(IdGenerator.uuid());
		String taskId = workflowExecuteManager.execute(workflowConfig, context);
		TaskPartGraphResponse response = new TaskPartGraphResponse();
		response.setTaskId(taskId);
		response.setRequestId(requestContext.getRequestId());
		return Result.success(response);
	}

	@PostMapping(value = { "/workflow/debug/part-graph/stop-task" })
	public Result<Boolean> stopTask(@RequestBody TaskStopRequest request) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (request == null || StringUtils.isBlank(request.getTaskId())) {
			return Result.error(requestContext.getRequestId(), ErrorCode.MISSING_PARAMS.toError("taskId is null"));
		}
		return Result.success(workflowExecuteManager.stopTask(request.getTaskId()));
	}

	/**
	 * Streams workflow execution events using Server-Sent Events (SSE). This endpoint
	 * provides real-time updates about workflow execution progress, including node status
	 * changes and execution results.
	 * @param appId Application identifier
	 * @param request Task run request containing: - inputs: Input parameters for the
	 * workflow - conversationId: Session identifier for tracking
	 * @return SseEmitter for streaming events including: - Message events: Node execution
	 * updates - Error events: Execution errors - Pause events: Task pause notifications -
	 * Finish events: Task completion notifications
	 */
	@PostMapping(value = "/workflow/{appId}/run_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamEvents(@PathVariable("appId") String appId, @RequestBody ApiTaskRunRequest request) {
		SseEmitter emitter = new SseEmitter(0L);

		// 设置超时和完成回调
		emitter.onTimeout(() -> {
			ApiTaskMsg timeoutMsg = new ApiTaskMsg();
			timeoutMsg.setEvent(ApiTaskMsg.Event.Error.name());
			timeoutMsg.setError_code("TIMEOUT");
			timeoutMsg.setError_message("Stream connection timeout");
			try {
				emitter.send(timeoutMsg);
				emitter.complete();
			}
			catch (IOException e) {
				log.error("Send timeout message error", e);
			}
		});

		emitter.onCompletion(() -> {
			log.info("SSE connection completed for task: {}", request.getConversationId());
		});

		ThreadPoolUtils.DEFAULT_TASK_EXECUTOR.execute(() -> {
			innerRunStream(emitter, appId, request);
		});

		return emitter;
	}

	private void innerRunStream(SseEmitter emitter, String appId, ApiTaskRunRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		long start = System.currentTimeMillis();
		String taskId = null;
		String conversationId = null;

		try {
			// Parameter validation and initialization
			validateAndInitParams(request, appId);
			String version = "lastPublished";
			WorkflowContext workflowContext = new WorkflowContext();
			workflowContext.setInvokeSource(InvokeSourceEnum.console.getCode());
			ApplicationVersion appVersion = appService.getAppVersion(appId, version);

			// Run task and get response
			TaskRunResponse response = workflowExecuteManager.runTask(appVersion, request.getInputs(),
					request.getConversationId(), workflowContext);
			taskId = response.getTaskId();
			conversationId = response.getConversationId();

			// Process task execution
			processTaskExecution(emitter, context, taskId, conversationId, start);

		}
		catch (Exception e) {
			log.error("WorkflowController streamEvents error requestId:{}, costTime:{}", context.getRequestId(),
					System.currentTimeMillis() - start, e);
			sendErrorMessage(emitter, taskId, conversationId, "INTERNAL_ERROR", e.getMessage());
		}
		finally {
			try {
				emitter.complete();
			}
			catch (Exception e) {
				log.error("Error completing emitter", e);
			}
		}
	}

	private void validateAndInitParams(ApiTaskRunRequest request, String appId) {
		if (request == null || StringUtils.isBlank(appId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("request or appId is null"));
		}
	}

	private void processTaskExecution(SseEmitter emitter, RequestContext context, String taskId, String conversationId,
			long startTime) throws Exception {
		List<NodeResult> lastNodeResults = Lists.newArrayList();
		Map<String, AtomicInteger> recmsgSeqIdMap = Maps.newHashMap();
		while (true) {
			// 检查超时
			if (System.currentTimeMillis() - startTime > InvokeSourceEnum.api.getTimeoutSeconds() * 1000) {
				sendTimeoutMessage(emitter, taskId, conversationId);
				break;
			}

			// 获取最新上下文
			WorkflowContext latestContext = getLatestContext(context, taskId);
			if (latestContext == null) {
				throw new BizException(ErrorCode.WORKFLOW_NODE_DEBUG_FAIL.toError());
			}

			List<NodeResult> currentNodeResults = Lists.newArrayList();
			latestContext.getExecuteOrderList().stream().forEach(nodeId -> {
				NodeResult currentNodeResult = latestContext.getNodeResultMap().get(nodeId);
				if ((NodeTypeEnum.OUTPUT.getCode().equals(currentNodeResult.getNodeType())
						|| NodeTypeEnum.END.getCode().equals(currentNodeResult.getNodeType())
						|| NodeTypeEnum.INPUT.getCode().equals(currentNodeResult.getNodeType()))
						&& (NodeStatusEnum.EXECUTING.getCode().equals(currentNodeResult.getNodeStatus())
								|| NodeStatusEnum.SUCCESS.getCode().equals(currentNodeResult.getNodeStatus())
								|| NodeStatusEnum.PAUSE.getCode().equals(currentNodeResult.getNodeStatus()))) {
					currentNodeResults.add(currentNodeResult);
				}
			});
			boolean handleResult = handleNodeMessage(emitter, taskId, conversationId, recmsgSeqIdMap, lastNodeResults,
					currentNodeResults);
			if (handleResult) {
				lastNodeResults = currentNodeResults;
			}

			// 处理任务状态
			if (handleTaskStatus(emitter, latestContext, taskId, conversationId)) {
				break;
			}
		}
	}

	private boolean handleNodeMessage(SseEmitter emitter, String taskId, String conversationId,
			Map<String, AtomicInteger> recmsgSeqIdMap, List<NodeResult> lastNodeResults,
			List<NodeResult> thisNodeResult) throws Exception {
		if (CollectionUtils.isEmpty(thisNodeResult)) {
			return false;
		}
		boolean diff = false;
		if (CollectionUtils.isEmpty(lastNodeResults)) {
			for (NodeResult nodeResult : thisNodeResult) {
				String incrementalContent = calculateIncrementalContent(nodeResult.getOutput(), "");
				AtomicInteger atomicInteger = recmsgSeqIdMap.get(nodeResult.getNodeId());
				if (atomicInteger == null) {
					atomicInteger = new AtomicInteger(0);
				}
				boolean nodeCompleted = nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode()) ? true
						: false;
				if (StringUtils.isNotBlank(incrementalContent) || nodeCompleted) {
					sendNodeMessage(emitter, nodeResult, taskId, conversationId, atomicInteger.incrementAndGet(),
							incrementalContent);
					diff = true;
				}
				recmsgSeqIdMap.put(nodeResult.getNodeId(), atomicInteger);
			}
		}
		else {
			Map<String, List<NodeResult>> listMap = lastNodeResults.stream()
				.collect(Collectors.groupingBy(NodeResult::getNodeId));
			for (NodeResult nodeResult : thisNodeResult) {
				List<NodeResult> lastNodeResultList = listMap.get(nodeResult.getNodeId());
				boolean nodeCompleted = (!lastNodeResultList.get(0)
					.getNodeStatus()
					.equals(NodeStatusEnum.SUCCESS.getCode())
						&& nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) ? true : false;
				String incrementalContent;
				if (CollectionUtils.isEmpty(lastNodeResultList)) {
					incrementalContent = calculateIncrementalContent(nodeResult.getOutput(), "");
				}
				else {
					incrementalContent = calculateIncrementalContent(nodeResult.getOutput(),
							lastNodeResultList.get(0).getOutput());
				}

				AtomicInteger atomicInteger = recmsgSeqIdMap.get(nodeResult.getNodeId());
				if (atomicInteger == null) {
					atomicInteger = new AtomicInteger(0);
				}
				if (StringUtils.isNotBlank(incrementalContent) || nodeCompleted) {
					sendNodeMessage(emitter, nodeResult, taskId, conversationId, atomicInteger.incrementAndGet(),
							incrementalContent);
					diff = true;
				}
				recmsgSeqIdMap.put(nodeResult.getNodeId(), atomicInteger);
			}
		}
		return diff;
	}

	private WorkflowContext getLatestContext(RequestContext context, String taskId) {
		return redisManager.get(WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + taskId);
	}

	private boolean handleTaskStatus(SseEmitter emitter, WorkflowContext context, String taskId, String conversationId)
			throws IOException {
		String taskStatus = context.getTaskStatus();

		if (NodeStatusEnum.FAIL.getCode().equals(taskStatus) || NodeStatusEnum.STOP.getCode().equals(taskStatus)) {
			sendErrorMessage(emitter, taskId, conversationId, context.getErrorCode(), context.getErrorInfo());
			return true;
		}

		if (NodeStatusEnum.PAUSE.getCode().equals(taskStatus)) {
			sendPauseMessage(emitter, context, taskId, conversationId);
			return true;
		}

		if (NodeStatusEnum.SUCCESS.getCode().equals(taskStatus)) {
			sendFinishMessage(emitter, taskId, conversationId);
			return true;
		}

		return false;
	}

	private String calculateIncrementalContent(String currentOutput, String lastOutput) {
		if (lastOutput != null && !lastOutput.isEmpty() && currentOutput.startsWith(lastOutput)) {
			return currentOutput.substring(lastOutput.length());
		}
		return currentOutput;
	}

	private void sendNodeMessage(SseEmitter emitter, NodeResult nodeResult, String taskId, String conversationId,
			int msgSeqId, String content) throws IOException {
		ApiTaskMsg message = new ApiTaskMsg();
		message.setEvent(ApiTaskMsg.Event.Message.name());
		message.setTaskId(taskId);
		message.setConversationId(conversationId);
		message.setNodeId(nodeResult.getNodeId());
		message.setNodeName(nodeResult.getNodeName());
		message.setNodeType(nodeResult.getNodeType());
		message.setNodeStatus(nodeResult.getNodeStatus());
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			message.setNodeIsCompleted(true);
		}
		else {
			message.setNodeIsCompleted(false);
		}
		message.setNodeMsgSeqId(msgSeqId);
		message.setTextContent(content);
		emitter.send(message);
	}

	private void sendErrorMessage(SseEmitter emitter, String taskId, String conversationId, String errorCode,
			String errorMessage) {
		try {
			ApiTaskMsg errorMsg = new ApiTaskMsg();
			errorMsg.setEvent(ApiTaskMsg.Event.Error.name());
			errorMsg.setError_code(errorCode);
			errorMsg.setError_message(errorMessage);
			errorMsg.setTaskId(taskId);
			errorMsg.setConversationId(conversationId);
			emitter.send(errorMsg);
		}
		catch (IOException e) {
			log.error("Error sending error message", e);
		}
	}

	private void sendTimeoutMessage(SseEmitter emitter, String taskId, String conversationId) {
		sendErrorMessage(emitter, taskId, conversationId, "TIMEOUT", "Task execution timeout");
	}

	private void sendPauseMessage(SseEmitter emitter, WorkflowContext context, String taskId, String conversationId)
			throws IOException {
		ApiTaskMsg pauseMsg = new ApiTaskMsg();
		pauseMsg.setEvent(ApiTaskMsg.Event.Paused.name());
		pauseMsg.setTaskId(taskId);
		pauseMsg.setConversationId(conversationId);
		pauseMsg.setPauseType(ApiTaskMsg.PauseType.InputNodeInterrupt.name());

		Optional<NodeResult> pauseNodeResult = context.getNodeResultMap()
			.values()
			.stream()
			.filter(result -> NodeStatusEnum.PAUSE.getCode().equals(result.getNodeStatus()))
			.findFirst();

		if (pauseNodeResult.isPresent()) {
			NodeResult nodeResult = pauseNodeResult.get();
			Map<String, Object> pauseData = Maps.newHashMap();
			pauseData.put("node_id", nodeResult.getNodeId());
			pauseData.put("node_name", nodeResult.getNodeName());
			pauseData.put("node_type", nodeResult.getNodeType());
			pauseData.put("input_params", nodeResult.getInput());
			pauseMsg.setPause_data(pauseData);
		}
		emitter.send(pauseMsg);
	}

	private void sendFinishMessage(SseEmitter emitter, String taskId, String conversationId) throws IOException {
		ApiTaskMsg finishMsg = new ApiTaskMsg();
		finishMsg.setEvent(ApiTaskMsg.Event.Finished.name());
		finishMsg.setTaskId(taskId);
		finishMsg.setConversationId(conversationId);
		emitter.send(finishMsg);
	}

}
