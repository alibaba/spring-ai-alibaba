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

package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.client.utils.NacosMcpClientUtils;
import com.alibaba.cloud.ai.mcp.nacos.registry.NacosMcpRegister;
import com.alibaba.cloud.ai.mcp.nacos.registry.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.mcp.nacos.service.model.NacosMcpServerEndpoint;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.ai.model.mcp.McpEndpointInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author yingzi
 * @date 2025/4/29:08:47
 */
@AutoConfiguration
@EnableConfigurationProperties({ NacosMcpSseClientProperties.class, NacosMcpProperties.class,
		NacosMcpRegistryProperties.class })
public class NacosMcpSseClientAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(NacosMcpSseClientAutoConfiguration.class);

	public NacosMcpSseClientAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(NacosMcpOperationService.class)
	public NacosMcpOperationService nacosMcpOperationService(NacosMcpProperties nacosMcpProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		try {
			return new NacosMcpOperationService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean(name = "server2NamedTransport")
	public Map<String, List<NamedClientMcpTransport>> server2NamedTransport(
			NacosMcpSseClientProperties nacosMcpSseClientProperties, NacosMcpOperationService nacosMcpOperationService,
			ObjectProvider<WebClient.Builder> webClientBuilderProvider,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = new HashMap<>();
		WebClient.Builder webClientBuilderTemplate = (WebClient.Builder) webClientBuilderProvider
			.getIfAvailable(WebClient::builder);
		ObjectMapper objectMapper = (ObjectMapper) objectMapperProvider.getIfAvailable(ObjectMapper::new);

		Map<String, String> connections = nacosMcpSseClientProperties.getConnections();
		connections.forEach((serverKey, serverName) -> {
			try {
				NacosMcpServerEndpoint serverEndpoint = nacosMcpOperationService.getServerEndpoint(serverName);
				if (serverEndpoint == null) {
					throw new NacosException(NacosException.NOT_FOUND,
							"can not find mcp server from nacos: " + serverName);
				}
				if (!StringUtils.equals(serverEndpoint.getProtocol(), AiConstants.Mcp.MCP_PROTOCOL_SSE)) {
					throw new Exception("mcp server protocol must be sse");
				}
				List<NamedClientMcpTransport> namedTransports = new ArrayList<>();
				for (McpEndpointInfo endpointInfo : serverEndpoint.getMcpEndpointInfoList()) {
					String url = "http://" + endpointInfo.getAddress() + ":" + endpointInfo.getPort();
					WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(url);
					WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper,
							serverEndpoint.getExportPath());
					namedTransports.add(new NamedClientMcpTransport(serverName + "-"
							+ NacosMcpClientUtils.getMcpEndpointInfoId(endpointInfo, serverEndpoint.getExportPath()),
							transport));
				}
				server2NamedTransport.put(serverName, namedTransports);
			}
			catch (Exception e) {
				logger.error("get mcp server from nacos: {} error", serverName, e);
			}
		});
		return server2NamedTransport;
	}

}
