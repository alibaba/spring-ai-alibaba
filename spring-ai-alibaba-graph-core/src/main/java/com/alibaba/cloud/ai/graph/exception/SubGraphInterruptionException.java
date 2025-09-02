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
package com.alibaba.cloud.ai.graph.exception;

import java.util.Map;
import java.util.Optional;
import static java.lang.String.format;

public class SubGraphInterruptionException extends GraphRunnerException {

	final String parentNodeId;

	final String nodeId;

	final Map<String, Object> state;

	public SubGraphInterruptionException(String parentNodeId, String nodeId, Map<String, Object> state) {
		super(format("interruption in subgraph: %s on node: %s", parentNodeId, nodeId));
		this.parentNodeId = parentNodeId;
		this.nodeId = nodeId;
		this.state = state;
	}

	public String parentNodeId() {
		return parentNodeId;
	}

	public String nodeId() {
		return nodeId;
	}

	public Map<String, Object> state() {
		return state;
	}

	public static Optional<SubGraphInterruptionException> from(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SubGraphInterruptionException ex) {
				return Optional.of(ex);
			}
			current = current.getCause();
		}
		return Optional.empty();
	}

}
