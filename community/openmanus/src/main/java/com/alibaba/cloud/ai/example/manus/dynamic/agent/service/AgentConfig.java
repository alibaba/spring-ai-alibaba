package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import java.util.List;

public class AgentConfig {

	private String id;

	private String name;

	private String description;

	private String systemPrompt;

	private String nextStepPrompt;

	private List<String> availableTools;

	private String className;

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public List<String> getAvailableTools() {
		return availableTools;
	}

	public void setAvailableTools(List<String> availableTools) {
		this.availableTools = availableTools;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
