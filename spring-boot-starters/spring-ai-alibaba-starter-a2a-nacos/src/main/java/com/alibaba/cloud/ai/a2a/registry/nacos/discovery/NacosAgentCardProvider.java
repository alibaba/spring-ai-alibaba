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

package com.alibaba.cloud.ai.a2a.registry.nacos.discovery;

import com.alibaba.cloud.ai.a2a.registry.nacos.utils.AgentCardConverterUtil;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;

import com.alibaba.nacos.api.ai.A2aService;
import com.alibaba.nacos.api.ai.listener.AbstractNacosAgentCardListener;
import com.alibaba.nacos.api.ai.listener.NacosAgentCardEvent;
import com.alibaba.nacos.api.ai.model.a2a.AgentCard;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.common.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of AgentCardProvider for getting agent card from nacos a2a
 * registry.
 *
 * @author xiweng.yy
 */
public class NacosAgentCardProvider implements AgentCardProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(NacosAgentCardProvider.class);

	private final A2aService a2aService;

	private AgentCardWrapper agentCard;

	public NacosAgentCardProvider(A2aService a2aService) {
		this.a2aService = a2aService;
	}

	@Override
	public AgentCardWrapper getAgentCard() {
		if (null == agentCard) {
			throw new IllegalStateException("Please use getAgentCard(agentName) first.");
		}
		return agentCard;
	}

	@Override
	public AgentCardWrapper getAgentCard(String agentName) {
		try {
			AgentCard nacosAgentCard = a2aService.getAgentCard(agentName);
			agentCard = new NacosAgentCardWrapper(AgentCardConverterUtil.convertToA2aAgentCard(nacosAgentCard));
			a2aService.subscribeAgentCard(agentName, new AbstractNacosAgentCardListener() {
				@Override
				public void onEvent(NacosAgentCardEvent event) {
					AgentCard newAgentCard = event.getAgentCard();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Received new Agent Card: {}", JacksonUtils.toJson(newAgentCard));
					}
					agentCard.setAgentCard(AgentCardConverterUtil.convertToA2aAgentCard(newAgentCard));
				}
			});
			return agentCard;
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
