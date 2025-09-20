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
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import com.alibaba.cloud.ai.domain.impl.StdioParams;
import com.alibaba.cloud.ai.strategy.McpParameterFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StdioTransportProcesser extends AbstractTransport {

	private final Logger log = LoggerFactory.getLogger(StdioTransportProcesser.class);

	@Autowired
	private McpParameterFactory parameterFactory;

	@Override
	public McpSyncClient connect(McpConnectRequest mcpConnectRequest) {
		StdioParams stdioParams = parameterFactory.parse(McpTransportType.STDIO, mcpConnectRequest.getParams());
		ServerParameters serverParameters = ServerParameters.builder(stdioParams.getCommand())
			.args(stdioParams.getArgs())
			.env(stdioParams.getEnv())
			.build();
		// 去连接对应的mcpServer
		log.info("Connect server parameters: " + serverParameters);
		McpSyncClient mcpStdioClient = McpClient.sync(new StdioClientTransport(serverParameters)).build();
		log.info("Connect mcp stdio client: " + mcpStdioClient);
		if (!mcpStdioClient.isInitialized()) {
			mcpStdioClient.initialize();
		}
		return mcpStdioClient;
	}

	@Override
	protected McpTransportType getTransportType() {
		return McpTransportType.STDIO;
	}

	@Override
	public String getClientName() {
		return "STDIO_" + counter.getAndIncrement();
	}

}
