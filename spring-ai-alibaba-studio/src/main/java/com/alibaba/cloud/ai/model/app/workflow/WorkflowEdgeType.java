package com.alibaba.cloud.ai.model.app.workflow;

public enum WorkflowEdgeType {

	DIRECT("DIRECT"),

	CONDITION("CONDITION")

	;

	private String value;

	private WorkflowEdgeType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
