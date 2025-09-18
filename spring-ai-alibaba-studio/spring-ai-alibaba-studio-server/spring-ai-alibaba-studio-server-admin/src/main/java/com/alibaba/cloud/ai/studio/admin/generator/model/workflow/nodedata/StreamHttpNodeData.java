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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

public class StreamHttpNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("response", VariableType.ARRAY_STRING);
	}

	// HTTP request configuration
	private String method = "GET";

	private String url;

	private Map<String, String> headers;

	private Map<String, Object> body;

	// Streaming configuration
	private String streamFormat = "SSE"; // SSE, JSON_LINES, TEXT_STREAM

	private String streamMode = "DISTRIBUTE"; // DISTRIBUTE, AGGREGATE

	private String delimiter = "\n";

	private String outputKey;

	private Integer timeout = 30000; // 30 seconds default

	// Authentication configuration (if needed)
	private String authorization;

	private String authType; // BEARER, BASIC, API_KEY

	public StreamHttpNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public StreamHttpNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getMethod() {
		return method;
	}

	public StreamHttpNodeData setMethod(String method) {
		this.method = method;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public StreamHttpNodeData setUrl(String url) {
		this.url = url;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public StreamHttpNodeData setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, Object> getBody() {
		return body;
	}

	public StreamHttpNodeData setBody(Map<String, Object> body) {
		this.body = body;
		return this;
	}

	public String getStreamFormat() {
		return streamFormat;
	}

	public StreamHttpNodeData setStreamFormat(String streamFormat) {
		this.streamFormat = streamFormat;
		return this;
	}

	public String getStreamMode() {
		return streamMode;
	}

	public StreamHttpNodeData setStreamMode(String streamMode) {
		this.streamMode = streamMode;
		return this;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public StreamHttpNodeData setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public StreamHttpNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public StreamHttpNodeData setTimeout(Integer timeout) {
		this.timeout = timeout;
		return this;
	}

	public String getAuthorization() {
		return authorization;
	}

	public StreamHttpNodeData setAuthorization(String authorization) {
		this.authorization = authorization;
		return this;
	}

	public String getAuthType() {
		return authType;
	}

	public StreamHttpNodeData setAuthType(String authType) {
		this.authType = authType;
		return this;
	}

}
