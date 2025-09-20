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
