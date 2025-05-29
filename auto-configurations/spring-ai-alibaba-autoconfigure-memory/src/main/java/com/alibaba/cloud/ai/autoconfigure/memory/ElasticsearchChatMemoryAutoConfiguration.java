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

package com.alibaba.cloud.ai.autoconfigure.memory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.alibaba.cloud.ai.memory.elasticsearch.ElasticsearchChatMemoryRepository;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;

/**
 * Auto-configuration for ElasticSearch chat memory repository.
 */
@ConditionalOnClass({ ElasticsearchChatMemoryRepository.class, ElasticsearchClient.class })
@ConditionalOnProperty(prefix = "spring.ai.memory.elasticsearch", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@EnableConfigurationProperties(ElasticsearchChatMemoryProperties.class)
public class ElasticsearchChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchChatMemoryAutoConfiguration.class);

	@Bean
	@Qualifier("elasticsearchChatMemoryRepository")
	@ConditionalOnMissingBean(name = "elasticsearchChatMemoryRepository")
	ElasticsearchChatMemoryRepository elasticsearchChatMemoryRepository(ElasticsearchChatMemoryProperties properties)
			throws Exception {
		logger.info("Configuring elasticsearch chat memory repository");
		// Create HttpHosts for all nodes
		HttpHost[] httpHosts;
		if (!CollectionUtils.isEmpty(properties.getNodes())) {
			httpHosts = properties.getNodes().stream().map(node -> {
				String[] parts = node.split(":");
				return new HttpHost(parts[0], Integer.parseInt(parts[1]), properties.getScheme());
			}).toArray(HttpHost[]::new);
		}
		else {
			// Fallback to single node configuration
			httpHosts = new HttpHost[] {
					new HttpHost(properties.getHost(), properties.getPort(), properties.getScheme()) };
		}

		var restClientBuilder = RestClient.builder(httpHosts);

		// Add authentication if credentials are provided
		if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));

			// Create SSL context if using HTTPS
			if ("https".equalsIgnoreCase(properties.getScheme())) {
				SSLContext sslContext = SSLContextBuilder.create()
					.loadTrustMaterial(null, (chains, authType) -> true)
					.build();

				restClientBuilder.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
							.setSSLContext(sslContext)
							.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE));
			}
			else {
				restClientBuilder.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
			}
		}

		// Create the transport and client
		ElasticsearchTransport transport = new RestClientTransport(restClientBuilder.build(), new JacksonJsonpMapper());
		ElasticsearchClient elasticsearchClient = new ElasticsearchClient(transport);
		return new ElasticsearchChatMemoryRepository(elasticsearchClient);
	}

}
