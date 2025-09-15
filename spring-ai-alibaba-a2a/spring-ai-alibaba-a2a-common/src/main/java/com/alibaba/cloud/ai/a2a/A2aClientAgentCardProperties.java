/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A2a client agent card properties.
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = A2aClientAgentCardProperties.CONFIG_PREFIX)
public class A2aClientAgentCardProperties extends A2aAgentCardProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.a2a.client.card";

	private String version;

	private String preferredTransport;

	private String protocolVersion;

	private String wellKnownUrl;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPreferredTransport() {
		return preferredTransport;
	}

	public void setPreferredTransport(String preferredTransport) {
		this.preferredTransport = preferredTransport;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String getWellKnownUrl() {
		return wellKnownUrl;
	}

	public void setWellKnownUrl(String wellKnownUrl) {
		this.wellKnownUrl = wellKnownUrl;
	}

}
