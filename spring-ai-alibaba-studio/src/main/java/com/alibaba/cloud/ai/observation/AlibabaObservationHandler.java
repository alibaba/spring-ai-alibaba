package com.alibaba.cloud.ai.observation;

import com.alibaba.cloud.ai.observation.handler.ContextHandler;
import com.alibaba.cloud.ai.observation.handler.ContextHandlerFactory;
import io.micrometer.core.instrument.Clock;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description: AlibabaObservationHandler
 * @Author: 肖云涛
 * @Date: 2024/11/17
 */
public class AlibabaObservationHandler implements ObservationHandler<Observation.Context> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaObservationHandler.class);

	private final Clock clock;

	private final Tracer tracer;

	private final ContextHandlerFactory contextHandlerFactory;

	public AlibabaObservationHandler(ContextHandlerFactory contextHandlerFactory) {
		this.clock = Clock.SYSTEM;
		this.tracer = GlobalOpenTelemetry.getTracer("com.alibaba.cloud.ai");
		this.contextHandlerFactory = contextHandlerFactory;
	}

	@Override
	public void onStart(Observation.Context context) {
		long startTime = getCurrentTimeMillis();
		context.put("startTime", startTime);

		SpanBuilder spanBuilder = tracer.spanBuilder(context.getName())
			.setAttribute("component", "AlibabaChatClient")
			.setAttribute("start_time", startTime);
		Span span = spanBuilder.startSpan();

		context.put("span", span);
		LOGGER.info("onStart: Operation '{}' started. Start time: {}", context.getName(), startTime);
	}

	@Override
	public void onStop(Observation.Context context) {
		long startTime = context.getOrDefault("startTime", 0L);
		long endTime = getCurrentTimeMillis();
		long duration = endTime - startTime;

		Span span = context.getOrDefault("span", null);
		if (span != null) {
			span.setAttribute("duration_ns", duration);
			span.end();
		}
		ContextHandler<Observation.Context> handler = contextHandlerFactory.getHandler(context);

		if (handler != null) {
			handler.handle(context, duration);
		}
		else {
			LOGGER.warn("Unknown Observation.Context type: {}", context.getClass());
		}
	}

	@Override
	public void onError(Observation.Context context) {
		Span span = context.getOrDefault("span", null);
		if (span != null) {
			span.setAttribute("error", true);
			span.setAttribute("error.message", context.getError().getMessage());
			span.recordException(context.getError());
		}
		LOGGER.error("onError: Operation '{}' failed with error: {}", context.getName(),
				context.getError().getMessage());
	}

	@Override
	public boolean supportsContext(@NotNull Observation.Context context) {
		return true;
	}

	private long getCurrentTimeMillis() {
		return clock.monotonicTime() / 1_000_000;
	}

}