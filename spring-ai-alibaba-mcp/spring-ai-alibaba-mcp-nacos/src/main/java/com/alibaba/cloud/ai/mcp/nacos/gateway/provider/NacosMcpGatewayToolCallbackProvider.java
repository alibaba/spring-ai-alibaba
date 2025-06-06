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

package com.alibaba.cloud.ai.mcp.nacos.gateway.provider;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.Assert;

import java.util.List;

public class NacosMcpGatewayToolCallbackProvider implements ToolCallbackProvider {

	private final ToolCallback[] toolCallbacks;

	public NacosMcpGatewayToolCallbackProvider(ToolCallback[] toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public ToolCallback[] getToolCallbacks() {
		return this.toolCallbacks;
	}

	public static NacosMcpGatewayToolCallbackProvider.Builder builder() {
		return new NacosMcpGatewayToolCallbackProvider.Builder();
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

		public NacosMcpGatewayToolCallbackProvider build() {
			return new NacosMcpGatewayToolCallbackProvider(this.toolCallbacks);
		}

	}

}
