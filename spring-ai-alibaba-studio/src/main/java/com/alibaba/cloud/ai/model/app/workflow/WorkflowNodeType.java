package com.alibaba.cloud.ai.model.app.workflow;

import java.util.HashMap;
import java.util.Map;

public enum WorkflowNodeType {

	START("START", "start"),

	END("END", "end"),

	LLM("LLM", "llm"),

	CODE("CODE", "code"),

	RETRIEVER("RETRIEVER", "retriever"),

	HUMAN("HUMAN", "unsupported"),;

	private String value;

	private String difyValue;

	WorkflowNodeType(String value, String difyValue) {
		this.value = value;
		this.difyValue = difyValue;
	}

	public String value() {
		return this.value;
	}

	public String difyValue() {
		return this.difyValue;
	}

	public static WorkflowNodeType difyValueOf(String difyValue) {
		for (WorkflowNodeType nodeType : WorkflowNodeType.values()) {
			if (nodeType.difyValue.equals(difyValue)) {
				return nodeType;
			}
		}
		return null;
	}

}
