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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.nacos.common.utils.CollectionUtils;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;

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
		if (CollectionUtils.isNotEmpty(getAgentCard().additionalInterfaces())) {
			int shuffleIndex = ThreadLocalRandom.current().nextInt(getAgentCard().additionalInterfaces().size());
			pollingIndex.set(shuffleIndex);
		}
	}

	@Override
	public String url() {
		if (CollectionUtils.isEmpty(getAgentCard().additionalInterfaces())) {
			return super.url();
		}
		List<AgentInterface> agentInterfaces = getAgentCard().additionalInterfaces().stream().filter(agentInterface -> getAgentCard().preferredTransport().equals(agentInterface.transport())).toList();
		if (CollectionUtils.isEmpty(agentInterfaces)) {
			return super.url();
		}
		if (1 == agentInterfaces.size()) {
			return agentInterfaces.get(0).url();
		}
		int index = pollingIndex.incrementAndGet() % agentInterfaces.size();
		return agentInterfaces.get(index).url();
	}

	@Override
	public void setAgentCard(AgentCard agentCard) {
		super.setAgentCard(agentCard);
		shuffleStartIndex();
	}
}
