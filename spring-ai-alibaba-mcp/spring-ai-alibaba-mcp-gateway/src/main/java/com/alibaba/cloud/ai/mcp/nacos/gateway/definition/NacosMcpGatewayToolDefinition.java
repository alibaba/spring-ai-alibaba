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

package com.alibaba.cloud.ai.mcp.nacos.gateway.definition;

import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
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

	private String version;

	private String protocol;

	private McpServerRemoteServiceConfig remoteServerConfig;

	private Boolean enabled;

	private Object inputSchema;

	private McpToolMeta toolMeta;

	public NacosMcpGatewayToolDefinition() {
	}

	public NacosMcpGatewayToolDefinition(final String name, final String description, final String inputSchema) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
	}

	public NacosMcpGatewayToolDefinition(final String name, final String description, final Object inputSchema,
			final String version, final String protocol, final McpServerRemoteServiceConfig remoteServerConfig,
			final McpToolMeta toolMeta, final Boolean enabled) {
		Assert.hasText(name, "name cannot be null or empty");
		Assert.hasText(description, "description cannot be null or empty");
		Assert.notNull(inputSchema, "inputSchema cannot be null or empty");
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
		this.version = version;
		this.protocol = protocol;
		this.remoteServerConfig = remoteServerConfig;
		this.toolMeta = toolMeta;
		this.enabled = enabled;
	}

	public static NacosMcpGatewayToolDefinition.Builder builder() {
		return new NacosMcpGatewayToolDefinition.Builder();
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String description() {
		return this.description;
	}

	@Override
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

	public McpServerRemoteServiceConfig getRemoteServerConfig() {
		return remoteServerConfig;
	}

	public void setRemoteServerConfig(final McpServerRemoteServiceConfig remoteServerConfig) {
		this.remoteServerConfig = remoteServerConfig;
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

	public McpToolMeta getToolMeta() {
		return toolMeta;
	}

	public void setToolMeta(final McpToolMeta toolMeta) {
		this.toolMeta = toolMeta;
	}

	public static final class Builder {

		private String name;

		private String description;

		private String version;

		private String protocol;

		private McpServerRemoteServiceConfig remoteServerConfig;

		private Boolean enabled;

		private Object inputSchema;

		private McpToolMeta toolsMeta;

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

		public NacosMcpGatewayToolDefinition.Builder version(final String version) {
			this.version = version;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder remoteServerConfig(
				final McpServerRemoteServiceConfig remoteServerConfig) {
			this.remoteServerConfig = remoteServerConfig;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder enabled(final Boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder protocol(final String protocol) {
			this.protocol = protocol;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder inputSchema(final Object inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public NacosMcpGatewayToolDefinition.Builder toolsMeta(final McpToolMeta toolsMeta) {
			this.toolsMeta = toolsMeta;
			return this;
		}

		public NacosMcpGatewayToolDefinition build() {
			if (!StringUtils.isNoneBlank(this.description)) {
				this.description = ToolUtils.getToolDescriptionFromName(this.name);
			}

			return new NacosMcpGatewayToolDefinition(this.name, this.description, this.inputSchema, this.version,
					this.protocol, this.remoteServerConfig, this.toolsMeta, this.enabled);
		}

	}

}
