package com.alibaba.cloud.ai.model.workflow;

public enum EdgeType {

	DIRECT("direct"),

	CONDITIONAL("conditional")

	;

	private String value;

	EdgeType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
