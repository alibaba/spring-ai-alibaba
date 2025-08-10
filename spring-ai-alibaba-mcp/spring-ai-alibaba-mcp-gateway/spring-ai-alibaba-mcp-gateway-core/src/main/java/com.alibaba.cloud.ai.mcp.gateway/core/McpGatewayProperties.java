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
package com.alibaba.cloud.ai.mcp.gateway.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author aias00
 */
@ConfigurationProperties(prefix = McpGatewayProperties.CONFIG_PREFIX)
public class McpGatewayProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.gateway";

	private Boolean enabled = true;

	private String registry = "nacos";

	private String messageEndpoint = "/message";

	private SseConfig sse = new SseConfig();

	private StreamableConfig streamable = new StreamableConfig();

	public static class SseConfig {

		private Boolean enabled = true; // 默认启用，保持向后兼容

		private String endpoint = "/sse";

		private String protocolVersion = "2025-03-26";

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public void setProtocolVersion(String protocolVersion) {
			this.protocolVersion = protocolVersion;
		}

	}

	public static class StreamableConfig {

		private Boolean enabled = false;

		private String endpoint = "/streamable";

		private String protocolVersion = "2025-06-18";

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getProtocolVersion() {
			return protocolVersion;
		}

		public void setProtocolVersion(String protocolVersion) {
			this.protocolVersion = protocolVersion;
		}

	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRegistry() {
		return registry;
	}

	public void setRegistry(final String registry) {
		this.registry = registry;
	}

	public String getMessageEndpoint() {
		return messageEndpoint;
	}

	public void setMessageEndpoint(String messageEndpoint) {
		this.messageEndpoint = messageEndpoint;
	}

	public SseConfig getSse() {
		return sse;
	}

	public void setSse(SseConfig sse) {
		this.sse = sse;
	}

	public StreamableConfig getStreamable() {
		return streamable;
	}

	public void setStreamable(StreamableConfig streamable) {
		this.streamable = streamable;
	}

}
