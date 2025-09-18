/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.StreamHttpNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreamHttpNodeDataConverterTest {

	@InjectMocks
	private StreamHttpNodeDataConverter converter;

	@Test
	void shouldSupportStreamHttpNodeType() {
		assertThat(converter.supportNodeType(NodeType.STREAM_HTTP)).isTrue();
		assertThat(converter.supportNodeType(NodeType.HTTP)).isFalse();
		assertThat(converter.supportNodeType(NodeType.LLM)).isFalse();
	}

	@Test
	void shouldGenerateCorrectVarName() {
		assertThat(converter.generateVarName(1)).isEqualTo("streamHttpNode1");
		assertThat(converter.generateVarName(5)).isEqualTo("streamHttpNode5");
	}

	@Test
	void shouldParseDifyDSLFormat() throws Exception {
		// Given
		Map<String, Object> dslData = new java.util.HashMap<>();
		dslData.put("method", "POST");
		dslData.put("url", "https://api.example.com/stream");
		dslData.put("headers", Map.of("Authorization", "Bearer token123", "Content-Type", "application/json"));
		dslData.put("body", Map.of("query", "test query"));
		dslData.put("stream_format", "SSE");
		dslData.put("stream_mode", "DISTRIBUTE");
		dslData.put("delimiter", "\n");
		dslData.put("output_key", "stream_results");
		dslData.put("timeout", 60000);
		dslData.put("authorization", "Bearer token123");
		dslData.put("auth_type", "BEARER");

		// When
		StreamHttpNodeData result = converter.parseMapData(dslData, DSLDialectType.DIFY);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getMethod()).isEqualTo("POST");
		assertThat(result.getUrl()).isEqualTo("https://api.example.com/stream");
		assertThat(result.getHeaders()).containsEntry("Authorization", "Bearer token123");
		assertThat(result.getHeaders()).containsEntry("Content-Type", "application/json");
		assertThat(result.getBody()).containsEntry("query", "test query");
		assertThat(result.getStreamFormat()).isEqualTo("SSE");
		assertThat(result.getStreamMode()).isEqualTo("DISTRIBUTE");
		assertThat(result.getDelimiter()).isEqualTo("\n");
		assertThat(result.getOutputKey()).isEqualTo("stream_results");
		assertThat(result.getTimeout()).isEqualTo(60000);
		assertThat(result.getAuthorization()).isEqualTo("Bearer token123");
		assertThat(result.getAuthType()).isEqualTo("BEARER");
	}

	@Test
	void shouldDumpToDifyDSLFormat() throws Exception {
		// Given
		StreamHttpNodeData nodeData = new StreamHttpNodeData(List.of(), List.of());
		nodeData.setMethod("POST")
			.setUrl("https://api.example.com/stream")
			.setHeaders(Map.of("Authorization", "Bearer token123"))
			.setBody(Map.of("query", "test query"))
			.setStreamFormat("JSON_LINES")
			.setStreamMode("AGGREGATE")
			.setDelimiter("|")
			.setOutputKey("results")
			.setTimeout(45000)
			.setAuthorization("Bearer token123")
			.setAuthType("BEARER");

		// When
		Map<String, Object> result = converter.dumpMapData(nodeData, DSLDialectType.DIFY);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.get("method")).isEqualTo("POST");
		assertThat(result.get("url")).isEqualTo("https://api.example.com/stream");
		assertThat(result.get("headers")).isEqualTo(Map.of("Authorization", "Bearer token123"));
		assertThat(result.get("body")).isEqualTo(Map.of("query", "test query"));
		assertThat(result.get("stream_format")).isEqualTo("JSON_LINES");
		assertThat(result.get("stream_mode")).isEqualTo("AGGREGATE");
		assertThat(result.get("delimiter")).isEqualTo("|");
		assertThat(result.get("output_key")).isEqualTo("results");
		assertThat(result.get("timeout")).isEqualTo(45000);
		assertThat(result.get("authorization")).isEqualTo("Bearer token123");
		assertThat(result.get("auth_type")).isEqualTo("BEARER");
	}

	@Test
	void shouldParseStudioDSLFormat() throws Exception {
		// Given
		Map<String, Object> nodeParam = new java.util.HashMap<>();
		nodeParam.put("method", "GET");
		nodeParam.put("url", "https://api.example.com/events");
		nodeParam.put("headers", Map.of("Accept", "text/event-stream"));
		nodeParam.put("streamFormat", "SSE");
		nodeParam.put("streamMode", "DISTRIBUTE");
		nodeParam.put("delimiter", "\n");
		nodeParam.put("outputKey", "events");
		nodeParam.put("timeout", 30000);

		Map<String, Object> dslData = Map.of("node_param", nodeParam);

		// When
		StreamHttpNodeData result = converter.parseMapData(dslData, DSLDialectType.STUDIO);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getMethod()).isEqualTo("GET");
		assertThat(result.getUrl()).isEqualTo("https://api.example.com/events");
		assertThat(result.getHeaders()).containsEntry("Accept", "text/event-stream");
		assertThat(result.getStreamFormat()).isEqualTo("SSE");
		assertThat(result.getStreamMode()).isEqualTo("DISTRIBUTE");
		assertThat(result.getOutputKey()).isEqualTo("events");
		assertThat(result.getTimeout()).isEqualTo(30000);
	}

	@Test
	void shouldDumpToStudioDSLFormat() throws Exception {
		// Given
		StreamHttpNodeData nodeData = new StreamHttpNodeData(List.of(), List.of());
		nodeData.setMethod("GET")
			.setUrl("https://api.example.com/events")
			.setHeaders(Map.of("Accept", "text/event-stream"))
			.setStreamFormat("SSE")
			.setStreamMode("DISTRIBUTE")
			.setOutputKey("events")
			.setTimeout(30000);

		// When
		Map<String, Object> result = converter.dumpMapData(nodeData, DSLDialectType.STUDIO);

		// Then
		assertThat(result).isNotNull();
		@SuppressWarnings("unchecked")
		Map<String, Object> nodeParam = (Map<String, Object>) result.get("node_param");
		assertThat(nodeParam).isNotNull();
		assertThat(nodeParam.get("method")).isEqualTo("GET");
		assertThat(nodeParam.get("url")).isEqualTo("https://api.example.com/events");
		assertThat(nodeParam.get("headers")).isEqualTo(Map.of("Accept", "text/event-stream"));
		assertThat(nodeParam.get("streamFormat")).isEqualTo("SSE");
		assertThat(nodeParam.get("streamMode")).isEqualTo("DISTRIBUTE");
		assertThat(nodeParam.get("outputKey")).isEqualTo("events");
		assertThat(nodeParam.get("timeout")).isEqualTo(30000);
	}

	@Test
	void shouldHandleDefaultValues() throws Exception {
		// Given - minimal DSL data
		Map<String, Object> dslData = Map.of("url", "https://api.example.com/stream");

		// When
		StreamHttpNodeData result = converter.parseMapData(dslData, DSLDialectType.DIFY);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getMethod()).isEqualTo("GET"); // default
		assertThat(result.getUrl()).isEqualTo("https://api.example.com/stream");
		assertThat(result.getStreamFormat()).isEqualTo("SSE"); // default
		assertThat(result.getStreamMode()).isEqualTo("DISTRIBUTE"); // default
		assertThat(result.getDelimiter()).isEqualTo("\n"); // default
		assertThat(result.getTimeout()).isEqualTo(30000); // default
	}

}
