/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.exception.NotFoundException;
import com.alibaba.cloud.ai.oltp.StudioObservabilityProperties;
import com.alibaba.cloud.ai.service.StudioObservabilityService;
import com.alibaba.cloud.ai.utils.FileUtils;
import com.alibaba.cloud.ai.utils.JsonUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Description:
 * @Author: 肖云涛
 * @Date: 2024/12/8
 */
public class StudioObservabilityServiceImpl implements StudioObservabilityService {

	private static final Logger logger = Logger.getLogger(StudioObservabilityServiceImpl.class.getName());

	private final ObjectMapper objectMapper;

	private final StudioObservabilityProperties studioObservabilityProperties;

	private final Path outputPath;

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private final List<String> keyPrefixes = List.of("gen_ai.operation", "spring.ai");

	public StudioObservabilityServiceImpl(StudioObservabilityProperties studioObservabilityProperties) {
		this.objectMapper = new ObjectMapper();
		this.studioObservabilityProperties = studioObservabilityProperties;
		this.outputPath = Path.of(studioObservabilityProperties.getOutputFile());
	}

	@Override
	public CompletableResultCode export(Collection<ResourceSpansMarshaler> allResourceSpans) {
		try {
			FileUtils.createFileIfNotExists(outputPath);

			try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardOpenOption.APPEND)) {
				StringBuilder sb = new StringBuilder();
				for (ResourceSpansMarshaler resourceSpans : allResourceSpans) {
					String json = generateJson(resourceSpans);
					sb.append(json).append(LINE_SEPARATOR);

					if (sb.length() > 1024 * 1024) {
						writer.write(sb.toString());
						sb.setLength(0);
					}
				}

				if (!sb.isEmpty()) {
					writer.write(sb.toString());
				}
			}

		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Error exporting spans to file", e);
			return CompletableResultCode.ofFailure();
		}

		return CompletableResultCode.ofSuccess();
	}

	@Override
	public ArrayNode getAITraceInfo() {
		ArrayNode resultArray = objectMapper.createArrayNode();
		ArrayNode jsonArray = readObservabilityFile();

		for (JsonNode jsonNode : jsonArray) {
			JsonNode scopeSpans = jsonNode.path("scopeSpans");
			JsonNode spans = scopeSpans.get(0).path("spans");
			for (JsonNode span : spans) {
				JsonNode attributes = span.path("attributes");
				boolean hasMatchingKey = false;
				for (JsonNode attribute : attributes) {
					String key = attribute.path("key").asText();
					if (keyPrefixes.stream().anyMatch(key::startsWith)) {
						hasMatchingKey = true;
						break;
					}
				}
				if (hasMatchingKey) {
					resultArray.add(jsonNode);
					break;
				}
			}
		}
		return resultArray;
	}

	private String generateJson(ResourceSpansMarshaler resourceSpans) {
		SegmentedStringWriter sw = new SegmentedStringWriter(JsonUtil.JSON_FACTORY._getBufferRecycler());
		String json;
		try (JsonGenerator gen = JsonUtil.create(sw)) {
			resourceSpans.writeJsonTo(gen);
			json = sw.getAndClear();
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Error generating OTLP JSON spans", e);
			return "";
		}
		return json;
	}

	@Override
	public ArrayNode readObservabilityFile() {
		ArrayNode jsonArray = objectMapper.createArrayNode();

		if (!outputPath.toFile().isFile()) {
			logger.log(Level.WARNING, "Invalid file path: " + studioObservabilityProperties.getOutputFile());
			return jsonArray;
		}

		try {
			List<String> lines = FileUtils.readLines(outputPath);
			for (String line : lines) {
				try {
					JsonNode jsonNode = objectMapper.readTree(line);
					jsonArray.add(jsonNode);
				}
				catch (IOException e) {
					logger.log(Level.WARNING, "Invalid JSON entry in file: " + line, e);
				}
			}
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Error reading JSON from file", e);
		}
		return jsonArray;
	}

	@Override
	public JsonNode getTraceByTraceId(String traceId) {
		ArrayNode jsonArray = readObservabilityFile();

		for (JsonNode jsonNode : jsonArray) {
			JsonNode scopeSpans = jsonNode.path("scopeSpans");
			JsonNode spans = scopeSpans.get(0).path("spans");
			JsonNode traceIdNode = spans.get(0).path("traceId");
			if (traceIdNode.isTextual() && traceIdNode.asText().equals(traceId)) {
				return jsonNode;
			}
		}
		throw new NotFoundException("Not found trace info");
	}

	public List<ListResponse> extractSpansWithoutParentSpanId() {
		List<ListResponse> spanDataList = new ArrayList<>();
		ArrayNode jsonArray = readObservabilityFile();

		for (JsonNode jsonNode : jsonArray) {
			JsonNode scopeSpansNode = jsonNode.path("scopeSpans");

			// Flag to track if we've found a span without parentSpanId
			boolean found = false;

			for (JsonNode scopeSpan : scopeSpansNode) {
				JsonNode spansNode = scopeSpan.path("spans");

				// Skip further processing if already found a valid span
				if (found)
					break;

				for (JsonNode span : spansNode) {
					// Check if the span has no parentSpanId
					if (!span.has("parentSpanId")) {
						String traceId = span.path("traceId").asText();
						String startTimeUnixNano = span.path("startTimeUnixNano").asText();
						String endTimeUnixNano = span.path("endTimeUnixNano").asText();

						ListResponse spanData = new ListResponse(traceId, spansNode.size(), startTimeUnixNano,
								endTimeUnixNano);
						spanDataList.add(spanData);

						// Mark that we found a valid span and stop further searches
						found = true;
						break;
					}
				}
			}
		}

		return spanDataList;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ListResponse(@JsonProperty("traceId") String traceId, @JsonProperty("spansSize") Integer spansSize,
			@JsonProperty("startTimeUnixNano") String startTimeUnixNano,
			@JsonProperty("endTimeUnixNano") String endTimeUnixNano) {
	}

	@Override
	public String clearExportContent() {
		try {
			Files.deleteIfExists(outputPath);
			logger.log(Level.INFO, "File content cleared.");
			return "File content cleared successfully.";
		}
		catch (IOException e) {
			logger.log(Level.SEVERE, "Error clearing the file content", e);
			return "Error clearing the file content: " + e.getMessage();
		}
	}

}
