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

package com.alibaba.cloud.ai.agent.nacos;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolCallback;
import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolsInitializer;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.nacos.api.ai.model.mcp.McpServerDetailInfo;
import com.alibaba.nacos.api.ai.model.mcp.McpServerRemoteServiceConfig;
import com.alibaba.nacos.api.ai.model.mcp.McpTool;
import com.alibaba.nacos.api.ai.model.mcp.McpToolMeta;
import com.alibaba.nacos.api.ai.model.mcp.McpToolSpecification;
import com.alibaba.nacos.api.ai.model.mcp.registry.ServerVersionDetail;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NacosMcpGatewayTimeoutTest {

	@Test
	void shouldUseThirtySecondGatewayToolTimeoutByDefault() throws Exception {
		NacosOptions options = new NacosOptions(properties());

		assertThat(options.getMcpGatewayToolTimeout()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void shouldParseGatewayToolTimeoutFromProperties() throws Exception {
		Properties properties = properties();
		properties.setProperty("mcpGatewayToolTimeout", "75s");
		NacosOptions options = new NacosOptions(properties);

		assertThat(options.getMcpGatewayToolTimeout()).isEqualTo(Duration.ofSeconds(75));
	}

	@Test
	void shouldParseKebabCaseGatewayToolTimeoutFromProperties() throws Exception {
		Properties properties = properties();
		properties.setProperty("mcp-gateway-tool-timeout", "90s");
		NacosOptions options = new NacosOptions(properties);

		assertThat(options.getMcpGatewayToolTimeout()).isEqualTo(Duration.ofSeconds(90));
	}

	@Test
	void shouldPassConfiguredTimeoutToGatewayToolCallbacks() throws Exception {
		NacosMcpOperationService operationService = mock(NacosMcpOperationService.class);
		McpServerDetailInfo detailInfo = mcpServerDetailInfo();
		when(operationService.getServerDetail("order-service")).thenReturn(detailInfo);

		McpServersVO.McpServerVO serverVO = new McpServersVO.McpServerVO();
		serverVO.setMcpServerName("order-service");

		NacosMcpGatewayToolsInitializer initializer = new NacosMcpGatewayToolsInitializer(operationService,
				List.of(serverVO), Duration.ofSeconds(90));

		List<ToolCallback> callbacks = initializer.initializeTools();

		assertThat(callbacks).hasSize(1);
		assertThat(callbacks.get(0)).isInstanceOfSatisfying(NacosMcpGatewayToolCallback.class,
				callback -> assertThat(callback.getRequestTimeout()).isEqualTo(Duration.ofSeconds(90)));
	}

	private static Properties properties() {
		Properties properties = new Properties();
		properties.setProperty("serverAddr", "127.0.0.1:8848");
		properties.setProperty("agentName", "test-agent");
		return properties;
	}

	private static McpServerDetailInfo mcpServerDetailInfo() {
		McpTool tool = new McpTool();
		tool.setName("query");
		tool.setDescription("Query orders");
		tool.setInputSchema(Map.of("type", "object", "properties", Map.of()));

		McpToolMeta toolMeta = new McpToolMeta();
		toolMeta.setEnabled(true);

		McpToolSpecification toolSpecification = new McpToolSpecification();
		toolSpecification.setTools(List.of(tool));
		toolSpecification.setToolsMeta(java.util.Map.of("query", toolMeta));

		McpServerDetailInfo detailInfo = new McpServerDetailInfo();
		detailInfo.setName("order-service");
		detailInfo.setProtocol("mcp-sse");
		detailInfo.setRemoteServerConfig(new McpServerRemoteServiceConfig());
		detailInfo.setToolSpec(toolSpecification);
		ServerVersionDetail versionDetail = mock(ServerVersionDetail.class);
		when(versionDetail.getVersion()).thenReturn("1.0.0");
		detailInfo.setVersionDetail(versionDetail);
		return detailInfo;
	}

}
