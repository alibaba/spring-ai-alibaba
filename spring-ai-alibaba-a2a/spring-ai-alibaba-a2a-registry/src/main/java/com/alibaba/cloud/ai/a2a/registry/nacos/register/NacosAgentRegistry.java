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

package com.alibaba.cloud.ai.a2a.registry.nacos.register;

import com.alibaba.cloud.ai.a2a.registry.AgentRegistry;
import com.alibaba.cloud.ai.a2a.registry.nacos.properties.NacosA2aProperties;
import com.alibaba.cloud.ai.a2a.registry.nacos.service.NacosA2aOperationService;
import io.a2a.spec.AgentCard;

/**
 * The Agent registry for Nacos.
 *
 * @author xiweng.yy
 */
public class NacosAgentRegistry implements AgentRegistry {

	private final NacosA2aOperationService a2aOperationService;

	private final NacosA2aProperties nacosA2aProperties;

	public NacosAgentRegistry(NacosA2aOperationService a2aOperationService, NacosA2aProperties nacosA2aProperties) {
		this.a2aOperationService = a2aOperationService;
		this.nacosA2aProperties = nacosA2aProperties;
	}

	@Override
	public String registryName() {
		return String.format("Nacos[%s]", nacosA2aProperties.getServerAddr());
	}

	@Override
	public void register(AgentCard agentCard) {
		a2aOperationService.registerAgent(agentCard);
	}

}
