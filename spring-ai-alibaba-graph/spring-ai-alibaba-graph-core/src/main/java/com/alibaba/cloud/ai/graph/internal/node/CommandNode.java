package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandNode extends Node {

	private final Map<String, String> mappings;

	public CommandNode(String id, AsyncCommandAction action, Map<String, String> mappings) {
		super(id, (config) -> new AsyncCommandNodeActionWithConfig(action, mappings));
		this.mappings = mappings;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public record AsyncCommandNodeActionWithConfig(AsyncCommandAction action,
			Map<String, String> mappings) implements AsyncNodeActionWithConfig {

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			return CompletableFuture
				.completedFuture(Map.of("command", action.apply(state, config).join(), "mappings", mappings));
		}
	}

}
