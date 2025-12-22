package com.alibaba.cloud.ai.studio.admin.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient(ElasticsearchProperties properties) {
        try {
            URL url = new URL(properties.getUrl());
            
            return RestClient.builder(
                    new HttpHost(url.getHost(), url.getPort(), url.getProtocol()))
                .setRequestConfigCallback(requestConfigBuilder -> 
                    requestConfigBuilder
                        .setConnectTimeout(properties.getConnectTimeout())
                        .setSocketTimeout(properties.getSocketTimeout()))
                .setHttpClientConfigCallback(httpClientBuilder -> 
                    httpClientBuilder
                        .setMaxConnTotal(properties.getConnectionPool().getMaxConnections())
                        .setMaxConnPerRoute(properties.getConnectionPool().getMaxIdleConnections()))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("创建RestClient失败", e);
        }
    }

    @Bean
    public RestClientTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClientTransport transport) {
        return new ElasticsearchClient(transport);
    }
}