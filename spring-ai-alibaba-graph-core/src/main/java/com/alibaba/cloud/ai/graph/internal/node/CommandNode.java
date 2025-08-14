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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandNode extends Node {

	private final Map<String, String> mappings;

	private final AsyncCommandAction action;

	public CommandNode(String id, AsyncCommandAction action, Map<String, String> mappings) {
		super(id, (config) -> new AsyncCommandNodeActionWithConfig(action, mappings));
		this.mappings = mappings;
		this.action = action;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public AsyncCommandAction getAction() {
		return action;
	}

	public record AsyncCommandNodeActionWithConfig(AsyncCommandAction action,
			Map<String, String> mappings) implements AsyncNodeActionWithConfig {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of("command", action, "mappings", mappings));
		}
	}

}
