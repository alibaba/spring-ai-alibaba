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
package com.alibaba.cloud.ai.examples.deepresearch;

import java.net.URI;
import java.net.http.HttpRequest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class McpSearchConfigurationTest {

	private static final String CONNECTIONS = "spring.ai.mcp.client.streamable-http.connections.";

	@Test
	void profilesSelectOneResearchConnection() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ProfileConfiguration.class)
			.web(WebApplicationType.NONE)
			.run()) {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty(CONNECTIONS + "jina.url")).isEqualTo("https://mcp.jina.ai");
			assertThat(environment.getProperty(CONNECTIONS + "parallel.url")).isNull();
		}

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ProfileConfiguration.class)
			.web(WebApplicationType.NONE)
			.run("--spring.profiles.active=parallel")) {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty(CONNECTIONS + "parallel.url"))
				.isEqualTo("https://search.parallel.ai");
			assertThat(environment.getProperty(CONNECTIONS + "parallel.endpoint")).isEqualTo("/mcp");
			assertThat(environment.getProperty(CONNECTIONS + "jina.url")).isNull();
		}
	}

	@Test
	void jinaCredentialIsNotSentToParallel() {
		MockEnvironment environment = new MockEnvironment()
			.withProperty(CONNECTIONS + "jina.url", "https://mcp.jina.ai")
			.withProperty(CONNECTIONS + "parallel.url", "https://search.parallel.ai")
			.withProperty("JINA_API_KEY", "jina-secret");
		var customizer = new Application().mcpSyncHttpClientRequestCustomizer(environment);
		HttpRequest.Builder parallelRequest = HttpRequest.newBuilder(URI.create("https://search.parallel.ai/mcp"));
		customizer.customize(parallelRequest, "POST", URI.create("https://search.parallel.ai/mcp"), "", null);
		assertThat(parallelRequest.build().headers().firstValue("Authorization")).isEmpty();
		assertThat(parallelRequest.build().headers().firstValue("User-Agent"))
			.contains("spring-ai-alibaba-deepresearch/0.0.1-SNAPSHOT");

		HttpRequest.Builder jinaRequest = HttpRequest.newBuilder(URI.create("https://mcp.jina.ai/sse"));
		customizer.customize(jinaRequest, "POST", URI.create("https://mcp.jina.ai/sse"), "", null);
		assertThat(jinaRequest.build().headers().firstValue("Authorization")).contains("Bearer jina-secret");
	}

	@Configuration(proxyBeanMethods = false)
	static class ProfileConfiguration {
	}

}
