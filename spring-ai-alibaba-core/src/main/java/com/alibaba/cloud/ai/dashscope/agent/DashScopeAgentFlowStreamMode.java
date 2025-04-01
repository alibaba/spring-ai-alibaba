package com.alibaba.cloud.ai.dashscope.agent;

public enum DashScopeAgentFlowStreamMode {

	/**
	 * The streaming results from all nodes will be output in the thoughts field.
	 */
	FULL_THOUGHTS("full_thoughts"),

	/**
	 * Use the same output pattern as the agent application.
	 */
	AGENT_FORMAT("agent_format");

	private final String value;

	DashScopeAgentFlowStreamMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
