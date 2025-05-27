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
package com.alibaba.cloud.ai.mcp.nacos.registry;

import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

/**
 * @author Sunrisea
 */
@ConfigurationProperties(prefix = NacosMcpRegistryProperties.CONFIG_PREFIX)
public class NacosMcpRegistryProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos.registry";

	String serviceGroup = "DEFAULT_GROUP";

	String serviceName;

	String sseExportContextPath;

	boolean serviceRegister = true;

	boolean serviceEphemeral = true;

	@Autowired
	@JsonIgnore
	private Environment environment;

	public boolean isServiceRegister() {
		return serviceRegister;
	}

	public void setServiceRegister(boolean serviceRegister) {
		this.serviceRegister = serviceRegister;
	}

	public boolean isServiceEphemeral() {
		return serviceEphemeral;
	}

	public void setServiceEphemeral(boolean serviceEphemeral) {
		this.serviceEphemeral = serviceEphemeral;
	}

	public String getSseExportContextPath() {
		return sseExportContextPath;
	}

	public void setSseExportContextPath(String sseExportContextPath) {
		this.sseExportContextPath = sseExportContextPath;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@PostConstruct
	public void init() throws Exception {
		if (StringUtils.isBlank(this.sseExportContextPath)) {
			String path = environment.getProperty("server.servlet.context-path");
			if (!StringUtils.isBlank(path)) {
				this.sseExportContextPath = path;
			}
		}
	}

}
