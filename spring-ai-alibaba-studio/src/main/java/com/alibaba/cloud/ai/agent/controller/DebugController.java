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

package com.alibaba.cloud.ai.agent.controller;

import com.alibaba.cloud.ai.agent.oltp.ApiServerSpanExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Controller handling debug and tracing endpoints. */
@RestController
public class DebugController {

	private static final Logger log = LoggerFactory.getLogger(DebugController.class);

	private final ApiServerSpanExporter apiServerSpanExporter;

	@Autowired
	public DebugController(ApiServerSpanExporter apiServerSpanExporter) {
		this.apiServerSpanExporter = apiServerSpanExporter;
	}

	/**
	 * Endpoint for retrieving trace information stored by the ApiServerSpanExporter, based on event
	 * ID.
	 *
	 * @param eventId The ID of the event to trace (expected to be gcp.vertex.agent.event_id).
	 * @return A ResponseEntity containing the trace data or NOT_FOUND.
	 */
	@GetMapping("/debug/trace/{eventId}")
	public ResponseEntity<?> getTraceDict(@PathVariable String eventId) {
		log.info("Request received for GET /debug/trace/{}", eventId);
		Map<String, Object> traceData = this.apiServerSpanExporter.getEventTraceAttributes(eventId);
		if (traceData == null) {
			log.warn("Trace not found for eventId: {}", eventId);
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Collections.singletonMap("message", "Trace not found for eventId: " + eventId));
		}
		log.info("Returning trace data for eventId: {}", eventId);
		return ResponseEntity.ok(traceData);
	}

	/**
	 * Retrieves trace spans for a given session ID.
	 *
	 * @param sessionId The session ID.
	 * @return A ResponseEntity containing a list of span data maps for the session, or an empty list.
	 */
	@GetMapping("/debug/trace/session/{sessionId}")
	public ResponseEntity<Object> getSessionTrace(@PathVariable String sessionId) {
		log.info("Request received for GET /debug/trace/session/{}", sessionId);

		List<String> traceIdsForSession =
				this.apiServerSpanExporter.getSessionToTraceIdsMap().get(sessionId);

		if (traceIdsForSession == null || traceIdsForSession.isEmpty()) {
			log.warn("No trace IDs found for session ID: {}", sessionId);
			return ResponseEntity.ok(Collections.emptyList());
		}

		// Iterate over a snapshot of all spans to avoid concurrent modification issues
		// if the exporter is actively adding spans.
		List<SpanData> allSpansSnapshot =
				new ArrayList<>(this.apiServerSpanExporter.getAllExportedSpans());

		if (allSpansSnapshot.isEmpty()) {
			log.warn("No spans have been exported yet overall.");
			return ResponseEntity.ok(Collections.emptyList());
		}

		Set<String> relevantTraceIds = new HashSet<>(traceIdsForSession);
		List<Map<String, Object>> resultSpans = new ArrayList<>();

		for (SpanData span : allSpansSnapshot) {
			if (relevantTraceIds.contains(span.getSpanContext().getTraceId())) {
				Map<String, Object> spanMap = new HashMap<>();
				spanMap.put("name", span.getName());
				spanMap.put("span_id", span.getSpanContext().getSpanId());
				spanMap.put("trace_id", span.getSpanContext().getTraceId());
				spanMap.put("start_time", span.getStartEpochNanos());
				spanMap.put("end_time", span.getEndEpochNanos());

				Map<String, Object> attributesMap = new HashMap<>();
				span.getAttributes().forEach((key, value) -> attributesMap.put(key.getKey(), value));
				spanMap.put("attributes", attributesMap);

				String parentSpanId = span.getParentSpanId();
				if (SpanId.isValid(parentSpanId)) {
					spanMap.put("parent_span_id", parentSpanId);
				}
				else {
					spanMap.put("parent_span_id", null);
				}
				resultSpans.add(spanMap);
			}
		}

		log.info("Returning {} spans for session ID: {}", resultSpans.size(), sessionId);
		return ResponseEntity.ok(resultSpans);
	}
}
