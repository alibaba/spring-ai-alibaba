/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.mcp.discovery.tracing;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@Component
public class McpTraceExchangeFilterFunction implements ExchangeFilterFunction {

	private final Object tracer;

	public McpTraceExchangeFilterFunction(Object tracer) {
		this.tracer = tracer;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		// 如果没有tracer，直接执行请求
		if (tracer == null) {
			return next.exchange(request);
		}

		try {
			return Mono.fromCallable(() -> getCurrentSpan()).map(span -> {
				if (span != null) {
					String traceId = getTraceId(span);
					String spanId = getSpanId(span);

					if (traceId != null && spanId != null) {
						// 添加链路追踪头信息
						return ClientRequest.from(request)
							.header("X-Trace-Id", traceId)
							.header("X-Span-Id", spanId)
							.header("X-Request-ID", traceId)
							// 添加OpenTelemetry标准头
							.header("traceparent", buildTraceparent(traceId, spanId))
							.build();
					}
				}
				return request;
			}).defaultIfEmpty(request).flatMap(next::exchange);
		}
		catch (Exception e) {
			// 如果链路追踪不可用，继续正常执行
			return next.exchange(request);
		}
	}

	/**
	 * 通过反射获取当前Span
	 */
	private Object getCurrentSpan() {
		if (tracer == null) {
			return null;
		}
		try {
			Method currentSpanMethod = tracer.getClass().getMethod("currentSpan");
			return currentSpanMethod.invoke(tracer);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * 通过反射获取trace ID
	 */
	private String getTraceId(Object span) {
		try {
			Method contextMethod = span.getClass().getMethod("context");
			Object context = contextMethod.invoke(span);
			Method traceIdMethod = context.getClass().getMethod("traceId");
			return (String) traceIdMethod.invoke(context);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * 通过反射获取span ID
	 */
	private String getSpanId(Object span) {
		try {
			Method contextMethod = span.getClass().getMethod("context");
			Object context = contextMethod.invoke(span);
			Method spanIdMethod = context.getClass().getMethod("spanId");
			return (String) spanIdMethod.invoke(context);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * 构建符合W3C标准的traceparent头 格式：version-trace_id-parent_id-flags
	 */
	private String buildTraceparent(String traceId, String spanId) {
		return String.format("00-%s-%s-01", traceId, spanId);
	}

}
