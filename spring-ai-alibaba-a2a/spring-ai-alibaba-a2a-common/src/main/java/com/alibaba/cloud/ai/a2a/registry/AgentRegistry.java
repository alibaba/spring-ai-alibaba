package com.alibaba.cloud.ai.a2a.registry;

import io.a2a.spec.AgentCard;

/**
 * @author xiweng.yy
 */
public interface AgentRegistry {

	String registryName();

	void register(AgentCard agentCard);

}
