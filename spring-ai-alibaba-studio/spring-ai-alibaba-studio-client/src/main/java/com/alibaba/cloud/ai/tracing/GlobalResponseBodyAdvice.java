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
package com.alibaba.cloud.ai.tracing;

import com.alibaba.cloud.ai.common.R;
import io.micrometer.tracing.Tracer;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	private final Tracer tracer;

	public GlobalResponseBodyAdvice(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public boolean supports(MethodParameter returnType, Class converterType) {
		return returnType.getParameterType().equals(R.class);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
		if (tracer.currentSpan() != null) {
			String traceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
			R newBody = (R) body;
			newBody.setRequestId(traceId);
			return body;
		}
		else {
			return body;
		}
	}

}
