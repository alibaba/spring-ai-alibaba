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

package com.alibaba.cloud.ai.agent.oltp;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom SpanExporter that stores relevant span data. It handles two types of trace data storage:
 * 1. Event-ID based: Stores attributes of specific spans (call_llm, send_data, tool_response) keyed
 * by `gcp.vertex.agent.event_id`. This is used for debugging individual events. 2. Session-ID
 * based: Stores all exported spans and maintains a mapping from `session_id` (extracted from
 * `call_llm` spans) to a list of `trace_id`s. This is used for retrieving all spans related to a
 * session.
 */
@Component
public class ApiServerSpanExporter implements SpanExporter {
	private static final Logger exporterLog = LoggerFactory.getLogger(ApiServerSpanExporter.class);

	private final Map<String, Map<String, Object>> eventIdTraceStorage = new ConcurrentHashMap<>();

	// Session ID -> Trace IDs -> Trace Object
	private final Map<String, List<String>> sessionToTraceIdsMap = new ConcurrentHashMap<>();

	private final List<SpanData> allExportedSpans = Collections.synchronizedList(new ArrayList<>());

	public ApiServerSpanExporter() {
	}

	public Map<String, Object> getEventTraceAttributes(String eventId) {
		return this.eventIdTraceStorage.get(eventId);
	}

	public Map<String, List<String>> getSessionToTraceIdsMap() {
		return this.sessionToTraceIdsMap;
	}

	public List<SpanData> getAllExportedSpans() {
		return this.allExportedSpans;
	}

	@Override
	public CompletableResultCode export(Collection<SpanData> spans) {
		exporterLog.debug("ApiServerSpanExporter received {} spans to export.", spans.size());
		List<SpanData> currentBatch = new ArrayList<>(spans);
		allExportedSpans.addAll(currentBatch);

		for (SpanData span : currentBatch) {
			String spanName = span.getName();
			if ("call_llm".equals(spanName)
					|| "send_data".equals(spanName)
					|| (spanName != null && spanName.startsWith("tool_response"))) {
				String eventId =
						span.getAttributes().get(AttributeKey.stringKey("gcp.vertex.agent.event_id"));
				if (eventId != null && !eventId.isEmpty()) {
					Map<String, Object> attributesMap = new HashMap<>();
					span.getAttributes().forEach((key, value) -> attributesMap.put(key.getKey(), value));
					attributesMap.put("trace_id", span.getSpanContext().getTraceId());
					attributesMap.put("span_id", span.getSpanContext().getSpanId());
					attributesMap.putIfAbsent("gcp.vertex.agent.event_id", eventId);
					exporterLog.debug("Storing event-based trace attributes for event_id: {}", eventId);
					this.eventIdTraceStorage.put(eventId, attributesMap); // Use internal storage
				}
				else {
					exporterLog.trace(
							"Span {} for event-based trace did not have 'gcp.vertex.agent.event_id'"
									+ " attribute or it was empty.",
							spanName);
				}
			}

			if ("call_llm".equals(spanName)) {
				String sessionId =
						span.getAttributes().get(AttributeKey.stringKey("gcp.vertex.agent.session_id"));
				if (sessionId != null && !sessionId.isEmpty()) {
					String traceId = span.getSpanContext().getTraceId();
					sessionToTraceIdsMap
							.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
							.add(traceId);
					exporterLog.trace(
							"Associated trace_id {} with session_id {} for session tracing", traceId, sessionId);
				}
				else {
					exporterLog.trace(
							"Span {} for session trace did not have 'gcp.vertex.agent.session_id' attribute.",
							spanName);
				}
			}
		}
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode flush() {
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode shutdown() {
		exporterLog.debug("Shutting down ApiServerSpanExporter.");
		// no need to clear storage on shutdown, as everything is currently stored in memory.
		return CompletableResultCode.ofSuccess();
	}
}
