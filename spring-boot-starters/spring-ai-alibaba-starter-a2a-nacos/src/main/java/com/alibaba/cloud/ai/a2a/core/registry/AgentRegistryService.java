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

package com.alibaba.cloud.ai.a2a.core.registry;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent registry service.
 * <p>
 * Supports registering both single agent and multiple agents to the registry.
 *
 * @author xiweng.yy
 */
public class AgentRegistryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryService.class);

	private final AgentRegistry agentRegistry;

	private final List<AgentCard> agentCards;

	/**
	 * Constructor for single agent mode.
	 * @param agentRegistry the agent registry
	 * @param agentCard the single agent card
	 */
	public AgentRegistryService(AgentRegistry agentRegistry, AgentCard agentCard) {
		this.agentRegistry = agentRegistry;
		this.agentCards = Collections.singletonList(agentCard);
	}

	/**
	 * Constructor for multi-agent mode.
	 * @param agentRegistry the agent registry
	 * @param agentCards the list of agent cards
	 */
	public AgentRegistryService(AgentRegistry agentRegistry, List<AgentCard> agentCards) {
		this.agentRegistry = agentRegistry;
		this.agentCards = agentCards;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void register() {
		for (AgentCard agentCard : agentCards) {
			registerSingleAgent(agentCard);
		}
	}

	private void registerSingleAgent(AgentCard agentCard) {
		LOGGER.info("Auto register agent {} into Registry {}.", agentCard.name(), agentRegistry.registryName());
		try {
			agentRegistry.register(agentCard);
			LOGGER.info("Auto register agent {} into Registry {} successfully.", agentCard.name(),
					agentRegistry.registryName());
		}
		catch (Exception e) {
			LOGGER.error("Auto register agent {} into Registry {} failed.", agentCard.name(),
					agentRegistry.registryName(), e);
		}
	}

	/**
	 * Get the list of registered agent cards.
	 * @return an unmodifiable list of agent cards
	 */
	public List<AgentCard> getAgentCards() {
		return Collections.unmodifiableList(agentCards);
	}

}
