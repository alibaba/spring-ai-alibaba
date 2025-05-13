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
package com.alibaba.cloud.ai.mcp.nacos;

import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpProperties;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import java.util.Objects;
import java.util.Properties;

import static com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpProperties.CONFIG_PREFIX;

/**
 * @author Sunrisea
 */
@ConfigurationProperties(prefix = CONFIG_PREFIX + ".registry")
public class NacosMcpRegistryProperties extends NacosMcpProperties {

	String serviceNamespace;

	String serviceGroup = "DEFAULT_GROUP";

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

	public String getServiceNamespace() {
		return serviceNamespace;
	}

	void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}

	@PostConstruct
	public void init() throws Exception {
		super.init();
		if (StringUtils.isBlank(this.serviceNamespace)) {
			this.serviceNamespace = DEFAULT_NAMESPACE;
		}
		if (StringUtils.isBlank(this.sseExportContextPath)) {
			String path = environment.getProperty("server.servlet.context-path");
			if (!StringUtils.isBlank(path)) {
				this.sseExportContextPath = path;
			}
		}
	}

	public Properties getNacosProperties() {
		Properties properties = super.getNacosProperties();
		properties.put("groupName", Objects.toString(this.serviceGroup, "DEFAULT_GROUP"));
		properties.put(PropertyKeyConst.NAMESPACE, this.resolveNamespace());
		enrichNacosConfigProperties(properties);

		return properties;
	}

	private String resolveNamespace() {
		if (DEFAULT_NAMESPACE.equals(this.serviceNamespace)) {
			return "";
		}
		else {
			return Objects.toString(this.serviceNamespace, "");
		}
	}

}
