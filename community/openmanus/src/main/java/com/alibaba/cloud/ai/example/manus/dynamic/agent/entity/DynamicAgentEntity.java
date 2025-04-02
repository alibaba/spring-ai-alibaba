package com.alibaba.cloud.ai.example.manus.dynamic.agent.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "dynamic_agents")
public class DynamicAgentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String agentName;

	@Column(nullable = false, length = 1000)
	private String agentDescription;

	@Column(nullable = false, length = 4000)
	private String systemPrompt;

	@Column(nullable = false, length = 4000)
	private String nextStepPrompt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "dynamic_agent_tools", joinColumns = @JoinColumn(name = "agent_id"))
	@Column(name = "tool_key")
	private List<String> availableToolKeys;

	@Column(nullable = false)
	private String className;

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentDescription() {
		return agentDescription;
	}

	public void setAgentDescription(String agentDescription) {
		this.agentDescription = agentDescription;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public String getNextStepPrompt() {
		return nextStepPrompt;
	}

	public void setNextStepPrompt(String nextStepPrompt) {
		this.nextStepPrompt = nextStepPrompt;
	}

	public List<String> getAvailableToolKeys() {
		return availableToolKeys;
	}

	public void setAvailableToolKeys(List<String> availableToolKeys) {
		this.availableToolKeys = availableToolKeys;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
