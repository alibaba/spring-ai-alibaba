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
package com.alibaba.cloud.ai.example.manus.config.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class SseParameters {

	@JsonProperty("base_uri")
	private String baseUri;

	@JsonProperty("headers")
	private Map<String, String> headers;

	@JsonProperty("uri_variables")
	private Map<String, String> uriVariables;

	public String getBaseUri() {
		return baseUri;
	}

	public SseParameters setBaseUri(String baseUri) {
		this.baseUri = baseUri;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public SseParameters setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, String> getUriVariables() {
		return uriVariables;
	}

	public SseParameters setUriVariables(Map<String, String> uriVariables) {
		this.uriVariables = uriVariables;
		return this;
	}

}
