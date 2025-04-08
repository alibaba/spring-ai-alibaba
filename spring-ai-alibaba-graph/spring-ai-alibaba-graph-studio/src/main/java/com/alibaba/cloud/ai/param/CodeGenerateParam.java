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
package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * code generate param TODO complement
 */
public class CodeGenerateParam {

	@Schema(description = "node type", example = "CODE")
	private String nodeType;

	@Schema(description = "node data")
	private Map<String, Object> nodeData;

	public String getNodeType() {
		return nodeType;
	}

	public CodeGenerateParam setNodeType(String nodeType) {
		this.nodeType = nodeType;
		return this;
	}

	public Map<String, Object> getNodeData() {
		return nodeData;
	}

	public CodeGenerateParam setNodeData(Map<String, Object> nodeData) {
		this.nodeData = nodeData;
		return this;
	}

}
