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

package com.alibaba.cloud.ai.studio.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.AsyncResultRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.AsyncResultResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskStopRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.WorkflowResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.WorkflowStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.utils.ExceptionUtils;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AgentService;
import com.alibaba.cloud.ai.studio.core.base.service.WorkflowService;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_TASK_CONTEXT_PREFIX;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Controller for handling chat and workflow completions. Provides endpoints for both
 * streaming and non-streaming responses.
 *
 * @since 1.0.0.3
 */
@Slf4j
@RestController
@Tag(name = "chat")
@RequestMapping("/api/v1/apps")
public class ChatController {

	/** Service for handling agent-related operations */
	private final AgentService agentService;

	/** Service for handling workflow-related operations */
	private final WorkflowService workflowService;

	/** Redis manager for cache operations */
	private final RedisManager redisManager;

	public ChatController(AgentService agentService, WorkflowService workflowService, RedisManager redisManager) {
		this.agentService = agentService;
		this.workflowService = workflowService;
		this.redisManager = redisManager;
	}

	/**
	 * Handles chat completion requests. Supports both streaming and non-streaming
	 * responses.
	 * @param request The chat completion request
	 * @param response The HTTP servlet response
	 * @return The chat completion response or error message
	 */
	@PostMapping(value = { "/chat/completions" })
	public Object completion(@RequestBody AgentRequest request, HttpServletResponse response) {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		LogUtils.trace(context, "startCall", LogUtils.SUCCESS, start, request, null);

		if (request.getStream() != null && request.getStream()) {
			Flux<AgentResponse> responseFlux = agentService.streamCall(Flux.just(request));

			// in case nginx will buffer the response, we need to disable it
			response.addHeader("X-Accel-Buffering", "no");
			response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE);

			SseEmitter emitter = new SseEmitter(0L);
			responseFlux.doOnNext(data -> sendStreamingResponse(context, request, emitter, data, response))
				.onErrorResume(err -> handleError(context, request, err))
				.doFinally(type -> handleComplete(context, type, emitter))
				.subscribe();

			return emitter;
		}

		try {
			AgentResponse completion = agentService.call(request);
			String result = JsonUtils.toJson(completion);

			LogUtils.monitor(context, "ChatController", "endCall", context.getStartTime(), LogUtils.SUCCESS, request,
					result);
			return result;
		}
		catch (Exception e) {
			Error error = ExceptionUtils.convertError(e);
			response.setStatus(error.getStatusCode());

			String result = JsonUtils.toJson(error);
			LogUtils.monitor(context, "ChatController", "endCallError", context.getStartTime(), FAIL, request, result,
					e);

			return result;
		}
	}

	/**
	 * Handles workflow completion requests. Supports both streaming and non-streaming
	 * responses.
	 * @param request The workflow completion request
	 * @param response The HTTP servlet response
	 * @return The workflow completion response or error message
	 */
	@PostMapping(value = { "/workflow/completions" })
	public Object completion(@RequestBody WorkflowRequest request, HttpServletResponse response) {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		LogUtils.trace(context, "startCall", LogUtils.SUCCESS, start, request, null);

		if (request.getStream() != null && request.getStream()) {
			Flux<WorkflowResponse> responseFlux = workflowService.streamCall(Flux.just(request));

			// in case nginx will buffer the response, we need to disable it
			response.addHeader("X-Accel-Buffering", "no");
			response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE);

			SseEmitter emitter = new SseEmitter(0L);
			responseFlux.doOnNext(data -> sendStreamingResponse(context, request, emitter, data, response))
				.onErrorResume(err -> handleError(context, request, err))
				.doFinally(type -> handleComplete(context, type, emitter))
				.subscribe();

			return emitter;
		}

		try {
			WorkflowResponse completion = workflowService.call(request);
			String result = JsonUtils.toJson(completion);

			LogUtils.monitor(context, "ChatController", "endCall", context.getStartTime(), LogUtils.SUCCESS, request,
					result);
			return result;
		}
		catch (Exception e) {
			Error error = ExceptionUtils.convertError(e);
			response.setStatus(error.getStatusCode());

			String result = JsonUtils.toJson(error);
			LogUtils.monitor(context, "ChatController", "endCallError", context.getStartTime(), FAIL, request, result,
					e);

			return result;
		}
	}

	/**
	 * Sends streaming response for agent requests
	 * @param context The request context
	 * @param request The agent request
	 * @param emitter The SSE emitter
	 * @param completion The agent response
	 * @param response The HTTP servlet response
	 */
	private void sendStreamingResponse(RequestContext context, AgentRequest request, SseEmitter emitter,
			AgentResponse completion, HttpServletResponse response) {
		if (completion.getError() != null) {
			response.setStatus(completion.getError().getStatusCode());
		}

		String json = JsonUtils.toJson(completion);
		try {
			emitter.send(json, MediaType.TEXT_EVENT_STREAM);
		}
		catch (Exception e) {
			LogUtils.monitor(context, "ChatController", "endStreamCallError", context.getStartTime(), FAIL, request,
					e.getMessage(), e);
		}

		if (completion.getStatus() == AgentStatus.COMPLETED) {
			LogUtils.trace(context, "ChatController", "endStreamCall", context.getStartTime(), SUCCESS, request, json);
		}
	}

	/**
	 * Handles errors for agent streaming requests
	 * @param context The request context
	 * @param request The agent request
	 * @param err The error
	 * @return A mono containing the agent response
	 */
	private Mono<AgentResponse> handleError(RequestContext context, AgentRequest request, Throwable err) {
		LogUtils.monitor(context, "ChatController", "endStreamCallError", context.getStartTime(), FAIL, request,
				err.getMessage(), err);

		Error error = ExceptionUtils.convertError(err);
		AgentResponse completion = AgentResponse.builder().requestId(context.getRequestId()).error(error).build();

		return Mono.just(completion);
	}

	/**
	 * Sends streaming response for workflow requests
	 * @param context The request context
	 * @param request The workflow request
	 * @param emitter The SSE emitter
	 * @param completion The workflow response
	 * @param response The HTTP servlet response
	 */
	private void sendStreamingResponse(RequestContext context, WorkflowRequest request, SseEmitter emitter,
			WorkflowResponse completion, HttpServletResponse response) {
		if (completion.getError() != null) {
			response.setStatus(completion.getError().getStatusCode());
		}

		String json = JsonUtils.toJson(completion);
		try {
			emitter.send(json, MediaType.TEXT_EVENT_STREAM);
		}
		catch (Exception e) {
			LogUtils.monitor(context, "ChatController", "endStreamCallError", context.getStartTime(), FAIL, request,
					e.getMessage(), e);
		}

		if (completion.getStatus() == WorkflowStatus.COMPLETED) {
			LogUtils.trace(context, "ChatController", "endStreamCall", context.getStartTime(), SUCCESS, request, json);
		}
	}

	/**
	 * Handles errors for workflow streaming requests
	 * @param context The request context
	 * @param request The workflow request
	 * @param err The error
	 * @return A mono containing the workflow response
	 */
	private Mono<WorkflowResponse> handleError(RequestContext context, WorkflowRequest request, Throwable err) {
		LogUtils.monitor(context, "ChatController", "endStreamCallError", context.getStartTime(), FAIL, request,
				err.getMessage(), err);

		Error error = ExceptionUtils.convertError(err);
		WorkflowResponse completion = WorkflowResponse.builder().requestId(context.getRequestId()).error(error).build();

		return Mono.just(completion);
	}

	/**
	 * Handles completion of streaming requests
	 * @param context The request context
	 * @param signalType The signal type
	 * @param emitter The SSE emitter
	 */
	private void handleComplete(RequestContext context, SignalType signalType, SseEmitter emitter) {
		emitter.complete();
		LogUtils.monitor(context, "ChatController", "endStreamCall", context.getStartTime(), SUCCESS, null, null);
	}

	@PostMapping(value = { "/workflow/async-completions" })
	public Result<TaskRunResponse> asyncCompletion(@RequestBody WorkflowRequest request) {
		long start = System.currentTimeMillis();
		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		return Result.success(workflowService.asyncCall(request));
	}

	@PostMapping(value = { "/workflow/stop-completions" })
	public Result<Boolean> stopCompletion(@RequestBody TaskStopRequest request) {
		long start = System.currentTimeMillis();
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (request == null || StringUtils.isBlank(request.getTaskId())) {
			return Result.error(requestContext.getRequestId(), ErrorCode.MISSING_PARAMS.toError("taskId is null"));
		}
		return Result.success(workflowService.stop(request));
	}

	@PostMapping(value = { "/workflow/async-results" })
	public Result<AsyncResultResponse> getAsyncResults(@RequestBody AsyncResultRequest request) {
		long start = System.currentTimeMillis();
		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		try {
			if (request == null || request.getTaskId() == null) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("request or taskId is null"));
			}

			// 从Redis中获取工作流上下文
			String cacheKey = WORKFLOW_TASK_CONTEXT_PREFIX + context.getWorkspaceId() + "_" + request.getTaskId();
			WorkflowContext wfContext = redisManager.get(cacheKey);

			if (wfContext == null) {
				log.info("Async task not found: taskId={}, workspaceId={}, requestId={}", request.getTaskId(),
						context.getWorkspaceId(), context.getRequestId());
				return Result.error(context.getRequestId(),
						ErrorCode.WORKFLOW_CONFIG_INVALID.toError("taskId not exists"));
			}

			// 构建响应对象
			AsyncResultResponse response = new AsyncResultResponse();
			response.setTaskId(request.getTaskId());
			response.setRequestId(wfContext.getRequestId());
			response.setConversationId(wfContext.getConversationId());
			response.setTaskStatus(wfContext.getTaskStatus());
			response.setErrorCode(wfContext.getErrorCode());
			response.setErrorInfo(wfContext.getErrorInfo());

			// 计算执行时间
			if (wfContext.getStartTime() > 0) {
				response.setTaskExecTime((System.currentTimeMillis() - wfContext.getStartTime()) + "ms");
			}
			// 获取输出节点和结束节点的内容进行输出
			CopyOnWriteArrayList<String> executeOrderList = wfContext.getExecuteOrderList();
			ConcurrentHashMap<String, NodeResult> nodeResultMap = wfContext.getNodeResultMap();
			List<AsyncResultResponse.Output> outputs = Lists.newArrayList();
			if (CollectionUtils.isNotEmpty(executeOrderList)) {
				executeOrderList.forEach(nodeId -> {
					if (nodeResultMap != null) {
						NodeResult nodeResult = nodeResultMap.get(nodeId);
						log.info("getAsyncResults nodeType:{}", nodeResult.getNodeType());
						if (!nodeResult.getNodeType().equals(NodeTypeEnum.OUTPUT.getCode())
								&& !nodeResult.getNodeType().equals(NodeTypeEnum.END.getCode())) {
							return;
						}

						AsyncResultResponse.Output output = new AsyncResultResponse.Output();
						output.setNodeId(nodeId);
						output.setNodeName(nodeResult.getNodeName());
						output.setNodeType(nodeResult.getNodeType());
						output.setNodeStatus(nodeResult.getNodeStatus());
						output.setContent(nodeResult.getOutput());
						outputs.add(output);
					}

				});
			}
			// 设置节点结果
			response.setOutputs(outputs);

			LogUtils.monitor(context, "ChatController", "getAsyncResults", context.getStartTime(), SUCCESS, request,
					"Task status: " + wfContext.getTaskStatus());

			return Result.success(response);
		}
		catch (BizException e) {
			LogUtils.monitor(context, "ChatController", "getAsyncResults", context.getStartTime(), FAIL, request,
					e.getError(), e);
			throw e;
		}
		catch (Exception e) {
			LogUtils.monitor(context, "ChatController", "getAsyncResults", context.getStartTime(), FAIL, request,
					e.getMessage(), e);
			return Result.error(ErrorCode.WORKFLOW_DEBUG_GET_PROCESS_FAIL);
		}
	}

}
