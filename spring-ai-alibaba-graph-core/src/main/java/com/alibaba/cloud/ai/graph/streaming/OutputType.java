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
package com.alibaba.cloud.ai.graph.streaming;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_HOOK_NAME_PREFIX;
import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_MODEL_NAME;
import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;

public enum OutputType {
	AGENT_MODEL_STREAMING,
	AGENT_MODEL_FINISHED,
	AGENT_TOOL_STREAMING,
	AGENT_TOOL_FINISHED,
	AGENT_HOOK_STREAMING,
	AGENT_HOOK_FINISHED,
	GRAPH_NODE_STREAMING,
	GRAPH_NODE_FINISHED;

	/**
	 * Converts to a specific OutputType instance based on streaming and nodeId parameters
	 *
	 * @param streaming whether it is streaming output
	 * @param nodeId    the node ID
	 * @return the corresponding OutputType instance
	 */
	public static OutputType from(boolean streaming, String nodeId) {
		if (nodeId.startsWith(AGENT_MODEL_NAME)) {
			return streaming ? AGENT_MODEL_STREAMING : AGENT_MODEL_FINISHED;
		} else if (nodeId.startsWith(AGENT_TOOL_NAME)) {
			return streaming ? AGENT_TOOL_STREAMING : AGENT_TOOL_FINISHED;
		} else if (nodeId.startsWith(AGENT_HOOK_NAME_PREFIX)) {
			return streaming ? AGENT_HOOK_STREAMING : AGENT_HOOK_FINISHED;
		} else {
			return streaming ? GRAPH_NODE_STREAMING : GRAPH_NODE_FINISHED;
		}
	}

}
