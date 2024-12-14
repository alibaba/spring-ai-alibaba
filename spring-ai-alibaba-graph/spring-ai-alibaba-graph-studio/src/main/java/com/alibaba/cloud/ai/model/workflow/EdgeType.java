package com.alibaba.cloud.ai.model.workflow;

public enum EdgeType {

	DIRECT("DIRECT"),

	CONDITIONAL("CONDITIONAL")

	;

	private String value;

	private EdgeType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
