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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * {@link SpanExporter} SPI implementation for {@link OtlpFileSpanExporter}.
 *
 * <p>
 * This class is internal and is hence not for public use. Its APIs are unstable and can
 * change at any time.
 */
public class OtlpFileSpanExporterProvider implements ConfigurableSpanExporterProvider {

	@Override
	public SpanExporter createExporter(ConfigProperties config) {
		return OtlpFileSpanExporter.create();
	}

	@Override
	public String getName() {
		return "file-otlp";
	}

}
