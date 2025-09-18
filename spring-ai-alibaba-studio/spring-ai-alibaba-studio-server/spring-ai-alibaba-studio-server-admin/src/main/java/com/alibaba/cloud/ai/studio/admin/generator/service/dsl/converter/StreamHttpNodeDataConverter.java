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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.StreamHttpNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.stereotype.Component;

@Component
public class StreamHttpNodeDataConverter extends AbstractNodeDataConverter<StreamHttpNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.STREAM_HTTP.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<StreamHttpNodeData>> getDialectConverters() {
		return Stream.of(StreamHttpNodeDialectConverter.values())
			.map(StreamHttpNodeDialectConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum StreamHttpNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}

			@SuppressWarnings("unchecked")
			@Override
			public StreamHttpNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("variable_selector"))
					.filter(list -> list.size() == 2)
					.map(list -> Collections.singletonList(new VariableSelector(list.get(0), list.get(1))))
					.orElse(Collections.emptyList());

				List<com.alibaba.cloud.ai.studio.admin.generator.model.Variable> outputs = List.of();

				String method = (String) data.getOrDefault("method", "GET");
				String url = (String) data.get("url");

				// Parse headers
				Map<String, String> headers = Optional.ofNullable((Map<String, String>) data.get("headers"))
					.orElse(Collections.emptyMap());

				// Parse body
				Map<String, Object> body = Optional.ofNullable((Map<String, Object>) data.get("body"))
					.orElse(Collections.emptyMap());

				// Parse streaming configuration
				String streamFormat = (String) data.getOrDefault("stream_format", "SSE");
				String streamMode = (String) data.getOrDefault("stream_mode", "DISTRIBUTE");
				String delimiter = (String) data.getOrDefault("delimiter", "\n");
				String outputKey = (String) data.get("output_key");
				Integer timeout = Optional.ofNullable((Integer) data.get("timeout")).orElse(30000);

				// Parse authentication
				String authorization = (String) data.get("authorization");
				String authType = (String) data.get("auth_type");

				StreamHttpNodeData nodeData = new StreamHttpNodeData(inputs, outputs);
				nodeData.setMethod(method)
					.setUrl(url)
					.setHeaders(headers)
					.setBody(body)
					.setStreamFormat(streamFormat)
					.setStreamMode(streamMode)
					.setDelimiter(delimiter)
					.setOutputKey(outputKey)
					.setTimeout(timeout)
					.setAuthorization(authorization)
					.setAuthType(authType);

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(StreamHttpNodeData nodeData) {
				Map<String, Object> result = new LinkedHashMap<>();

				// Variable selector
				if (!nodeData.getInputs().isEmpty()) {
					VariableSelector selector = nodeData.getInputs().get(0);
					result.put("variable_selector", List.of(selector.getNamespace(), selector.getName()));
				}

				// HTTP configuration
				if (!"GET".equals(nodeData.getMethod())) {
					result.put("method", nodeData.getMethod());
				}
				if (nodeData.getUrl() != null) {
					result.put("url", nodeData.getUrl());
				}
				if (nodeData.getHeaders() != null && !nodeData.getHeaders().isEmpty()) {
					result.put("headers", nodeData.getHeaders());
				}
				if (nodeData.getBody() != null && !nodeData.getBody().isEmpty()) {
					result.put("body", nodeData.getBody());
				}

				// Streaming configuration
				if (!"SSE".equals(nodeData.getStreamFormat())) {
					result.put("stream_format", nodeData.getStreamFormat());
				}
				if (!"DISTRIBUTE".equals(nodeData.getStreamMode())) {
					result.put("stream_mode", nodeData.getStreamMode());
				}
				if (!"\n".equals(nodeData.getDelimiter())) {
					result.put("delimiter", nodeData.getDelimiter());
				}
				if (nodeData.getOutputKey() != null) {
					result.put("output_key", nodeData.getOutputKey());
				}
				if (nodeData.getTimeout() != null && !nodeData.getTimeout().equals(30000)) {
					result.put("timeout", nodeData.getTimeout());
				}

				// Authentication
				if (nodeData.getAuthorization() != null) {
					result.put("authorization", nodeData.getAuthorization());
				}
				if (nodeData.getAuthType() != null) {
					result.put("auth_type", nodeData.getAuthType());
				}

				return result;
			}
		}),

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.STUDIO.equals(dialect);
			}

			@SuppressWarnings("unchecked")
			@Override
			public StreamHttpNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				// Studio format parsing - more structured format
				List<VariableSelector> inputs = Collections.emptyList();
				List<com.alibaba.cloud.ai.studio.admin.generator.model.Variable> outputs = List.of();

				// Parse from config.node_param structure
				Map<String, Object> nodeParam = (Map<String, Object>) data.get("node_param");
				if (nodeParam == null) {
					nodeParam = data; // fallback to root level
				}

				String method = (String) nodeParam.getOrDefault("method", "GET");
				String url = (String) nodeParam.get("url");

				Map<String, String> headers = Optional.ofNullable((Map<String, String>) nodeParam.get("headers"))
					.orElse(Collections.emptyMap());

				Map<String, Object> body = Optional.ofNullable((Map<String, Object>) nodeParam.get("body"))
					.orElse(Collections.emptyMap());

				String streamFormat = (String) nodeParam.getOrDefault("streamFormat", "SSE");
				String streamMode = (String) nodeParam.getOrDefault("streamMode", "DISTRIBUTE");
				String delimiter = (String) nodeParam.getOrDefault("delimiter", "\n");
				String outputKey = (String) nodeParam.get("outputKey");
				Integer timeout = Optional.ofNullable((Integer) nodeParam.get("timeout")).orElse(30000);

				String authorization = (String) nodeParam.get("authorization");
				String authType = (String) nodeParam.get("authType");

				StreamHttpNodeData nodeData = new StreamHttpNodeData(inputs, outputs);
				nodeData.setMethod(method)
					.setUrl(url)
					.setHeaders(headers)
					.setBody(body)
					.setStreamFormat(streamFormat)
					.setStreamMode(streamMode)
					.setDelimiter(delimiter)
					.setOutputKey(outputKey)
					.setTimeout(timeout)
					.setAuthorization(authorization)
					.setAuthType(authType);

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(StreamHttpNodeData nodeData) {
				Map<String, Object> result = new LinkedHashMap<>();
				Map<String, Object> nodeParam = new LinkedHashMap<>();

				// HTTP configuration
				nodeParam.put("method", nodeData.getMethod());
				if (nodeData.getUrl() != null) {
					nodeParam.put("url", nodeData.getUrl());
				}
				if (nodeData.getHeaders() != null) {
					nodeParam.put("headers", nodeData.getHeaders());
				}
				if (nodeData.getBody() != null) {
					nodeParam.put("body", nodeData.getBody());
				}

				// Streaming configuration
				nodeParam.put("streamFormat", nodeData.getStreamFormat());
				nodeParam.put("streamMode", nodeData.getStreamMode());
				nodeParam.put("delimiter", nodeData.getDelimiter());
				if (nodeData.getOutputKey() != null) {
					nodeParam.put("outputKey", nodeData.getOutputKey());
				}
				nodeParam.put("timeout", nodeData.getTimeout());

				// Authentication
				if (nodeData.getAuthorization() != null) {
					nodeParam.put("authorization", nodeData.getAuthorization());
				}
				if (nodeData.getAuthType() != null) {
					nodeParam.put("authType", nodeData.getAuthType());
				}

				result.put("node_param", nodeParam);
				return result;
			}
		}),

		CUSTOM(defaultCustomDialectConverter(StreamHttpNodeData.class));

		private final DialectConverter<StreamHttpNodeData> converter;

		StreamHttpNodeDialectConverter(DialectConverter<StreamHttpNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<StreamHttpNodeData> dialectConverter() {
			return this.converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "streamHttpNode" + count;
	}

	@Override
	public BiConsumer<StreamHttpNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((streamHttpNodeData, idToVarName) -> {
				// Set output key
				streamHttpNodeData.setOutputKey(
						streamHttpNodeData.getVarName() + "_" + StreamHttpNodeData.getDefaultOutputSchema().getName());
				streamHttpNodeData.setOutputs(List.of(StreamHttpNodeData.getDefaultOutputSchema()));
			}).andThen(super.postProcessConsumer(dialectType)).andThen((streamHttpNodeData, idToVarName) -> {
				// Convert Dify variable templates to SAA intermediate variables
				if (streamHttpNodeData.getHeaders() != null) {
					Map<String, String> convertedHeaders = streamHttpNodeData.getHeaders()
						.entrySet()
						.stream()
						.collect(Collectors.toMap(
								entry -> this.convertVarTemplate(dialectType, entry.getKey().replace("{{#", "${{#"),
										idToVarName),
								entry -> this.convertVarTemplate(dialectType, entry.getValue().replace("{{#", "${{#"),
										idToVarName),
								(oldVal, newVal) -> newVal));
					streamHttpNodeData.setHeaders(convertedHeaders);
				}

				// Convert URL template variables
				if (streamHttpNodeData.getUrl() != null) {
					String convertedUrl = this.convertVarTemplate(dialectType,
							streamHttpNodeData.getUrl().replace("{{#", "${{#"), idToVarName);
					streamHttpNodeData.setUrl(convertedUrl);
				}
			});
			case STUDIO -> emptyProcessConsumer().andThen((streamHttpNodeData, idToVarName) -> {
				// Set output key for Studio format
				if (streamHttpNodeData.getOutputKey() == null) {
					streamHttpNodeData.setOutputKey(streamHttpNodeData.getVarName() + "_"
							+ StreamHttpNodeData.getDefaultOutputSchema().getName());
				}
				streamHttpNodeData.setOutputs(List.of(StreamHttpNodeData.getDefaultOutputSchema()));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
