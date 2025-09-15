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
package com.alibaba.cloud.ai.mcp.common.tracing;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

public class McpTraceExchangeFilterFunction implements ExchangeFilterFunction {

	private final Object tracer;

	public McpTraceExchangeFilterFunction(Object tracer) {
		this.tracer = tracer;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		// If there is no tracer, execute the request directly
		if (tracer == null) {
			return next.exchange(request);
		}

		try {

			Object span = getCurrentSpan();
			if (span != null) {
				String traceId = getTraceId(span);
				String spanId = getSpanId(span);

				if (traceId != null && spanId != null) {

					ClientRequest enrichedRequest = ClientRequest.from(request)
						.header("X-Trace-Id", traceId)
						.header("X-Span-Id", spanId)
						.header("X-Request-ID", traceId)

						.header("traceparent", buildTraceparent(traceId, spanId))
						.build();
					return next.exchange(enrichedRequest);
				}
			}
			return next.exchange(request);
		}
		catch (Exception e) {

			return next.exchange(request);
		}
	}

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

	private String buildTraceparent(String traceId, String spanId) {
		return String.format("00-%s-%s-01", traceId, spanId);
	}

}
