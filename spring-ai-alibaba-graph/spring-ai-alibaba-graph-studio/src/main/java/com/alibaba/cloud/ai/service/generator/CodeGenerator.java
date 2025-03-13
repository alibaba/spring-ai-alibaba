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

package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.workflow.NodeType;

import java.util.Map;

/**
 * CodeGenerator abstracts the code generation of a specific node
 */
public interface CodeGenerator {

	/**
	 * whether the node type is supported
	 * @param nodeType {@link NodeType}
	 * @return true if supported
	 */
	Boolean supportNodeType(String nodeType);

	/**
	 * generate code
	 * @param nodeData node properties
	 * @return code string
	 */
	String generateCode(Map<String, Object> nodeData);

}
