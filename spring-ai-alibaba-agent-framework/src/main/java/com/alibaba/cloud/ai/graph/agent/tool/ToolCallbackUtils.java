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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for ToolCallback collections.
 */
public final class ToolCallbackUtils {

	private ToolCallbackUtils() {
	}

	@SafeVarargs
	public static List<ToolCallback> deduplicateByName(List<ToolCallback>... toolGroups) {
		Map<String, ToolCallback> deduplicated = new LinkedHashMap<>();
		if (toolGroups == null) {
			return List.of();
		}
		for (List<ToolCallback> toolGroup : toolGroups) {
			if (toolGroup == null || toolGroup.isEmpty()) {
				continue;
			}
			for (ToolCallback toolCallback : toolGroup) {
				if (toolCallback == null || toolCallback.getToolDefinition() == null
						|| toolCallback.getToolDefinition().name() == null) {
					continue;
				}
				deduplicated.putIfAbsent(toolCallback.getToolDefinition().name(), toolCallback);
			}
		}
		return new ArrayList<>(deduplicated.values());
	}

}
