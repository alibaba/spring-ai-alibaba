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
package com.alibaba.cloud.ai.graph.agent.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when MCP (Model Context Protocol) tools are dynamically changed.
 * 
 * <p>This event is part of the dynamic tool discovery mechanism and enables real-time
 * updates to the tool registry when MCP server configurations change.
 * 
 * <h3>When Published</h3>
 * <ul>
 *   <li>When MCP server configuration in Nacos changes</li>
 *   <li>When tools are dynamically registered or unregistered</li>
 *   <li>When Nacos configuration listener detects content updates</li>
 * </ul>
 * 
 * <h3>Publisher</h3>
 * <ul>
 *   <li>{@link com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolCallback}</li>
 *   <li>Triggered by Nacos {@code AbstractListener} on configuration changes</li>
 * </ul>
 * 
 * <h3>Listeners</h3>
 * <ul>
 *   <li>{@link com.alibaba.cloud.ai.graph.agent.tool.CachingToolCallbackProvider} implementations</li>
 *   <li>Listeners invalidate tool caches to trigger reload of updated tools</li>
 * </ul>
 * 
 * <h3>ServerId Format</h3>
 * The {@code serverId} parameter follows the format: {@code "nacos-mcp:" + dataId + "@@" + group}
 * <p>Example: {@code "nacos-mcp:mcp-config@@DEFAULT_GROUP"}
 * <p>This identifier uniquely represents the Nacos configuration item that triggered the change.
 * 
 * @see com.alibaba.cloud.ai.graph.agent.tool.CachingToolCallbackProvider
 * @see com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolCallback
 */
public class McpToolsChangedEvent extends ApplicationEvent {

	private final String serverId;

	public McpToolsChangedEvent(Object source, String serverId) {
		super(source);
		this.serverId = serverId;
	}

	public String getServerId() {
		return serverId;
	}

}
