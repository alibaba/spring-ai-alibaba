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

import com.alibaba.cloud.ai.a2a.registry.AgentRegistry;
import com.alibaba.cloud.ai.a2a.registry.AgentRegistryService;
import io.a2a.spec.AgentCard;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * The AutoConfiguration for A2A server registry.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = { A2aServerAgentCardAutoConfiguration.class, A2aServerAutoConfiguration.class })
@ConditionalOnBean({ AgentCard.class })
public class A2aServerRegistryAutoConfiguration {

	@Bean
	@ConditionalOnBean(AgentRegistry.class)
	public AgentRegistryService agentRegistryService(AgentCard agentCard, AgentRegistry agentRegistry) {
		return new AgentRegistryService(agentRegistry, agentCard);
	}

}
