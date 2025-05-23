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

import com.alibaba.cloud.ai.mcp.nacos2.registry.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos2.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos2.registry.model.McpNacosConstant;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	public NamingService nacosNamingService(NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosMcpRegistryProperties.getServiceNamespace());
		try {
			return NamingFactory.createNamingService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public NacosConfigService nacosConfigService(NacosMcpProperties nacosMcpProperties,
			NacosMcpRegistryProperties nacosMcpRegistryProperties) {
		Properties nacosProperties = nacosMcpProperties.getNacosProperties();
		nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosMcpRegistryProperties.getServiceNamespace());
		try {
			return new NacosConfigService(nacosProperties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean(name = "server2NamedTransport")
	public Map<String, List<NamedClientMcpTransport>> server2NamedTransport(
			NacosMcpSseClientProperties nacosMcpSseClientProperties, NamingService namingService,
			ObjectProvider<WebClient.Builder> webClientBuilderProvider,
			NacosMcpRegistryProperties nacosMcpRegistryProperties, ObjectProvider<ObjectMapper> objectMapperProvider) {
		Map<String, List<NamedClientMcpTransport>> server2NamedTransport = new HashMap<>();
		WebClient.Builder webClientBuilderTemplate = (WebClient.Builder) webClientBuilderProvider
			.getIfAvailable(WebClient::builder);
		ObjectMapper objectMapper = (ObjectMapper) objectMapperProvider.getIfAvailable(ObjectMapper::new);

		Map<String, String> connections = nacosMcpSseClientProperties.getConnections();
		connections.forEach((serviceKey, serviceName) -> {
			try {
				List<Instance> instances = namingService.selectInstances(
						serviceName + McpNacosConstant.SERVER_NAME_SUFFIX, nacosMcpRegistryProperties.getServiceGroup(),
						true);
				List<NamedClientMcpTransport> namedTransports = new ArrayList<>();
				for (Instance instance : instances) {
					String url = instance.getMetadata().getOrDefault("scheme", "http") + "://" + instance.getIp() + ":"
							+ instance.getPort();

					WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(url);
					WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);
					namedTransports
						.add(new NamedClientMcpTransport(serviceName + "-" + instance.getInstanceId(), transport));
				}

				server2NamedTransport.put(serviceName, namedTransports);
			}
			catch (NacosException e) {
				logger.error("nacos naming service: {} error", serviceName, e);
			}
		});
		return server2NamedTransport;
	}

}
