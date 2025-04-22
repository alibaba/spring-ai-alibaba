/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mcp.dynamic.server.definiation;

import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicNacosToolDefinition implements ToolDefinition {

	private String name;

	private String description;

	private String requestMethod;

	private String requestPath;

	private Object inputSchema;

	public DynamicNacosToolDefinition() {
	}

	public DynamicNacosToolDefinition(final String name, final String description, final String requestMethod,
			final String requestPath, final String inputSchema) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
		this.requestMethod = StringUtils.isNoneBlank(requestMethod) ? requestMethod : "GET";
		this.requestPath = StringUtils.isNoneBlank(requestPath) ? requestPath : name;
	}

	public static DynamicNacosToolDefinition.Builder builder() {
		return new DynamicNacosToolDefinition.Builder();
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

	public static final class Builder {

		private String name;

		private String description;

		private String requestMethod;

		private String requestPath;

		private String inputSchema;

		private Builder() {
		}

		public DynamicNacosToolDefinition.Builder name(final String name) {
			this.name = name;
			return this;
		}

		public DynamicNacosToolDefinition.Builder description(final String description) {
			this.description = description;
			return this;
		}

		public DynamicNacosToolDefinition.Builder requestMethod(final String requestMethod) {
			this.requestMethod = requestMethod;
			return this;
		}

		public DynamicNacosToolDefinition.Builder requestPath(final String requestPath) {
			this.requestPath = requestPath;
			return this;
		}

		public DynamicNacosToolDefinition.Builder inputSchema(final String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public ToolDefinition build() {
			if (!StringUtils.isNoneBlank(this.description)) {
				this.description = ToolUtils.getToolDescriptionFromName(this.name);
			}

			return new DynamicNacosToolDefinition(this.name, this.description, this.requestMethod, this.requestPath,
					this.inputSchema);
		}

	}

}
