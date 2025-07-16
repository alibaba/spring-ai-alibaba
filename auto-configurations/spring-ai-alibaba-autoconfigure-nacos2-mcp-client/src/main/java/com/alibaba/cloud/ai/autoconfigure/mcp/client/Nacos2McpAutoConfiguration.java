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
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * @author yingzi
 * @since 2025/6/4 16:11
 */
@AutoConfiguration
@EnableConfigurationProperties({ Nacos2McpSseClientProperties.class, NacosMcpProperties.class })
public class Nacos2McpAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Nacos2McpAutoConfiguration.class);

	public Nacos2McpAutoConfiguration() {
	}

	@Bean
	public NamingService namespace2NamingService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return NamingFactory.createNamingService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public NacosConfigService namespace2NacosConfigService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosConfigService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
