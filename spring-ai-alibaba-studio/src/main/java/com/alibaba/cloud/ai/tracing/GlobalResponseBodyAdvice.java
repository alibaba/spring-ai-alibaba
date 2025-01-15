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