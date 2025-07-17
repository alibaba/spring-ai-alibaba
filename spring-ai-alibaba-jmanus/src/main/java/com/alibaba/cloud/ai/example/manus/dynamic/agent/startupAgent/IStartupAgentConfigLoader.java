package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import java.util.List;

/**
 * Interface for startup agent configuration loader
 */
public interface IStartupAgentConfigLoader {

	/**
	 * Clear cache
	 */
	void clearCache();

	/**
	 * Get cache size
	 */
	int getCacheSize();

	/**
	 * Load agent configuration by name
	 */
	StartupAgentConfigLoader.AgentConfig loadAgentConfig(String agentName);

	/**
	 * Scan available agents
	 */
	List<String> scanAvailableAgents();

}
