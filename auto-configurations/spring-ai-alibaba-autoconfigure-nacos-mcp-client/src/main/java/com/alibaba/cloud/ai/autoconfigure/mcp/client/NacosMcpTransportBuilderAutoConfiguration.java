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

import com.alibaba.cloud.ai.mcp.discovery.client.builder.WebFluxSseClientTransportBuilder;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * @author Sunrisea
 * @since 2025/7/4 11:16
 */
@AutoConfiguration(before = { NacosMcpClientAutoConfiguration.class })
@ConditionalOnClass(WebFluxSseClientTransport.class)
public class NacosMcpTransportBuilderAutoConfiguration {

	@Bean
	public WebFluxSseClientTransportBuilder webFluxSseClientTransportBuilder() {
		return new WebFluxSseClientTransportBuilder();
	}

}
