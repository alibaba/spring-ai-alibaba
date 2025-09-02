/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.autoconfigure.a2a.server;

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.route.JsonRpcA2aRouterProvider;
import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * The AutoConfiguration for A2A server.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = A2aServerHandlerAutoConfiguration.class)
@EnableConfigurationProperties({ A2aServerProperties.class })
public class A2aServerAutoConfiguration {

	@Bean
	@ConditionalOnBean({ JsonRpcA2aRequestHandler.class })
	public RouterFunction<ServerResponse> a2aRouterFunction(A2aServerProperties a2aServerProperties,
			JsonRpcA2aRequestHandler a2aRequestHandler) {
		return new JsonRpcA2aRouterProvider(a2aServerProperties.getAgentCardUrl(), a2aServerProperties.getMessageUrl())
			.getRouter(a2aRequestHandler);
	}

}
