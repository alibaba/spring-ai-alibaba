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

package com.alibaba.cloud.ai.autoconfigure.mcp.router;

import com.alibaba.cloud.ai.mcp.router.config.McpRouterProperties;
import com.alibaba.cloud.ai.mcp.router.config.DbMcpProperties;
import com.alibaba.cloud.ai.mcp.router.core.discovery.McpServiceDiscovery;
import com.alibaba.cloud.ai.mcp.router.core.discovery.DbMcpServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author digitzh
 */
@Configuration
@EnableConfigurationProperties({ McpRouterProperties.class, DbMcpProperties.class })
@ConditionalOnExpression("$" + "{" + McpRouterProperties.CONFIG_PREFIX + ".enabled:true} == true " + "and '$" + "{"
		+ McpRouterProperties.CONFIG_PREFIX + ".discovery-type}' == 'database'")
public class DbMcpRouterAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DbMcpRouterAutoConfiguration.class);

	@Bean
	public McpServiceDiscovery mySqlMcpServiceDiscovery(DbMcpProperties dbMcpProperties) {
		log.info("Creating DB MCP service discovery with configuration: {}", dbMcpProperties);
		return new DbMcpServiceDiscovery(dbMcpProperties);
	}

}
