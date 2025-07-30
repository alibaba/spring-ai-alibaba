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

package com.alibaba.cloud.ai.autoconfigure.mcp.tracing;

import com.alibaba.cloud.ai.mcp.common.tracing.McpTraceExchangeFilterFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@AutoConfiguration
@ConditionalOnClass(ExchangeFilterFunction.class)
public class McpTracingAutoConfiguration {

	@Autowired
	private ApplicationContext applicationContext;

	@Bean(name = "mcpTraceExchangeFilterFunction")
	@ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
	public McpTraceExchangeFilterFunction mcpTraceExchangeFilterFunction() {
		try {

			Object tracer = applicationContext.getBean("tracer");
			return new McpTraceExchangeFilterFunction(tracer);
		}
		catch (Exception e) {

			try {
				Class<?> tracerClass = Class.forName("io.micrometer.tracing.Tracer");
				Object tracer = applicationContext.getBean(tracerClass);
				return new McpTraceExchangeFilterFunction(tracer);
			}
			catch (Exception ex) {
				throw new RuntimeException("Failed to create MCP trace filter: Tracer bean not found", ex);
			}
		}
	}

}
