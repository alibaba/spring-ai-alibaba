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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author yingzi
 * @date 2025/6/4 16:11
 */
@AutoConfiguration
@EnableConfigurationProperties({ Nacos2McpSseClientProperties.class, NacosMcpProperties.class })
public class Nacos2McpAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Nacos2McpAutoConfiguration.class);

	public Nacos2McpAutoConfiguration() {
	}

	@Bean(name = "namespace2NamingService")
	public Map<String, NamingService> namespace2NamingService(NacosMcpProperties nacosMcpProperties,
			Nacos2McpSseClientProperties nacos2McpSseClientProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		Map<String, NamingService> namespace2NamingService = new HashMap<>();
		for (Nacos2McpSseClientProperties.NacosSseParameters nacosSseParameters : nacos2McpSseClientProperties
			.getConnections()
			.values()) {
			nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosSseParameters.serviceNamespace());
			try {
				NamingService namingService = NamingFactory.createNamingService(nacosProperties);
				namespace2NamingService.put(nacosSseParameters.serviceNamespace(), namingService);
			}
			catch (NacosException e) {
				logger.warn("nacos naming service: {} error", nacosSseParameters.serviceName(), e);
			}
		}
		return namespace2NamingService;
	}

	@Bean(name = "namespace2NacosConfigService")
	public Map<String, NacosConfigService> namespace2NacosConfigService(NacosMcpProperties nacosMcpProperties,
			Nacos2McpSseClientProperties nacos2McpSseClientProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		Map<String, NacosConfigService> namespace2NacosConfigService = new HashMap<>();
		for (Nacos2McpSseClientProperties.NacosSseParameters nacosSseParameters : nacos2McpSseClientProperties
			.getConnections()
			.values()) {
			nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosSseParameters.serviceNamespace());
			try {
				NacosConfigService nacosConfigService = new NacosConfigService(nacosProperties);
				namespace2NacosConfigService.put(nacosSseParameters.serviceNamespace(), nacosConfigService);
			}
			catch (NacosException e) {
				logger.warn("nacos naming service: {} error", nacosSseParameters.serviceName(), e);
			}
		}
		return namespace2NacosConfigService;
	}

}
