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

package com.alibaba.cloud.ai.mcp.client.component;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * @author yingzi
 * @since 2025/7/14
 */

public class McpSyncClientWrapper {

	private final McpSyncClient client;

	private final List<ToolCallback> toolCallbacks;

	public McpSyncClientWrapper(McpSyncClient client, List<ToolCallback> toolCallbacks) {
		this.client = client;
		this.toolCallbacks = toolCallbacks;
	}

	public McpSyncClient getClient() {
		return client;
	}

	public List<ToolCallback> getToolCallbacks() {
		return toolCallbacks;
	}

}
