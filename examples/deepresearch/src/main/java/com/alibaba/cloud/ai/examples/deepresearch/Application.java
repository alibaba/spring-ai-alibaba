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
package com.alibaba.cloud.ai.examples.deepresearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpRequest;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
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

	@Bean
	public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener(Environment environment) {
		return event -> {
			String port = environment.getProperty("server.port", "8080");
			String contextPath = environment.getProperty("server.servlet.context-path", "");
			String accessUrl = "http://localhost:" + port + contextPath + "/chatui/index.html";
			System.out.println("\n🎉========================================🎉");
			System.out.println("✅ Application is ready!");
			System.out.println("🚀 Chat with you agent: " + accessUrl);
			System.out.println("🎉========================================🎉\n");
		};
	}

}
