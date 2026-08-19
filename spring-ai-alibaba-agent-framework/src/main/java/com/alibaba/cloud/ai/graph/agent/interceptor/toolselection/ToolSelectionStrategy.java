/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolselection;

import java.util.List;

/**
 * Strategy for selecting relevant tools from lightweight tool metadata.
 * Implementations may use an LLM, lexical search, a vector store, or business rules.
 * Implementations should be thread-safe because an interceptor can serve concurrent
 * agent requests.
 */
@FunctionalInterface
public interface ToolSelectionStrategy {

	/**
	 * Select tool names in relevance order.
	 * @param request selection request containing the query and lightweight metadata
	 * @return selected tool names; unknown names are ignored by the interceptor
	 * @throws Exception when selection fails
	 */
	List<String> select(ToolSelectionRequest request) throws Exception;
}
