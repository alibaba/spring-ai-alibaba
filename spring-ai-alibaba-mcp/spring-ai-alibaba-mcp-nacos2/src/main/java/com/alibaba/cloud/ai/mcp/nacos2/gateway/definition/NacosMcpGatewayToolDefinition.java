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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.definition;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NacosMcpGatewayToolDefinition implements ToolDefinition {

	private String name;

	private String description;

	private String serviceName;

	private String requestMethod;

	private String requestPath;

	private Object inputSchema;

	public NacosMcpGatewayToolDefinition() {
	}

	public NacosMcpGatewayToolDefinition(final String name, final String description, final String serviceName,
			final String requestMethod, final String requestPath, final String inputSchema) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.serviceName = serviceName;
		this.inputSchema = inputSchema;
		this.requestMethod = StringUtils.isNoneBlank(requestMethod) ? requestMethod : "GET";
		this.requestPath = StringUtils.isNoneBlank(requestPath) ? requestPath : name;
	}

	public static NacosMcpGatewayToolDefinition.Builder builder() {
		return new NacosMcpGatewayToolDefinition.Builder();
	}

	public String name() {
		return this.name;
	}

	public String description() {
		return this.description;
	}

	public String inputSchema() {
		return JacksonUtils.toJson(this.inputSchema);
	}

	public String requestMethod() {
		return this.requestMethod;
	}

	public String requestPath() {
		return this.requestPath;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(final String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public void setRequestPath(final String requestPath) {
		this.requestPath = requestPath;
	}

	public Object getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(final Object inputSchema) {
		this.inputSchema = inputSchema;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public static final class Builder {

		private String name;

		private String description;

		private String serviceName;

		private String requestMethod;

		private String requestPath;

		private String inputSchema;

		private Builder() {
		}

		public NacosMcpGatewayToolDefinition.Builder name(final String name) {
			this.name = name;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder description(final String description) {
			this.description = description;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder serviceName(final String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder requestMethod(final String requestMethod) {
			this.requestMethod = requestMethod;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder requestPath(final String requestPath) {
			this.requestPath = requestPath;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder inputSchema(final String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public ToolDefinition build() {
			if (!StringUtils.isNoneBlank(this.description)) {
				this.description = ToolUtils.getToolDescriptionFromName(this.name);
			}

			return new NacosMcpGatewayToolDefinition(this.name, this.description, this.serviceName, this.requestMethod,
					this.requestPath, this.inputSchema);
		}

	}

}
