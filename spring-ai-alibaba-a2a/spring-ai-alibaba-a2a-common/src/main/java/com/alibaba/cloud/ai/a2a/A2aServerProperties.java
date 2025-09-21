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

import com.alibaba.cloud.ai.a2a.constants.A2aConstants;
import com.alibaba.cloud.ai.a2a.route.JsonRpcA2aRouterProvider;
import com.alibaba.cloud.ai.a2a.utils.InetUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Properties for A2A protocol
 *
 * @author xiweng.yy
 */
@ConfigurationProperties(prefix = A2aServerProperties.CONFIG_PREFIX)
public class A2aServerProperties implements EnvironmentAware {

	private static final Logger log = LoggerFactory.getLogger(A2aServerProperties.class);

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.a2a.server";

	private String type = A2aConstants.AGENT_TRANSPORT_TYPE_JSON_RPC;

	private String agentCardUrl = JsonRpcA2aRouterProvider.DEFAULT_WELL_KNOWN_URL;

	private String messageUrl = JsonRpcA2aRouterProvider.DEFAULT_MESSAGE_URL;

	private String address;

	private Integer port;

	private String version = A2aConstants.DEFAULT_AGENT_VERSION;

	private Environment environment;

	@PostConstruct
	public void init() {
		if (!StringUtils.hasLength(this.getAddress())) {
			String addressFromServerAddress = environment.resolvePlaceholders("${server.address:}");
			if (StringUtils.hasLength(addressFromServerAddress)) {
				setAddress(addressFromServerAddress);
			}
			else {
				setAddress(InetUtils.findFirstNonLoopbackIpv4Address().getHostAddress());
			}
		}
		if (null == this.getPort()) {
			String portFromServerPort = environment.resolvePlaceholders("${server.port:}");
			if (StringUtils.hasLength(portFromServerPort)) {
				setPort(Integer.parseInt(portFromServerPort));
			}
		}
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAgentCardUrl() {
		return agentCardUrl;
	}

	public void setAgentCardUrl(String agentCardUrl) {
		this.agentCardUrl = agentCardUrl;
	}

	public String getMessageUrl() {
		return messageUrl;
	}

	public void setMessageUrl(String messageUrl) {
		this.messageUrl = messageUrl;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

}
