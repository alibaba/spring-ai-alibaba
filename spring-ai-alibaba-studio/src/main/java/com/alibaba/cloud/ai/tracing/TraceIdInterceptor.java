package com.alibaba.cloud.ai.tracing;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

	private final Tracer tracer;

	public TraceIdInterceptor(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (tracer.currentSpan() != null) {
			String traceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
			response.setHeader("X-Request-ID", traceId);
		}
		return true;
	}

}
