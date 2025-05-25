package com.alibaba.cloud.ai.model.workflow;

import java.util.Arrays;
import java.util.Optional;

public enum NodeType {

	START("start", "start"),

	END("end", "end"),

	ANSWER("answer", "answer"),

	LLM("llm", "llm"),

	CODE("code", "code"),

	RETRIEVER("retriever", "knowledge-retrieval"),

	AGGREGATOR("aggregator", "variable-aggregator"),

	HUMAN("human", "unsupported"),

	BRANCH("branch", "if-else");

	private String value;

	private String difyValue;

	NodeType(String value, String difyValue) {
		this.value = value;
		this.difyValue = difyValue;
	}

	public String value() {
		return this.value;
	}

	public String difyValue() {
		return this.difyValue;
	}

	public static Optional<NodeType> fromValue(String value) {
		return Arrays.stream(NodeType.values()).filter(nodeType -> nodeType.value.equals(value)).findFirst();
	}

	public static Optional<NodeType> fromDifyValue(String difyValue) {
		return Arrays.stream(NodeType.values()).filter(nodeType -> nodeType.difyValue.equals(difyValue)).findFirst();
	}

}
