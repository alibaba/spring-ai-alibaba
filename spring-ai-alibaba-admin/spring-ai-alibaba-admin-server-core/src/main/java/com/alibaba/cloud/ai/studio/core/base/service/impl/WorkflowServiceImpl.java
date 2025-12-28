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

import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.InvokeSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ParamSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.WorkflowStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskStopRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.base.service.WorkflowService;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.runtime.WorkflowExecuteManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_TASK_CONTEXT_PREFIX;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.SYS_HISTORY_LIST_KEY;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Title workspace service.<br>
 * Description workspace service.<br>
 *
 * @since 1.0.0.3
 */

@Slf4j
@Service
public class WorkflowServiceImpl implements WorkflowService {

	@Resource
	private WorkflowExecuteManager workflowExecuteManager;

	@Resource
	private RedisManager redisManager;

	@Resource
	private AppService appService;

	@Resource
	private CommonConfig commonConfig;

	@Override
	public WorkflowResponse call(WorkflowRequest request) {
		List<WorkflowResponse> allResponses = streamCall(Flux.just(request)).collectList().block();

		Optional<WorkflowResponse> any = allResponses.stream()
			.filter(workflowResponse -> workflowResponse.getStatus().equals(WorkflowStatus.FAILED)
					|| workflowResponse.getStatus().equals(WorkflowStatus.PAUSE))
			.findAny();
		if (any.isPresent()) {
			return any.get();
		}

		// 筛选出nodeType为End的响应
		List<WorkflowResponse> endNodeResponses = allResponses.stream()
			.filter(resp -> NodeTypeEnum.END.getCode().equals(resp.getNodeType()))
			.sorted((r1, r2) -> Integer.compare(r1.getNodeMsgSeqId(), r2.getNodeMsgSeqId()))
			.collect(Collectors.toList());

		// 拼接已完成节点的内容
		StringBuilder contentBuilder = new StringBuilder();
		for (WorkflowResponse resp : endNodeResponses) {
			if (resp.getMessage() != null) {
				contentBuilder.append(resp.getMessage().getContent());
			}
		}

		// 构建最终响应
		WorkflowResponse finalResponse = allResponses.get(allResponses.size() - 1);
		finalResponse.setMessage(new ChatMessage(MessageRole.ASSISTANT, contentBuilder.toString()));
		return finalResponse;
	}

	@Override
	public Flux<WorkflowResponse> streamCall(Flux<WorkflowRequest> requestFlux) {

		RequestContext requestContext = RequestContextHolder.getRequestContext();
		WorkflowContext context = BeanCopierUtils.copy(requestContext, WorkflowContext.class);
		context.setStream(true);

		return requestFlux.doOnNext(request -> checkAndInitContext(context, request))
			// request model
			.flatMap(request -> streamExecute(context, request))
			// read timeout
			.timeout(Duration.ofSeconds(InvokeSourceEnum.api.getTimeoutSeconds()))
			// handle error
			.onErrorResume(err -> handleThrowable(context, err))
			// post handle
			.doOnNext(response -> postHandle(context, response))
			// final call
			.doFinally(signal -> statistics(context, signal));
	}

	@Override
	public TaskRunResponse asyncCall(WorkflowRequest request) {
		ApplicationVersion appVersion = appService.getAppVersion(request.getAppId(), "lastPublished");
		WorkflowContext workflowContext = new WorkflowContext();
		workflowContext.setInvokeSource(InvokeSourceEnum.async.getCode());
		return workflowExecuteManager.runTask(appVersion, request.getInputParams(), request.getConversationId(),
				workflowContext);
	}

	@Override
	public Boolean stop(TaskStopRequest request) {
		return workflowExecuteManager.stopTask(request.getTaskId());
	}

	private void statistics(WorkflowContext context, SignalType signalType) {
		long firstResponseTime = context.getFirstResponseTime();
		long startTime = context.getStartTime();
		long endTime = context.getEndTime();

		LogUtils.monitor(context, "WorkflowService", "firstResponse", context.getStartTime(), "", null,
				firstResponseTime - startTime);

		if (SignalType.CANCEL == signalType) {
			if (endTime <= 0) {
				endTime = System.currentTimeMillis();
			}

			LogUtils.monitor(context, "WorkflowService", "handleRequests", context.getStartTime(), "cancel",
					"request cancelled", endTime - startTime);
		}
		else {
			if (context.getError() == null) {
				LogUtils.monitor(context, "WorkflowService", "handleRequests", context.getStartTime(), SUCCESS, null,
						context.getTaskResult());
				return;
			}

			LogUtils.monitor(context, "WorkflowService", "handleRequests", context.getStartTime(), FAIL, null,
					context.getError());
		}

		LogUtils.statistics(context, context.getError() == null);
	}

	private void postHandle(WorkflowContext context, WorkflowResponse response) {
		response.setRequestId(context.getRequestId());
		response.setConversationId(context.getConversationId());
		// context.setTaskResult(response.getMessage().getContent());
		context.setEndTime(System.currentTimeMillis());
	}

	private Mono<WorkflowResponse> handleThrowable(WorkflowContext context, Throwable err) {
		WorkflowResponse response = new WorkflowResponse();
		Error error;
		if (err instanceof BizException be) {
			error = be.getError();
		}
		else if (err instanceof TimeoutException) {
			error = ErrorCode.WORKFLOW_EXECUTION_TIMEOUT.toError();
		}
		else {
			error = ErrorCode.WORKFLOW_EXECUTE_ERROR.toError(err.getMessage());
		}
		response.setError(error);
		LogUtils.monitor(context, "WorkflowService", "handleThrowable", context.getStartTime(), error.getCode(), null,
				response, err);
		return Mono.just(response);
	}

	private void checkAndInitContext(WorkflowContext workflowContext, WorkflowRequest request) {
		Long start = System.currentTimeMillis();
		RequestContext context = RequestContextHolder.getRequestContext();
		try {
			// check input params
			if (Objects.isNull(request)) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("request"));
			}

			String uid = context.getAccountId();
			if (Objects.isNull(uid)) {
				throw new BizException(ErrorCode.UNAUTHORIZED.toError());
			}

			String appId = request.getAppId();
			if (StringUtils.isBlank(appId)) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("appId"));
			}

			// if (CollectionUtils.isEmpty(request.getMessages())) {
			// throw new BizException(ErrorCode.MISSING_PARAMS.toError("messages"));
			// }

			if (Objects.isNull(context.getWorkspaceId())) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
			}

			// get app config
			Application app = appService.getApp(appId);
			if (app == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}

			String configStr;
			if (BooleanUtils.isTrue(request.getDraft())) {
				configStr = app.getConfigStr();
				if (configStr == null) {
					throw new BizException(ErrorCode.APP_CONFIG_NOT_FOUND.toError());
				}
			}
			else {
				configStr = app.getPubConfigStr();
				if (configStr == null) {
					throw new BizException(ErrorCode.APP_NOT_PUBLISHED.toError());
				}
			}

			workflowContext.setAppId(appId);

			if (!CollectionUtils.isEmpty(request.getInputParams())) {
				request.getInputParams().stream().forEach(input -> {
					String key = input.getKey();
					String source = input.getSource();
					if (ParamSourceEnum.sys.name().equals(source)) {
						workflowContext.getSysMap()
							.put(key, VariableUtils.convertValueByType(input.getKey(), input.getType(),
									input.getValue()));
					}
					else {
						workflowContext.getUserMap()
							.put(key, VariableUtils.convertValueByType(input.getKey(), input.getType(),
									input.getValue()));
					}
				});
			}

			// 处理messages作为上下文
			if (!CollectionUtils.isEmpty(request.getMessages())) {
				workflowContext.getSysMap().put(SYS_HISTORY_LIST_KEY, request.getMessages());
			}

			String conversationId = request.getConversationId() == null ? IdGenerator.uuid()
					: request.getConversationId();
			workflowContext.setWorkflowConfig(JsonUtils.fromJson(configStr, WorkflowConfig.class));
			workflowContext.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
			workflowContext.setRequestId(context.getRequestId());
			workflowContext.setWorkspaceId(context.getWorkspaceId());
			workflowContext.setConversationId(conversationId);
			workflowContext.setInvokeSource(InvokeSourceEnum.api.getCode());
			LogUtils.monitor(context, "WorkflowService", "check", start, SUCCESS, request, null);
		}
		catch (BizException e) {
			LogUtils.monitor(context, "WorkflowService", "check", start, FAIL, request, e.getError(), e);
			throw e;
		}
	}

	private Flux<WorkflowResponse> streamExecute(WorkflowContext workflowContext, WorkflowRequest request) {
		String taskId = workflowExecuteManager.execute(workflowContext);
		String requestId = request.getRequestId();
		String conversationId = request.getConversationId();
		Sinks.Many<WorkflowResponse> sink = Sinks.many().unicast().onBackpressureBuffer();
		ThreadPoolUtils.DEFAULT_TASK_EXECUTOR.execute(() -> {
			List<NodeResult> lastNodeResults = Lists.newArrayList();
			Map<String, AtomicInteger> recmsgSeqIdMap = Maps.newHashMap();
			boolean firstResponse = true;
			boolean isCompleted = false;
			while (true) {
				if (isCompleted) {
					handleCompletedMsg(sink, workflowContext, requestId, taskId, conversationId);
					break;
				}
				String taskStatus = workflowContext.getTaskStatus();
				isCompleted = NodeStatusEnum.FAIL.getCode().equals(taskStatus)
						|| NodeStatusEnum.PAUSE.getCode().equals(taskStatus)
						|| NodeStatusEnum.SUCCESS.getCode().equals(taskStatus);
				List<NodeResult> currentNodeResults = Lists.newArrayList();
				workflowContext.getExecuteOrderList().forEach(nodeId -> {
					NodeResult currentNodeResult = workflowContext.getNodeResultMap().get(nodeId);
					if ((NodeTypeEnum.OUTPUT.getCode().equals(currentNodeResult.getNodeType())
							|| NodeTypeEnum.END.getCode().equals(currentNodeResult.getNodeType())
							|| NodeTypeEnum.INPUT.getCode().equals(currentNodeResult.getNodeType()))
							&& (NodeStatusEnum.EXECUTING.getCode().equals(currentNodeResult.getNodeStatus())
									|| NodeStatusEnum.SUCCESS.getCode().equals(currentNodeResult.getNodeStatus())
									|| NodeStatusEnum.PAUSE.getCode().equals(currentNodeResult.getNodeStatus()))) {
						NodeResult copyNodeResult = BeanCopierUtils.copy(currentNodeResult, NodeResult.class);
						currentNodeResults.add(copyNodeResult);
					}
				});
				boolean handleResult = handleNodeMessage(sink, requestId, taskId, conversationId, recmsgSeqIdMap,
						lastNodeResults, currentNodeResults);
				if (handleResult) {
					if (firstResponse) {
						firstResponse = false;
						workflowContext.setFirstResponseTime(System.currentTimeMillis());
					}
					lastNodeResults = currentNodeResults;
				}
				else {
					try {
						Thread.sleep(50);
					}
					catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		return sink.asFlux();
	}

	private boolean handleNodeMessage(Sinks.Many<WorkflowResponse> sink, String requestId, String taskId,
			String conversationId, Map<String, AtomicInteger> recmsgSeqIdMap, List<NodeResult> lastNodeResults,
			List<NodeResult> thisNodeResult) {
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
					sendNodeMessage(sink, nodeResult, requestId, taskId, conversationId,
							atomicInteger.incrementAndGet(), incrementalContent);
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
				boolean nodeCompleted = ((CollectionUtils.isEmpty(lastNodeResultList)
						|| !lastNodeResultList.get(0).getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode()))
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
					sendNodeMessage(sink, nodeResult, requestId, taskId, conversationId,
							atomicInteger.incrementAndGet(), incrementalContent);
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

	private void handleCompletedMsg(Sinks.Many<WorkflowResponse> sink, WorkflowContext context, String requestId,
			String taskId, String conversationId) {
		String taskStatus = context.getTaskStatus();

		if (NodeStatusEnum.FAIL.getCode().equals(taskStatus)) {
			sendErrorMessage(sink, requestId, taskId, conversationId, context.getError());
		}

		if (NodeStatusEnum.PAUSE.getCode().equals(taskStatus)) {
			sendPauseMessage(sink, context, requestId, taskId, conversationId);
		}

		if (NodeStatusEnum.SUCCESS.getCode().equals(taskStatus)) {
			sendFinishMessage(sink, requestId, taskId, conversationId);
		}
	}

	private String calculateIncrementalContent(String currentOutput, String lastOutput) {
		if (lastOutput != null && !lastOutput.isEmpty() && currentOutput.startsWith(lastOutput)) {
			return currentOutput.substring(lastOutput.length());
		}
		return currentOutput;
	}

	private void sendNodeMessage(Sinks.Many<WorkflowResponse> sink, NodeResult nodeResult, String requestId,
			String taskId, String conversationId, int msgSeqId, String content) {
		WorkflowResponse response = new WorkflowResponse();
		response.setRequestId(requestId);
		response.setTaskId(taskId);
		response.setConversationId(conversationId);
		ChatMessage message = new ChatMessage(MessageRole.ASSISTANT, content);
		response.setMessage(message);
		response.setNodeId(nodeResult.getNodeId());
		response.setNodeName(nodeResult.getNodeName());
		response.setNodeType(nodeResult.getNodeType());
		response.setNodeStatus(nodeResult.getNodeStatus());
		response.setStatus(WorkflowStatus.IN_PROGRESS);
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			response.setNodeIsCompleted(true);
		}
		else {
			response.setNodeIsCompleted(false);
		}
		response.setNodeMsgSeqId(msgSeqId);
		sink.tryEmitNext(response);
	}

	private void sendErrorMessage(Sinks.Many<WorkflowResponse> sink, String requestId, String taskId,
			String conversationId, Error error) {
		WorkflowResponse response = new WorkflowResponse();
		response.setRequestId(requestId);
		response.setTaskId(taskId);
		response.setConversationId(conversationId);
		response.setError(error);
		response.setStatus(WorkflowStatus.FAILED);
		sink.tryEmitNext(response);
		sink.tryEmitComplete();
	}

	private void sendPauseMessage(Sinks.Many<WorkflowResponse> sink, WorkflowContext context, String requestId,
			String taskId, String conversationId) {
		Optional<NodeResult> pauseNodeResult = context.getNodeResultMap()
			.values()
			.stream()
			.filter(result -> NodeStatusEnum.PAUSE.getCode().equals(result.getNodeStatus()))
			.findFirst();

		WorkflowResponse response = new WorkflowResponse();
		response.setRequestId(requestId);
		response.setTaskId(taskId);
		response.setConversationId(conversationId);
		response.setStatus(WorkflowStatus.PAUSE);
		if (pauseNodeResult.isPresent()) {
			NodeResult nodeResult = pauseNodeResult.get();
			Map<String, Object> pauseData = Maps.newHashMap();
			pauseData.put("node_id", nodeResult.getNodeId());
			pauseData.put("node_name", nodeResult.getNodeName());
			pauseData.put("node_type", nodeResult.getNodeType());
			pauseData.put("input_params", nodeResult.getInput());
			ChatMessage message = new ChatMessage(MessageRole.ASSISTANT, JsonUtils.toJson(pauseData));
			response.setMessage(message);
			sink.tryEmitNext(response);
			sink.tryEmitComplete();
		}
	}

	private void sendFinishMessage(Sinks.Many<WorkflowResponse> sink, String requestId, String taskId,
			String conversationId) {
		WorkflowResponse response = new WorkflowResponse();
		response.setRequestId(requestId);
		response.setTaskId(taskId);
		response.setConversationId(conversationId);
		response.setStatus(WorkflowStatus.COMPLETED);
		sink.tryEmitNext(response);
		sink.tryEmitComplete();
	}

}
