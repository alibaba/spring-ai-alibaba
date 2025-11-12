package com.alibaba.cloud.ai.examples.deepresearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpRequest;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;

@SpringBootApplication
public class ExamplesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamplesApplication.class, args);
	}

	@Bean
	public McpSyncHttpClientRequestCustomizer mcpSyncHttpClientRequestCustomizer() {
		return new McpSyncHttpClientRequestCustomizer() {
			@Override
			public void customize(HttpRequest.Builder builder, String method, URI endpoint, String body, McpTransportContext context) {
				builder.header("Authorization", "Bearer " + System.getenv("JINA_API_KEY"));
				builder.timeout(java.time.Duration.ofSeconds(120));
			}
		};
	}

}
