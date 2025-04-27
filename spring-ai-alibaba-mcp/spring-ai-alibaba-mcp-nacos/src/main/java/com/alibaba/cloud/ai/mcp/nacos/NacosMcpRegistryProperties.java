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

import com.alibaba.nacos.api.utils.NetUtils;
import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT_PORT;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
import static java.util.Collections.unmodifiableMap;

/**
 * @author Sunrisea
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.nacos")
public class NacosMcpRegistryProperties {

	public static final String DEFAULT_NAMESPACE = "public";

	public static final String DEFAULT_ADDRESS = "127.0.0.1:8848";

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.nacos";

	private static final Pattern PATTERN = Pattern.compile("-(\\w)");

	private static final Logger log = LoggerFactory.getLogger(NacosMcpRegistryProperties.class);

	String serverAddr;

	String serviceNamespace;

	String serviceGroup = "DEFAULT_GROUP";

	String username;

	String password;

	String accessKey;

	String secretKey;

	String endpoint;

	String ip;

	String sseExportContextPath;

	boolean serviceRegister = true;

	@Autowired
	@JsonIgnore
	private Environment environment;

	public boolean isServiceRegister() {
		return serviceRegister;
	}

	public void setServiceRegister(boolean serviceRegister) {
		this.serviceRegister = serviceRegister;
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	String getServerAddr() {
		return serverAddr;
	}

	void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	String getServiceNamespace() {
		return serviceNamespace;
	}

	void setServiceNamespace(String serviceNamespace) {
		this.serviceNamespace = serviceNamespace;
	}

	@PostConstruct
	public void init() throws Exception {
		if (StringUtils.isEmpty(this.ip)) {
			this.ip = NetUtils.localIP();
		}
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
		Properties properties = new Properties();
		properties.put(SERVER_ADDR, Objects.toString(this.serverAddr, ""));
		properties.put(USERNAME, Objects.toString(this.username, ""));
		properties.put(PASSWORD, Objects.toString(this.password, ""));
		properties.put(NAMESPACE, this.resolveNamespace());
		properties.put(ACCESS_KEY, Objects.toString(this.accessKey, ""));
		properties.put(SECRET_KEY, Objects.toString(this.secretKey, ""));
		String endpoint = Objects.toString(this.endpoint, "");
		if (endpoint.contains(":")) {
			int index = endpoint.indexOf(":");
			properties.put(ENDPOINT, endpoint.substring(0, index));
			properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
		}
		else {
			properties.put(ENDPOINT, endpoint);
		}

		enrichNacosConfigProperties(properties);

		if (StringUtils.isEmpty(this.serverAddr) && StringUtils.isEmpty(this.endpoint)) {
			properties.put(SERVER_ADDR, DEFAULT_ADDRESS);
		}

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

	protected void enrichNacosConfigProperties(Properties nacosConfigProperties) {
		if (environment == null) {
			return;
		}
		String prefix = "spring.ai.alibaba.mcp.nacos";

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
		Map<String, Object> properties = getSubProperties(env.getPropertySources(), env, prefix);
		properties.forEach((k, v) -> nacosConfigProperties.putIfAbsent(resolveKey(k), String.valueOf(v)));
	}

	protected String resolveKey(String key) {
		Matcher matcher = PATTERN.matcher(key);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private Map<String, Object> getSubProperties(PropertySources propertySources, PropertyResolver propertyResolver,
			String prefix) {

		Map<String, Object> subProperties = new LinkedHashMap<String, Object>();

		for (PropertySource<?> source : propertySources) {
			for (String name : getPropertyNames(source)) {
				if (!subProperties.containsKey(name) && name.startsWith(prefix)) {
					String subName = name.substring(prefix.length());
					if (!subProperties.containsKey(subName)) { // take first one
						Object value = source.getProperty(name);
						if (value instanceof String) {
							value = propertyResolver.resolvePlaceholders((String) value);
						}
						subProperties.put(subName, value);
					}
				}
			}
		}
		return unmodifiableMap(subProperties);
	}

	private String[] getPropertyNames(PropertySource propertySource) {

		String[] propertyNames = propertySource instanceof EnumerablePropertySource
				? ((EnumerablePropertySource<?>) propertySource).getPropertyNames() : null;

		if (propertyNames == null) {
			return new String[0];
		}
		return propertyNames;
	}

}
