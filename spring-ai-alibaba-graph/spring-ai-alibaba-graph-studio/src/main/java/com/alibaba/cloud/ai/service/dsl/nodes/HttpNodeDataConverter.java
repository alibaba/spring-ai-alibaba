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

package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.HttpNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		return Stream.of(HttpNodeDialectConverter.DIFY, HttpNodeDialectConverter.CUSTOM)
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
			public HttpNodeData parse(Map<String, Object> data) {
				// variable_selector -> inputs
				List<VariableSelector> inputs = Optional.ofNullable((List<String>) data.get("variable_selector"))
					.filter(list -> list.size() == 2)
					.map(list -> Collections.singletonList(new VariableSelector(list.get(0), list.get(1))))
					.orElse(Collections.emptyList());

				List<com.alibaba.cloud.ai.model.Variable> outputs = List.of();

				HttpMethod method = data.containsKey("method")
						? HttpMethod.valueOf(((String) data.get("method")).toUpperCase()) : HttpMethod.GET;
				String url = (String) data.get("url");

				// headers & query_params
				Map<String, String> headers = (Map<String, String>) (Map<?, ?>) data.getOrDefault("headers",
						Collections.emptyMap());
				Map<String, String> queryParams = (Map<String, String>) (Map<?, ?>) data.getOrDefault("query_params",
						Collections.emptyMap());

				// body
				Object rawBody = data.get("body");
				HttpRequestNodeBody body = HttpRequestNodeBody.from(rawBody);

				// auth
				AuthConfig auth = null;
				if (data.containsKey("auth")) {
					Map<String, Object> am = (Map<String, Object>) data.get("auth");
					String type = ((String) am.get("type")).toUpperCase();
					if ("BASIC".equals(type)) {
						auth = AuthConfig.basic((String) am.get("username"), (String) am.get("password"));
					}
					else if ("BEARER".equals(type)) {
						auth = AuthConfig.bearer((String) am.get("token"));
					}
				}

				// retry_config
				Map<String, Object> rcMap = (Map<String, Object>) data.get("retry_config");
				int maxRetries = rcMap != null && rcMap.get("max_retries") != null
						? ((Number) rcMap.get("max_retries")).intValue() : 3;
				long maxRetryInterval = rcMap != null && rcMap.get("max_retry_interval") != null
						? ((Number) rcMap.get("max_retry_interval")).longValue() : 1000L;
				boolean enable = rcMap != null && rcMap.get("enable") != null ? (Boolean) rcMap.get("enable") : true;
				RetryConfig retryConfig = new RetryConfig(maxRetries, maxRetryInterval, enable);

				// output_key
				String outputKey = (String) data.get("output_key");

				return new HttpNodeData(inputs, outputs, method, url, headers, queryParams, body, auth, retryConfig,
						outputKey);
			}

			@Override
			public Map<String, Object> dump(HttpNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				// variable_selector
				if (!nd.getInputs().isEmpty()) {
					VariableSelector sel = nd.getInputs().get(0);
					m.put("variable_selector", List.of(sel.getNamespace(), sel.getName()));
				}
				// method
				if (nd.getMethod() != HttpMethod.GET) {
					m.put("method", nd.getMethod().name().toLowerCase());
				}
				// url
				if (nd.getUrl() != null) {
					m.put("url", nd.getUrl());
				}
				// headers
				if (!nd.getHeaders().isEmpty()) {
					m.put("headers", nd.getHeaders());
				}
				// query_params
				if (!nd.getQueryParams().isEmpty()) {
					m.put("query_params", nd.getQueryParams());
				}
				// body
				HttpRequestNodeBody body = nd.getBody();
				if (body != null && body.getType() != null) {
					m.put("body", body);
				}
				// auth
				AuthConfig ac = nd.getAuthConfig();
				if (ac != null) {
					Map<String, Object> am = new LinkedHashMap<>();
					am.put("type", ac.getTypeName());
					if (ac.isBasic()) {
						am.put("username", ac.getUsername());
						am.put("password", ac.getPassword());
					}
					else if (ac.isBearer()) {
						am.put("token", ac.getToken());
					}
					m.put("auth", am);
				}
				// retry_config
				RetryConfig rc = nd.getRetryConfig();
				if (rc != null) {
					Map<String, Object> rm = new LinkedHashMap<>();
					rm.put("max_retries", rc.getMaxRetries());
					rm.put("max_retry_interval", rc.getMaxRetryInterval());
					rm.put("enable", rc.isEnable());
					m.put("retry_config", rm);
				}
				// output_key
				if (nd.getOutputKey() != null) {
					m.put("output_key", nd.getOutputKey());
				}
				return m;
			}
		}), CUSTOM(defaultCustomDialectConverter(HttpNodeData.class));

		private final DialectConverter<HttpNodeData> converter;

		HttpNodeDialectConverter(DialectConverter<HttpNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<HttpNodeData> dialectConverter() {
			return this.converter;
		}

	}

}
