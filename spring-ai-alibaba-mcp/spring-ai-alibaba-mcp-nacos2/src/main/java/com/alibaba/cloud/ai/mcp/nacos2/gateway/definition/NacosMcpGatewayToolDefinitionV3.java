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
public class NacosMcpGatewayToolDefinitionV3 implements ToolDefinition {

	private String name;

	private String description;

	private String version;

	private String protocol;

	private Object remoteServerConfig;

	private Object localServerConfig;

	private Object credentials;

	private Boolean enabled;

	private Object inputSchema;

	private Object toolsMeta;

	public NacosMcpGatewayToolDefinitionV3() {
	}

	public NacosMcpGatewayToolDefinitionV3(final String name, final String description, final String inputSchema) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
	}

	public NacosMcpGatewayToolDefinitionV3(final String name, final String description, final Object inputSchema,
			final String version, final String protocol, final Object remoteServerConfig,
			final Object localServerConfig, final Object credentials, final Object toolsMeta, final Boolean enabled) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.notNull(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
		this.version = version;
		this.protocol = protocol;
		this.remoteServerConfig = remoteServerConfig;
		this.localServerConfig = localServerConfig;
		this.credentials = credentials;
		this.toolsMeta = toolsMeta;
		this.enabled = enabled;
	}

	public static NacosMcpGatewayToolDefinitionV3.Builder builder() {
		return new NacosMcpGatewayToolDefinitionV3.Builder();
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

	public Object getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(final Object inputSchema) {
		this.inputSchema = inputSchema;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public Object getRemoteServerConfig() {
		return remoteServerConfig;
	}

	public void setRemoteServerConfig(final Object remoteServerConfig) {
		this.remoteServerConfig = remoteServerConfig;
	}

	public Object getLocalServerConfig() {
		return localServerConfig;
	}

	public void setLocalServerConfig(final Object localServerConfig) {
		this.localServerConfig = localServerConfig;
	}

	public Object getCredentials() {
		return credentials;
	}

	public void setCredentials(final Object credentials) {
		this.credentials = credentials;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}

	public Object getToolsMeta() {
		return toolsMeta;
	}

	public void setToolsMeta(final Object toolsMeta) {
		this.toolsMeta = toolsMeta;
	}

	public static final class Builder {

		private String name;

		private String description;

		private String version;

		private String protocol;

		private Object remoteServerConfig;

		private Object localServerConfig;

		private Object credentials;

		private Boolean enabled;

		private Object inputSchema;

		private Object toolsMeta;

		private Builder() {
		}

		public NacosMcpGatewayToolDefinitionV3.Builder name(final String name) {
			this.name = name;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder description(final String description) {
			this.description = description;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder version(final String version) {
			this.version = version;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder remoteServerConfig(final Object remoteServerConfig) {
			this.remoteServerConfig = remoteServerConfig;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder localServerConfig(final Object localServerConfig) {
			this.localServerConfig = localServerConfig;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder credentials(final Object credentials) {
			this.credentials = credentials;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder enabled(final Boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder protocol(final String protocol) {
			this.protocol = protocol;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder inputSchema(final Object inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public NacosMcpGatewayToolDefinitionV3.Builder toolsMeta(final Object toolsMeta) {
			this.toolsMeta = toolsMeta;
			return this;
		}

		public ToolDefinition build() {
			if (!StringUtils.isNoneBlank(this.description)) {
				this.description = ToolUtils.getToolDescriptionFromName(this.name);
			}

			return new NacosMcpGatewayToolDefinitionV3(this.name, this.description, this.inputSchema, this.version,
					this.protocol, this.remoteServerConfig, this.localServerConfig, this.credentials, this.toolsMeta,
					this.enabled);
		}

	}

}
