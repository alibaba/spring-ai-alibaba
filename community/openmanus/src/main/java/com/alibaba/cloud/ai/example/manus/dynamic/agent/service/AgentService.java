package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;
import java.util.List;

public interface AgentService {

	List<AgentConfig> getAllAgents();

	AgentConfig getAgentById(String id);

	AgentConfig createAgent(AgentConfig agentConfig);

	AgentConfig updateAgent(AgentConfig agentConfig);

	void deleteAgent(String id);

	List<Tool> getAvailableTools();

}
