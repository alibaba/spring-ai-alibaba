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

package com.alibaba.cloud.ai.mcp.router.config;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.router.OpenMeteoService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Properties;

/**
 * MCP Router 配置类
 */
@Configuration
@EnableAutoConfiguration
// @EnableWebMvc
@ComponentScan(basePackages = "com.alibaba.cloud.ai.mcp")
@EnableConfigurationProperties({ NacosMcpProperties.class })
@AutoConfiguration(after = { McpServerAutoConfiguration.class })
public class McpRouterConfig {

	// 配置可以在这里添加
	@Bean
	public NacosMcpOperationService nacosMcpOperationService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosMcpOperationService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
		return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
	}

}
