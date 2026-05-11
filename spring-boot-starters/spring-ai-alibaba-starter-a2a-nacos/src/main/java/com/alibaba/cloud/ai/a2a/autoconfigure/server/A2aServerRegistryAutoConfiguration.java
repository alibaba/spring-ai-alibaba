/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.autoconfigure.server;

import com.alibaba.cloud.ai.a2a.core.registry.AgentRegistry;
import com.alibaba.cloud.ai.a2a.core.registry.AgentRegistryService;
import com.alibaba.cloud.ai.a2a.core.route.MultiAgentRequestRouter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.a2a.spec.AgentCard;

/**
 * The AutoConfiguration for A2A server registry in single-agent mode.
 * <p>
 * This configuration is skipped when multi-agent mode is active
 * (when {@link MultiAgentRequestRouter} bean exists). In multi-agent mode,
 * the registry service is created by {@link A2aServerMultiAgentAutoConfiguration}.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = { A2aServerAgentCardAutoConfiguration.class, A2aServerAutoConfiguration.class,
		A2aServerMultiAgentAutoConfiguration.class })
@ConditionalOnBean({ AgentCard.class })
@ConditionalOnMissingBean(MultiAgentRequestRouter.class)
public class A2aServerRegistryAutoConfiguration {

	@Bean
	@ConditionalOnBean(AgentRegistry.class)
	@ConditionalOnMissingBean(AgentRegistryService.class)
	public AgentRegistryService agentRegistryService(AgentCard agentCard, AgentRegistry agentRegistry) {
		return new AgentRegistryService(agentRegistry, agentCard);
	}

}
