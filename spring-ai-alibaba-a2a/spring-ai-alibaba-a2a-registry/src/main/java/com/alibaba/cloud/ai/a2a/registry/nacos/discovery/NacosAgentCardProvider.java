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

package com.alibaba.cloud.ai.a2a.registry.nacos.discovery;

import com.alibaba.cloud.ai.a2a.registry.nacos.utils.AgentCardConverterUtil;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import io.a2a.spec.AgentCard;

/**
 * Implementation of {@link AgentCardProvider} for getting agent card from nacos a2a
 * registry.
 *
 * @author xiweng.yy
 */
public class NacosAgentCardProvider implements AgentCardProvider {

	private final A2aService a2aService;

	private com.alibaba.nacos.api.ai.model.a2a.AgentCard nacosAgentCard;

	public NacosAgentCardProvider(A2aService a2aService) {
		this.a2aService = a2aService;
	}

	@Override
	public AgentCard getAgentCard() {
		if (null == nacosAgentCard) {
			throw new IllegalStateException("Please use getAgentCard(agentName) first");
		}
		return AgentCardConverterUtil.convertToA2aAgentCard(nacosAgentCard);
	}

	@Override
	public AgentCard getAgentCard(String agentName) {
		try {
			nacosAgentCard = a2aService.getAgentCard(agentName);
			return AgentCardConverterUtil.convertToA2aAgentCard(nacosAgentCard);
		}
		catch (NacosException e) {
			throw new NacosRuntimeException(e.getErrCode(), e.getErrMsg());
		}
	}

	@Override
	public boolean supportGetAgentCardByName() {
		return true;
	}

}
