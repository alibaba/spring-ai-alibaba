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

package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.TimeoutConfig;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The data model of the HTTP node, which contains all the configurable items of the
 * Builder.。
 */
public class HttpNodeData extends NodeData {

	/** HTTP method, default GET */
	private HttpMethod method = HttpMethod.GET;

	/** Request URL */
	private String url;

	/** Request header */
	private Map<String, String> headers = Collections.emptyMap();

	/** queryParams */
	private Map<String, String> queryParams = Collections.emptyMap();

	/** body */
	private HttpRequestNodeBody body = new HttpRequestNodeBody();

	/** authConfig */
	private AuthConfig authConfig;

	/** retryConfig */
	private RetryConfig retryConfig = new RetryConfig(3, 1000, true);

	/** TimeoutConfig */
	private TimeoutConfig timeoutConfig;

	/** outputKey */
	private String outputKey;

	public HttpNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs,
			HttpMethod method, String url, Map<String, String> headers, Map<String, String> queryParams,
			HttpRequestNodeBody body, AuthConfig authConfig, RetryConfig retryConfig, TimeoutConfig timeoutConfig,
			String outputKey) {
		super(inputs, outputs);
		this.method = method;
		this.url = url;
		this.headers = headers != null ? headers : Collections.emptyMap();
		this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
		this.body = body != null ? body : new HttpRequestNodeBody();
		this.authConfig = authConfig;
		this.retryConfig = retryConfig != null ? retryConfig : new RetryConfig(3, 1000, true);
		this.timeoutConfig = timeoutConfig;
		this.outputKey = outputKey;
	}

	public HttpNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
		this(inputs, outputs, HttpMethod.GET, null, Collections.emptyMap(), Collections.emptyMap(),
				new HttpRequestNodeBody(), null, new RetryConfig(3, 1000, true),
				new TimeoutConfig(10, 60, 20, 300, 600, 6000), null);
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}

	public HttpRequestNodeBody getBody() {
		return body;
	}

	public void setBody(HttpRequestNodeBody body) {
		this.body = body;
	}

	public AuthConfig getAuthConfig() {
		return authConfig;
	}

	public void setAuthConfig(AuthConfig authConfig) {
		this.authConfig = authConfig;
	}

	public RetryConfig getRetryConfig() {
		return retryConfig;
	}

	public void setRetryConfig(RetryConfig retryConfig) {
		this.retryConfig = retryConfig;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
