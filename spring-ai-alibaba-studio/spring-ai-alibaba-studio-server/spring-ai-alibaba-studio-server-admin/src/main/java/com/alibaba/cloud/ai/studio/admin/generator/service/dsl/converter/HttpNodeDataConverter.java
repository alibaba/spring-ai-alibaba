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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.TimeoutConfig;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.HttpNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.ai.util.json.JsonParser;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Convert the HTTP node configuration in the Dify DSL to and from the HttpNodeData
 * object.
 */
@Component
public class HttpNodeDataConverter extends AbstractNodeDataConverter<HttpNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.HTTP.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<HttpNodeData>> getDialectConverters() {
		return Stream.of(HttpNodeDialectConverter.values())
			.map(HttpNodeDialectConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum HttpNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}

			@SuppressWarnings("unchecked")
			@Override
			public HttpNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("variable_selector"))
					.filter(list -> list.size() == 2)
					.map(list -> Collections.singletonList(new VariableSelector(list.get(0), list.get(1))))
					.orElse(Collections.emptyList());

				List<com.alibaba.cloud.ai.studio.admin.generator.model.Variable> outputs = List.of();

				HttpMethod method = data.containsKey("method")
						? HttpMethod.valueOf(((String) data.get("method")).toUpperCase()) : HttpMethod.GET;
				String url = (String) data.get("url");

				// headers & query_params
				Object headersObj = data.get("headers");
				Map<String, String> headers;
				if (headersObj instanceof Map) {
					headers = (Map<String, String>) headersObj;
				}
				else if (headersObj instanceof String str) {
					// Dify DSL以"key:value"的形式每行存储一个headers对
					headers = Arrays.stream(str.split("\\r?\\n"))
						.map(line -> line.split(":"))
						.filter(parts -> parts.length == 2)
						.collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim(),
								(oldValue, newValue) -> newValue));
				}
				else {
					headers = Collections.emptyMap();
				}
				Object paramsObj = data.get("params");
				Map<String, String> queryParams;
				if (paramsObj instanceof Map) {
					queryParams = (Map<String, String>) paramsObj;
				}
				else if (paramsObj instanceof String str) {
					// Dify DSL以"key:value"的形式每行存储一个params对
					queryParams = Arrays.stream(str.split("\\r?\\n"))
						.map(line -> line.split(":"))
						.filter(parts -> parts.length == 2)
						.collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim(),
								(oldValue, newValue) -> newValue));
				}
				else {
					queryParams = Collections.emptyMap();
				}

				// body
				Object rawBody = data.get("body");
				HttpRequestNodeBody body = HttpRequestNodeBody.from(rawBody);

				// auth
				AuthConfig auth = null;
				if (data.containsKey("authorization")) {
					Map<String, Object> am = (Map<String, Object>) data.get("authorization");
					String type = ((String) am.getOrDefault("type", "no-auth")).toLowerCase();
					auth = switch (type) {
						case "basic" -> AuthConfig.basic((String) am.get("username"), (String) am.get("password"));
						case "bearer" -> AuthConfig.bearer((String) am.get("token"));
						default -> auth;
					};
				}

				// retry_config
				Map<String, Object> rcMap = (Map<String, Object>) data.get("retry_config");
				int maxRetries = rcMap != null && rcMap.get("max_retries") != null
						? ((Number) rcMap.get("max_retries")).intValue() : 3;
				long maxRetryInterval = rcMap != null && rcMap.get("retry_interval") != null
						? ((Number) rcMap.get("retry_interval")).longValue() : 1000L;
				boolean enable = rcMap != null && rcMap.get("retry_enabled") != null
						? (Boolean) rcMap.get("retry_enabled") : true;
				RetryConfig retryConfig = new RetryConfig(maxRetries, maxRetryInterval, enable);

				// timeout_config
				Map<String, Object> timeoutMap = (Map<String, Object>) data.get("timeout");
				TimeoutConfig timeoutConfig;
				if (timeoutMap != null) {
					int connect = timeoutMap.get("connect") != null ? ((Number) timeoutMap.get("connect")).intValue()
							: 10;
					int read = timeoutMap.get("read") != null ? ((Number) timeoutMap.get("read")).intValue() : 60;
					int write = timeoutMap.get("write") != null ? ((Number) timeoutMap.get("write")).intValue() : 20;
					int maxConnect = timeoutMap.get("max_connect_timeout") != null
							? ((Number) timeoutMap.get("max_connect_timeout")).intValue() : 300;
					int maxRead = timeoutMap.get("max_read_timeout") != null
							? ((Number) timeoutMap.get("max_read_timeout")).intValue() : 600;
					int maxWrite = timeoutMap.get("max_write_timeout") != null
							? ((Number) timeoutMap.get("max_write_timeout")).intValue() : 600;
					timeoutConfig = new TimeoutConfig(connect, read, write, maxConnect, maxRead, maxWrite);
				}
				else {
					timeoutConfig = new TimeoutConfig(10, 60, 20, 300, 600, 6000);
				}

				// output_key
				String outputKey = (String) data.get("output_key");

				HttpNodeData nd = new HttpNodeData(inputs, outputs, method, url, headers, queryParams, body, auth,
						retryConfig, timeoutConfig, outputKey);
				if (rawBody instanceof Map<?, ?>) {
					nd.setRawBodyMap((Map<String, Object>) rawBody);
				}
				return nd;
			}

			@Override
			public Map<String, Object> dump(HttpNodeData nd) {
				throw new UnsupportedOperationException();
			}
		}), STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public HttpNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				// 获取必要信息
				HttpMethod httpMethod = HttpMethod.valueOf(Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "method"))
					.orElse("GET")
					.toString()
					.toUpperCase());
				String url = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "url");

				Map<String, String> headers = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, Object.class, "config", "node_param", "headers")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("key") && map.containsKey("value"))
					.collect(Collectors.toUnmodifiableMap(map -> map.get("key").toString(),
							map -> map.get("value").toString()));
				Map<String, String> queryParams = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, Object.class, "config", "node_param", "params")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("key") && map.containsKey("value"))
					.collect(Collectors.toUnmodifiableMap(map -> map.get("key").toString(),
							map -> map.get("value").toString()));

				Object rawBody = MapReadUtil.getMapDeepValue(data, Object.class, "config", "node_param", "body");
				HttpRequestNodeBody body = HttpRequestNodeBody.from(rawBody);
				AuthConfig auth = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "authorization",
							"auth_config", "value"))
					.stream()
					.map(AuthConfig::bearer)
					.findFirst()
					.orElse(null);

				int maxRetries = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "retry_config",
							"max_retries"))
					.map(Integer::parseInt)
					.orElse(1);
				long maxRetryInterval = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "retry_config",
							"retry_interval"))
					.map(Long::parseLong)
					.orElse(1000L);
				boolean enable = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "retry_config",
							"retry_enabled"))
					.map(Boolean::parseBoolean)
					.orElse(false);
				RetryConfig retryConfig = new RetryConfig(maxRetries, maxRetryInterval, enable);
				String outputKey = "output";
				return new HttpNodeData(List.of(), List.of(), httpMethod, url, headers, queryParams, body, auth,
						retryConfig, new TimeoutConfig(10, 60, 20, 300, 600, 6000), outputKey);
			}

			@Override
			public Map<String, Object> dump(HttpNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(defaultCustomDialectConverter(HttpNodeData.class));

		private final DialectConverter<HttpNodeData> converter;

		HttpNodeDialectConverter(DialectConverter<HttpNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<HttpNodeData> dialectConverter() {
			return this.converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "httpNode" + count;
	}

	@Override
	public BiConsumer<HttpNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((httpNodeData, idToVarName) -> {
				// 设置输出键
				httpNodeData.setOutputKey(httpNodeData.getVarName() + "_"
						+ HttpNodeData.getDefaultOutputSchemas(dialectType).get(0).getName());
				httpNodeData.setOutputs(HttpNodeData.getDefaultOutputSchemas(dialectType));
			}).andThen(super.postProcessConsumer(dialectType)).andThen((httpNodeData, idToVarName) -> {
				// 将headers，params，body的Dify参数占位符转化为SAA中间变量
				httpNodeData.setHeaders(httpNodeData.getHeaders()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(
							// HttpNode源代码使用${}的变量格式
							entry -> this.convertVarTemplate(dialectType, entry.getKey().replace("{{#", "${{#"),
									idToVarName),
							entry -> this.convertVarTemplate(dialectType, entry.getValue().replace("{{#", "${{#"),
									idToVarName),
							(oldVal, newVal) -> newVal)));
				httpNodeData.setQueryParams(httpNodeData.getQueryParams()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(
							entry -> this.convertVarTemplate(dialectType, entry.getKey().replace("{{#", "${{#"),
									idToVarName),
							entry -> this.convertVarTemplate(dialectType, entry.getValue().replace("{{#", "${{#"),
									idToVarName),
							(oldVal, newVal) -> newVal)));
				httpNodeData.getBody().setData(httpNodeData.getBody().getData().stream().peek(data -> {
					if (data.getKey() != null)
						data.setKey(this.convertVarTemplate(dialectType, data.getKey().replace("{{#", "${{#"),
								idToVarName));
					if (data.getValue() != null)
						data.setValue(this.convertVarTemplate(dialectType, data.getValue().replace("{{#", "${{#"),
								idToVarName));
				}).toList());
				// 处理rawBodyMap
				Map<String, Object> rawBodyMap = httpNodeData.getRawBodyMap();
				if (!CollectionUtils.isEmpty(rawBodyMap)) {
					String json = JsonParser.toJson(rawBodyMap);
					json = this.convertVarTemplate(dialectType, json.replace("{{#", "${{#"), idToVarName);
					try {
						httpNodeData.setRawBodyMap(JsonParser.fromJson(json, new TypeReference<Map<String, Object>>() {
						}));
					}
					catch (Exception ignore) {
					}
				}
			});
			case STUDIO -> emptyProcessConsumer().andThen((httpNodeData, idToVarName) -> {
				// 设置输出键
				httpNodeData.setOutputKey(httpNodeData.getVarName() + "_"
						+ HttpNodeData.getDefaultOutputSchemas(dialectType).get(0).getName());
				httpNodeData.setOutputs(HttpNodeData.getDefaultOutputSchemas(dialectType));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
