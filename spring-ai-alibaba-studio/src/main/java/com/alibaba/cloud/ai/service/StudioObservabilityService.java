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
package com.alibaba.cloud.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collection;

/**
 * @Description:
 * @Author: 肖云涛
 * @Date: 2024/12/8
 */
public interface StudioObservabilityService {

	CompletableResultCode export(Collection<ResourceSpansMarshaler> allResourceSpans);

	ArrayNode getAITraceInfo();

	ArrayNode readObservabilityFile();

	JsonNode getTraceByTraceId(String traceId);

	String clearExportContent();

}
