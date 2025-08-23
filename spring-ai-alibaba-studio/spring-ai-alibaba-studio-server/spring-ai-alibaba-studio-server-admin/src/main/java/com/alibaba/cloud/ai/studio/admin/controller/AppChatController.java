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

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentStatus;
import com.alibaba.cloud.ai.studio.runtime.utils.ExceptionUtils;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.AgentService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Controller for handling chat completions and streaming responses. Provides endpoints
 * for both synchronous and streaming chat interactions.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "app_chat")
@RequestMapping("/console/v1/apps")
public class AppChatController {

	/** Service for handling agent-related operations */
	private final AgentService agentService;

	public AppChatController(AgentService agentService) {
		this.agentService = agentService;
	}

	/**
	 * Handles chat completion requests, supporting both streaming and non-streaming
	 * responses. For streaming responses, uses Server-Sent Events (SSE) to deliver
	 * real-time updates.
	 * @param request The chat request containing user input and configuration
	 * @param response HTTP response object for setting headers and status
	 * @return Either a streaming response or a single completion response
	 */
	@PostMapping(value = { "/chat/completions" })
	public Object completion(@RequestBody AgentRequest request, HttpServletResponse response) {
		long start = System.currentTimeMillis();

		RequestContext context = RequestContextHolder.getRequestContext();
		context.setStartTime(System.currentTimeMillis());

		LogUtils.trace(context, "startCall", LogUtils.SUCCESS, start, request, null);
		request.setDraft(true);
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

			LogUtils.monitor(context, "AgentChatController", "endCall", context.getStartTime(), LogUtils.SUCCESS,
					request, result);
			return result;
		}
		catch (Exception e) {
			Error error = ExceptionUtils.convertError(e);
			response.setStatus(error.getStatusCode());

			String result = JsonUtils.toJson(error);
			LogUtils.monitor(context, "AgentChatController", "endCallError", context.getStartTime(), FAIL, request,
					result, e);

			return result;
		}
	}

	/**
	 * Sends a streaming response to the client using SSE. Handles error status codes and
	 * logs completion status.
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
			LogUtils.monitor(context, "AgentChatController", "endStreamCallError", context.getStartTime(), FAIL,
					request, e.getMessage(), e);
		}

		if (completion.getStatus() == AgentStatus.COMPLETED) {
			LogUtils.trace(context, "AgentChatController", "endStreamCall", context.getStartTime(), SUCCESS, request,
					json);
		}
	}

	/**
	 * Handles errors during streaming by converting them to appropriate error responses.
	 * Logs the error and returns a formatted error response.
	 */
	private Mono<AgentResponse> handleError(RequestContext context, AgentRequest request, Throwable err) {
		LogUtils.monitor(context, "AgentChatController", "endStreamCallError", context.getStartTime(), FAIL, request,
				err.getMessage(), err);

		Error error = ExceptionUtils.convertError(err);
		AgentResponse completion = AgentResponse.builder().requestId(context.getRequestId()).error(error).build();

		return Mono.just(completion);
	}

	/**
	 * Handles completion of the streaming response. Completes the SSE emitter and logs
	 * the completion status.
	 */
	private void handleComplete(RequestContext context, SignalType signalType, SseEmitter emitter) {
		emitter.complete();
		LogUtils.monitor(context, "AgentChatController", "endStreamCall", context.getStartTime(), SUCCESS, null, null);
	}

}
