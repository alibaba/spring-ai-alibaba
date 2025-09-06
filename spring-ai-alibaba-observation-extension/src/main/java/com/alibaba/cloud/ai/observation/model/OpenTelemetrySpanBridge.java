package com.alibaba.cloud.ai.observation.model;

import io.micrometer.tracing.handler.TracingObservationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import io.opentelemetry.api.trace.Span;

public final class OpenTelemetrySpanBridge {

	private static final Logger logger = LoggerFactory.getLogger(OpenTelemetrySpanBridge.class);

	private static Method toOtelMethod;

	@Nullable
	public static Span retrieveOtelSpan(@Nullable TracingObservationHandler.TracingContext tracingContext) {
		if (tracingContext == null) {
			return null;
		}

		io.micrometer.tracing.Span micrometerSpan = tracingContext.getSpan();
		try {
			if (toOtelMethod == null) {
				Method method = tracingContext.getSpan()
					.getClass()
					.getDeclaredMethod("toOtel", io.micrometer.tracing.Span.class);
				method.setAccessible(true);
				toOtelMethod = method;
			}

			Object otelSpanObject = toOtelMethod.invoke(null, micrometerSpan);
			if (otelSpanObject instanceof Span otelSpan) {
				return otelSpan;
			}
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
			logger.warn("Failed to retrieve the OpenTelemetry Span from Micrometer context", ex);
			return null;
		}

		return null;
	}

}
