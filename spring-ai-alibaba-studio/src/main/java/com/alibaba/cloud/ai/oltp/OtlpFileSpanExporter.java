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
package com.alibaba.cloud.ai.oltp;

import com.alibaba.cloud.ai.service.StudioObservabilityService;
import com.alibaba.cloud.ai.service.impl.StudioObservabilityServiceImpl;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link SpanExporter} which writes {@linkplain SpanData spans} to a {@link Logger} in
 * OTLP JSON format. Each log line will include a single {@code ResourceSpans}.
 */
public final class OtlpFileSpanExporter implements SpanExporter {

	private static final Logger logger = Logger.getLogger(OtlpFileSpanExporter.class.getName());

	private final AtomicBoolean isShutdown = new AtomicBoolean();

	private final StudioObservabilityProperties studioObservabilityProperties;

	private final StudioObservabilityService studioObservabilityService;

	/** Returns a new {@link OtlpFileSpanExporter}. */
	public static SpanExporter create() {
		return new OtlpFileSpanExporter(new StudioObservabilityProperties());
	}

	private OtlpFileSpanExporter(StudioObservabilityProperties studioObservabilityProperties) {
		this.studioObservabilityProperties = studioObservabilityProperties;
		this.studioObservabilityService = new StudioObservabilityServiceImpl(studioObservabilityProperties);
	}

	@Override
	public CompletableResultCode export(Collection<SpanData> spans) {
		if (!studioObservabilityProperties.isEnabled()) {
			return CompletableResultCode.ofSuccess();
		}

		if (isShutdown.get()) {
			return CompletableResultCode.ofFailure();
		}

		ResourceSpansMarshaler[] allResourceSpans = ResourceSpansMarshaler.create(spans);

		return studioObservabilityService.export(List.of(allResourceSpans));
	}

	@Override
	public CompletableResultCode flush() {
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode shutdown() {
		if (!isShutdown.compareAndSet(false, true)) {
			logger.log(Level.INFO, "Calling shutdown() multiple times.");
		}
		return CompletableResultCode.ofSuccess();
	}

}
