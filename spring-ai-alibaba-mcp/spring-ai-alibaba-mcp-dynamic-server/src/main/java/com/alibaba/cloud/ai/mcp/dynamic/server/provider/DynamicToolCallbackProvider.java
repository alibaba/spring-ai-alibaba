package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.Assert;

import java.util.List;

public class DynamicToolCallbackProvider implements ToolCallbackProvider {

	private final ToolCallback[] toolCallbacks;

	public DynamicToolCallbackProvider(ToolCallback[] toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public ToolCallback[] getToolCallbacks() {
		return this.toolCallbacks;
	}

	public static DynamicToolCallbackProvider.Builder builder() {
		return new DynamicToolCallbackProvider.Builder();
	}

	public static class Builder {

		private ToolCallback[] toolCallbacks;

		private Builder() {
		}

		public Builder toolCallbacks(ToolCallback[] toolCallbacks) {
			this.toolCallbacks = toolCallbacks;
			return this;
		}

		public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
			this.toolCallbacks = toolCallbacks.toArray(new ToolCallback[0]);
			return this;
		}

		public DynamicToolCallbackProvider build() {
			return new DynamicToolCallbackProvider(this.toolCallbacks);
		}

	}

}
