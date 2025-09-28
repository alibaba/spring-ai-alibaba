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

package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.container.McpClientContainer;
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import com.alibaba.cloud.ai.service.McpInspectorService;
import com.alibaba.cloud.ai.strategy.transport.impl.AbstractTransport;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Service;

@Service
public class McpInspectorServiceImpl implements McpInspectorService {

	private final McpClientContainer mcpClientContainer;

	public McpInspectorServiceImpl(McpClientContainer mcpClientContainer) {
		this.mcpClientContainer = mcpClientContainer;
	}

	// 拿到对应的信息
	@Override
	public R<String> init(McpConnectRequest request) {
		McpTransportType transportType = request.getTransportType();
		AbstractTransport abstractTransport = AbstractTransport.transportTypeMap.get(transportType);
		if (abstractTransport == null) {
			throw new RuntimeException("Unsupported transport type: " + transportType);
		}
		String clientName = abstractTransport.getClientName();
		McpSyncClient mcpClient = abstractTransport.connect(request);
		mcpClientContainer.add(clientName, mcpClient);
		return R.success(clientName);
	}

	@Override
	public R<McpSchema.ListToolsResult> listTools(String clientName) {
		McpSyncClient mcpSyncClient = mcpClientContainer.get(clientName);
		return R.success(mcpSyncClient.listTools());

	}

}
