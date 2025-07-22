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

package com.alibaba.cloud.ai.autoconfigure.mcp.gateway;

import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayProperties;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolsInitializer;
import com.alibaba.cloud.ai.mcp.gateway.core.utils.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;

/**
 * @author aias00
 */
@EnableConfigurationProperties({ McpServerProperties.class, McpGatewayProperties.class })
@AutoConfiguration(after = { McpServerAutoConfiguration.class })
@ConditionalOnProperty(prefix = McpGatewayProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class McpGatewayAutoConfiguration implements ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(McpGatewayAutoConfiguration.class);

	@Override
	public void setApplicationContext(@NonNull final ApplicationContext applicationContext) throws BeansException {
		SpringBeanUtils.getInstance().setApplicationContext(applicationContext);
	}

	@Bean
	public ToolCallbackProvider callbackProvider(final McpGatewayToolsInitializer toolsInitializer) {
		return McpGatewayToolCallbackProvider.builder().toolCallbacks(toolsInitializer.initializeTools()).build();
	}

}
