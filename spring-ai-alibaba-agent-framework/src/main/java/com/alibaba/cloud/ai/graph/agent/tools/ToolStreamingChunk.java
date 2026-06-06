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
package com.alibaba.cloud.ai.graph.agent.tools;

import java.util.Map;

/**
 * Runtime-only tool streaming chunk emitted during tool execution.
 *
 * @param toolCallId tool call id from the assistant message
 * @param toolName tool name
 * @param content chunk text content
 * @param metadata optional chunk metadata
 * @author Zhengcy05
 * @since 1.0.0
 */
public record ToolStreamingChunk(String toolCallId, String toolName, String content, Map<String, Object> metadata) {

	public ToolStreamingChunk {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
