/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.subagent;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

/**
 * Holds default tools and task tools for the subagent example. Wraps
 * {@code List<ToolCallback>} so they are not registered as separate beans and
 * do not conflict with Spring AI's {@code ToolCallingAutoConfiguration}.
 */
public record SubagentTools(
		List<ToolCallback> defaultTools,
		List<ToolCallback> taskTools,
		ReactAgent dependencyAnalyzerAgent) {
}
