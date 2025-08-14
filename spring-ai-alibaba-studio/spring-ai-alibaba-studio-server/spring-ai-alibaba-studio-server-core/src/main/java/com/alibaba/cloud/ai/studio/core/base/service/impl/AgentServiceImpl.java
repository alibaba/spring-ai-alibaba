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
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.Application;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AgentService;
import com.alibaba.cloud.ai.studio.core.base.service.AppService;
import com.alibaba.cloud.ai.studio.core.agent.AgentExecutor;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.agent.AgentContext;
import com.alibaba.cloud.ai.studio.core.utils.ErrorHandlerUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Implementation of agent service for handling agent inference requests. Supports both
 * basic and workflow agent types with synchronous and streaming responses.
 *
 * @since 1.0.0.3
 */
@Service
public class AgentServiceImpl implements AgentService {

	/** Service for managing application configurations */
	private final AppService appService;

	/** Common configuration settings */
	private final CommonConfig commonConfig;

	/** Executor for basic agent type */
	private final AgentExecutor basicAgentExecutor;

	/** Executor for workflow agent type */
	private final AgentExecutor workflowAgentExecutor;

	public AgentServiceImpl(AppService appService, CommonConfig commonConfig,
			@Qualifier("basicAgentExecutor") AgentExecutor basicAgentExecutor,
			@Qualifier("workflowAgentExecutor") AgentExecutor workflowAgentExecutor) {
		this.appService = appService;
		this.commonConfig = commonConfig;
		this.workflowAgentExecutor = workflowAgentExecutor;
		this.basicAgentExecutor = basicAgentExecutor;
	}

	/**
	 * Synchronously processes an agent request and returns the response
	 */
	@Override
	public AgentResponse call(AgentRequest request) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		AgentContext context = BeanCopierUtils.copy(requestContext, AgentContext.class);

		checkAndInitContext(context, request);
		AgentResponse response;
		try {
			response = execute(context, request);
		}
		catch (Exception e) {
			response = handleThrowable(context, e).block();
		}

		postHandle(context, response);
		statistics(context, SignalType.ON_COMPLETE);
		return response;
	}

	/**
	 * Processes agent requests in a streaming manner
	 */
	@Override
	public Flux<AgentResponse> streamCall(Flux<AgentRequest> requestFlux) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		AgentContext context = BeanCopierUtils.copy(requestContext, AgentContext.class);
		context.setStream(true);

		return requestFlux.doOnNext(request -> checkAndInitContext(context, request))
			// request model
			.flatMap(request -> streamExecute(context, request))
			// read timeout
			.timeout(Duration.ofSeconds(commonConfig.getAgentReadTimeout()))
			// handle error
			.onErrorResume(err -> handleThrowable(context, err))
			// post handle
			.doOnNext(response -> postHandle(context, response))
			// final call
			.doFinally(signal -> statistics(context, signal));
	}

	/**
	 * Validates and initializes the agent context with request parameters
	 */
	private void checkAndInitContext(AgentContext context, AgentRequest request) {
		Long start = System.currentTimeMillis();
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

			if (CollectionUtils.isEmpty(request.getMessages())) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("messages"));
			}

			if (Objects.isNull(context.getWorkspaceId())) {
				throw new BizException(ErrorCode.MISSING_PARAMS.toError("workspace_id"));
			}

			// get app config
			Application app = appService.getApp(appId);
			if (app == null) {
				throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
			}

			String configStr;
			if (request.isDraft()) {
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

			context.setAppType(app.getType());
			context.setConfig(JsonUtils.fromJson(configStr, AgentConfig.class));

			boolean memoryEnabled = memoryEnabled(context, request);
			context.setMemoryEnabled(memoryEnabled);

			if (StringUtils.isBlank(request.getConversationId())) {
				request.setConversationId(IdGenerator.idStr());
			}

			context.setAppId(appId);
			context.setConversationId(request.getConversationId());
			context.setRequest(request);
			context.setSource(context.getSource());
			LogUtils.monitor(context, "AgentService", "check", start, SUCCESS, request, null);
		}
		catch (BizException e) {
			LogUtils.monitor(context, "AgentService", "check", start, FAIL, request, e.getError(), e);
			throw e;
		}
	}

	/**
	 * Executes the agent request in streaming mode based on app type
	 */
	private Flux<AgentResponse> streamExecute(AgentContext context, AgentRequest request) {
		if (context.getAppType() == AppType.BASIC) {
			return basicAgentExecutor.streamExecute(context, request);
		}
		else {
			throw new BizException(ErrorCode.APP_TYPE_NOT_SUPPORT.toError(context.getAppType().getValue()));
		}
	}

	/**
	 * Executes the agent request based on app type
	 */
	private AgentResponse execute(AgentContext context, AgentRequest request) {
		if (context.getAppType() == AppType.BASIC) {
			return basicAgentExecutor.execute(context, request);
		}
		else if (context.getAppType() == AppType.WORKFLOW) {
			return workflowAgentExecutor.execute(context, request);
		}
		else {
			throw new BizException(ErrorCode.APP_TYPE_NOT_SUPPORT.toError(context.getAppType().getValue()));
		}
	}

	/**
	 * Handles exceptions and converts them to appropriate error responses
	 */
	private Mono<AgentResponse> handleThrowable(AgentContext context, Throwable err) {
		AgentResponse response = new AgentResponse();

		Error error;
		if (err instanceof BizException be) {
			error = be.getError();
			response.setError(error);
		}
		else if (err instanceof TimeoutException) {
			error = ErrorCode.REQUEST_TIMEOUT.toError();
		}
		else if (err instanceof WebClientResponseException wre) {
			error = ErrorHandlerUtils.parseOpenAiError(wre.getStatusCode().value(), wre.getResponseBodyAsString());
		}
		else {
			error = ErrorCode.AGENT_CALL_ERROR.toError();
		}

		response.setError(error);
		LogUtils.monitor(context, "AgentService", "handleThrowable", context.getStartTime(), error.getCode(), null,
				response, err);

		return Mono.just(response);
	}

	/**
	 * Performs post-processing on the agent response
	 */
	private void postHandle(AgentContext context, AgentResponse response) {
		response.setRequestId(context.getRequestId());
		response.setConversationId(context.getConversationId());
		context.setResponse(response);
		context.setEndTime(System.currentTimeMillis());
	}

	/**
	 * Records statistics and monitoring information for the request
	 */
	private void statistics(AgentContext context, SignalType signalType) {
		long firstResponseTime = context.getFirstResponseTime();
		long startTime = context.getStartTime();
		long endTime = context.getEndTime();

		LogUtils.monitor(context, "AgentService", "firstResponse", context.getStartTime(), "", null,
				firstResponseTime - startTime);

		AgentResponse response = context.getResponse();
		if (SignalType.CANCEL == signalType) {
			if (endTime <= 0) {
				endTime = System.currentTimeMillis();
			}

			LogUtils.monitor(context, "AgentService", "handleRequests", context.getStartTime(), "cancel",
					"request cancelled", endTime - startTime);
		}
		else {
			if (response.getError() == null) {
				LogUtils.monitor(context, "AgentService", "handleRequests", context.getStartTime(), SUCCESS, null,
						response);
				return;
			}

			LogUtils.monitor(context, "AgentService", "handleRequests", context.getStartTime(), FAIL, null, response);
		}

		LogUtils.statistics(context, response.getError() == null);
	}

	/**
	 * Checks if memory feature is enabled for the agent
	 */
	protected boolean memoryEnabled(AgentContext context, AgentRequest request) {
		if (context.getAppType() != AppType.BASIC) {
			return false;
		}

		AgentConfig config = context.getConfig();
		return config.getMemory() != null && config.getMemory().getDialogRound() > 0
				&& StringUtils.isNotBlank(request.getConversationId());
	}

}
