/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
