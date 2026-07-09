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

package com.alibaba.cloud.ai.agent.nacos.tools;

import java.time.Duration;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import io.modelcontextprotocol.client.McpClient;
import org.junit.jupiter.api.Test;

import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NacosMcpGatewayToolCallbackTest {

	@Test
	void shouldApplyRequestAndInitializationTimeoutsWhenConfigured() {
		NacosMcpGatewayToolCallback callback = new NacosMcpGatewayToolCallback(toolDefinition(),
				mock(NacosMcpOperationService.class), new McpServersVO.McpServerVO(), Duration.ofSeconds(90));
		McpClient.SyncSpec clientSpec = mock(McpClient.SyncSpec.class, RETURNS_SELF);

		callback.configureTimeouts(clientSpec);

		verify(clientSpec).requestTimeout(Duration.ofSeconds(90));
		verify(clientSpec).initializationTimeout(Duration.ofSeconds(90));
	}

	@Test
	void shouldNotOverrideSdkTimeoutsWhenUnset() {
		NacosMcpGatewayToolCallback callback = new NacosMcpGatewayToolCallback(toolDefinition(),
				mock(NacosMcpOperationService.class), new McpServersVO.McpServerVO(), null);
		McpClient.SyncSpec clientSpec = mock(McpClient.SyncSpec.class, RETURNS_SELF);

		callback.configureTimeouts(clientSpec);

		verifyNoInteractions(clientSpec);
	}

	private static NacosMcpGatewayToolDefinition toolDefinition() {
		return NacosMcpGatewayToolDefinition.builder()
				.name("order-service_tools_query")
				.description("Query orders")
				.inputSchema(Map.of("type", "object", "properties", Map.of()))
				.build();
	}

}
