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

package com.alibaba.cloud.ai.strategy.transport.impl;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.container.McpClientContainer;
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import com.alibaba.cloud.ai.domain.impl.SSEParams;
import com.alibaba.cloud.ai.strategy.McpParameterFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SSETransportProcesser extends AbstractTransport {

	private final Logger log = LoggerFactory.getLogger(SSETransportProcesser.class);

	@Autowired
	private McpClientContainer mcpClientContainer;

	@Autowired
	private McpParameterFactory parameterFactory;

	@Override
	protected McpTransportType getTransportType() {
		return McpTransportType.SSE;
	}

	@Override
	public McpSyncClient connect(McpConnectRequest mcpConnectRequest) {
		// 去连接对应的mcpServer
		SSEParams sseParams = parameterFactory.parse(mcpConnectRequest.getTransportType(),
				mcpConnectRequest.getParams());
		HttpClientSseClientTransport sseTransport = HttpClientSseClientTransport.builder(sseParams.getBaseUri())
			.sseEndpoint(sseParams.getSseEndpoint())
			.build();
		McpSyncClient mcpSyncClient = McpClient.sync(sseTransport).build();
		mcpClientContainer.add(getClientName(), mcpSyncClient);
		if (!mcpSyncClient.isInitialized()) {
			mcpSyncClient.initialize();
		}
		return mcpSyncClient;
	}

	@Override
	public String getClientName() {
		return "SSE_" + counter.getAndIncrement();
	}

}
