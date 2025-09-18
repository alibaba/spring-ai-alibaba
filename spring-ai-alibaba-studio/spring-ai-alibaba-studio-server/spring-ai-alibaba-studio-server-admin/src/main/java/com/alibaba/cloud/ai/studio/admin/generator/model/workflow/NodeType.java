/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.studio.admin.generator.model.workflow;

import java.util.Arrays;
import java.util.Optional;

// TODO: 将枚举类的DSL Value字段改为Function<DSLDialectType, String>
public enum NodeType {

	START("start", "start", "Start"),

	END("end", "end", "End"),

	ANSWER("answer", "answer", "UNSUPPORTED"),

	MIDDLE_OUTPUT("middle-output", "UNSUPPORTED", "Output"),

	AGENT("agent", "agent", "UNSUPPORTED"),

	LLM("llm", "llm", "LLM"),

	CODE("code", "code", "Script"),

	RETRIEVER("retriever", "knowledge-retrieval", "Retrieval"),

	AGGREGATOR("aggregator", "variable-aggregator", "UNSUPPORTED"),

	HUMAN("human", "UNSUPPORTED", "UNSUPPORTED"),

	BRANCH("branch", "if-else", "Judge"),

	DOC_EXTRACTOR("document-extractor", "document-extractor", "UNSUPPORTED"),

	QUESTION_CLASSIFIER("question-classifier", "question-classifier", "Classifier"),

	HTTP("http", "http-request", "API"),

	LIST_OPERATOR("list-operator", "list-operator", "UNSUPPORTED"),

	PARAMETER_PARSING("parameter-parsing", "parameter-extractor", "ParameterExtractor"),

	TOOL("tool", "tool", "UNSUPPORTED"),

	// Dify的MCP使用ToolNode定义
	MCP("mcp", "UNSUPPORTED", "MCP"),

	TEMPLATE_TRANSFORM("template-transform", "template-transform", "UNSUPPORTED"),

	ITERATION("iteration", "iteration", "Parallel"),

	EMPTY("empty", "UNSUPPORTED", "UNSUPPORTED"),

	ITERATION_START("iteration-start", "iteration-start", "ParallelStart"),

	ITERATION_END("iteration-end", "iteration-end", "ParallelEnd"),

	ASSIGNER("assigner", "assigner", "VariableAssign");

	private final String value;

	private final String difyValue;

	private final String studioValue;

	NodeType(String value, String difyValue, String studioValue) {
		this.value = value;
		this.difyValue = difyValue;
		this.studioValue = studioValue;
	}

	public String value() {
		return this.value;
	}

	public String difyValue() {
		return this.difyValue;
	}

	public String studioValue() {
		return this.studioValue;
	}

	public static boolean isEmpty(NodeType nodeType) {
		return NodeType.EMPTY.equals(nodeType) || NodeType.ITERATION_START.equals(nodeType)
				|| NodeType.ITERATION_END.equals(nodeType);
	}

	public static Optional<NodeType> fromValue(String value) {
		return Arrays.stream(NodeType.values()).filter(nodeType -> nodeType.value.equals(value)).findFirst();
	}

	public static Optional<NodeType> fromDifyValue(String difyValue) {
		return Arrays.stream(NodeType.values()).filter(nodeType -> nodeType.difyValue.equals(difyValue)).findFirst();
	}

	public static Optional<NodeType> fromStudioValue(String studioValue) {
		return Arrays.stream(NodeType.values())
			.filter(nodeType -> nodeType.studioValue.equals(studioValue))
			.findFirst();
	}

}
