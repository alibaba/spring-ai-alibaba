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

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.nacos.common.utils.CollectionUtils;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/**
 * Spring AI Alibaba Agent Card Wrapper for Nacos.
 *
 * @author xiweng.yy
 */
public class NacosAgentCardWrapper extends AgentCardWrapper {

	private final AtomicInteger pollingIndex;

	public NacosAgentCardWrapper(AgentCard agentCard) {
		super(agentCard);
		this.pollingIndex = new AtomicInteger(0);
		shuffleStartIndex();
	}

	private void shuffleStartIndex() {
		if (CollectionUtils.isNotEmpty(getAgentCard().supportedInterfaces())) {
			int shuffleIndex = ThreadLocalRandom.current().nextInt(getAgentCard().supportedInterfaces().size());
			pollingIndex.set(shuffleIndex);
		}
	}

	@Override
	public AgentEndpoint endpoint() {
		AgentCard agentCard = getAgentCard();
		AgentEndpoint preferred = preferredEndpoint(agentCard);
		if (CollectionUtils.isEmpty(agentCard.supportedInterfaces())) {
			return preferred;
		}
		List<AgentInterface> agentInterfaces = agentCard.supportedInterfaces()
			.stream()
			.filter(agentInterface -> agentInterface != null
					&& preferred.protocolBinding().equalsIgnoreCase(agentInterface.protocolBinding())
					&& Objects.equals(preferred.protocolVersion(), agentInterface.protocolVersion())
					&& Objects.equals(preferred.tenant(), agentInterface.tenant()))
			.toList();
		if (CollectionUtils.isEmpty(agentInterfaces)) {
			return preferred;
		}
		int index = agentInterfaces.size() == 1 ? 0
				: Math.floorMod(pollingIndex.getAndIncrement(), agentInterfaces.size());
		return new AgentEndpoint(agentInterfaces.get(index).url(), preferred.protocolBinding(),
				preferred.protocolVersion(), preferred.tenant());
	}

	@Override
	public void setAgentCard(AgentCard agentCard) {
		super.setAgentCard(agentCard);
		shuffleStartIndex();
	}
}
