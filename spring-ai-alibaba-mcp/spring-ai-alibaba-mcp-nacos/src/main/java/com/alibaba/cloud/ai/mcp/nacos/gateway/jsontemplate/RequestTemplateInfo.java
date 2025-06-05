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

package com.alibaba.cloud.ai.mcp.nacos.gateway.jsontemplate;

import com.fasterxml.jackson.databind.JsonNode;

public class RequestTemplateInfo {

	public String url;

	public String method;

	public boolean argsToUrlParam;

	public boolean argsToJsonBody;

	public boolean argsToFormBody;

	public JsonNode headers;

	public JsonNode body;

	public JsonNode rawNode;

	public RequestTemplateInfo(String url, String method, boolean argsToUrlParam, boolean argsToJsonBody,
			boolean argsToFormBody, JsonNode headers, JsonNode body, JsonNode rawNode) {
		this.url = url;
		this.method = method;
		this.argsToUrlParam = argsToUrlParam;
		this.argsToJsonBody = argsToJsonBody;
		this.argsToFormBody = argsToFormBody;
		this.headers = headers;
		this.body = body;
		this.rawNode = rawNode;
	}

}
