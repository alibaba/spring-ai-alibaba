package com.alibaba.cloud.ai.a2a.registry;

import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Agent registry service.
 *
 * @author xiweng.yy
 */
public class AgentRegistryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryService.class);

	private final AgentRegistry agentRegistry;

	private final AgentCard agentCard;

	public AgentRegistryService(AgentRegistry agentRegistry, AgentCard agentCard) {
		this.agentRegistry = agentRegistry;
		this.agentCard = agentCard;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void register() {
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

}
