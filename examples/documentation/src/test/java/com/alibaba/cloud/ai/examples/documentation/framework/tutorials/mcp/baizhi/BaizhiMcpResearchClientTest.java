/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp.baizhi;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the optional Baizhi example against a local Streamable HTTP MCP server.
 *
 * <p>
 * All credentials are synthetic. These tests verify initialization, discovery, the
 * explicit tool selection, schema preservation, real callback dispatch, authentication
 * failure, missing-tool failure, timeouts, result semantics and resource cleanup. They do
 * not contact the hosted Baizhi service or a model provider, and they are not evidence of
 * production compatibility.
 *
 * @author Spring AI Alibaba
 */
class BaizhiMcpResearchClientTest {

	private static final String SYNTHETIC_TOKEN = "synthetic-local-test-token";

	private static final List<String> REQUIRED_TOOLS = BaizhiMcpResearchConfig.DEFAULT_REQUIRED_TOOLS;

	@Test
	void initializesDiscoversAndRegistersOnlyExplicitlyRequiredTools() {
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN,
				defaultTools(new AtomicReference<>()));
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN))) {

			assertThat(client.selectedToolNames()).containsExactlyElementsOf(REQUIRED_TOOLS);
			assertThat(client.toolCallbacks()).hasSize(3);
			assertThat(client.toolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
				.containsExactlyElementsOf(REQUIRED_TOOLS);
			assertThat(server.lastAuthorizationHeader()).isEqualTo("Bearer " + SYNTHETIC_TOKEN);
		}
	}

	@Test
	void preservesTheDiscoveredInputSchema() {
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN,
				defaultTools(new AtomicReference<>()));
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN))) {

			ToolCallback search = findCallback(client, "websearch_search");

			assertThat(search.getToolDefinition().description()).isEqualTo("Search the public web");
			assertThat(search.getToolDefinition().inputSchema()).contains("\"query\"").contains("\"required\"");
		}
	}

	@Test
	void dispatchesTheRealToolCallbackAndReturnsTextContent() {
		AtomicReference<Map<String, Object>> receivedArguments = new AtomicReference<>();
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN,
				defaultTools(receivedArguments));
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN))) {

			ToolCallback scrape = findCallback(client, "web_scrape");
			String result = scrape.call("{\"url\":\"https://example.com/article\"}");

			assertThat(result).isEqualTo("Page content for https://example.com/article");
			assertThat(receivedArguments.get()).containsEntry("url", "https://example.com/article");
		}
	}

	@Test
	void failsWhenARequiredToolIsMissingInsteadOfFallingBackToAllTools() {
		List<McpServerFeatures.SyncToolSpecification> onlyTwoTools = defaultTools(new AtomicReference<>()).subList(0,
				2);
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, onlyTwoTools)) {
			assertThatThrownBy(() -> BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("web_extract")
				.hasMessageContaining("falling back");
		}
	}

	@Test
	void failsBeforeNetworkWhenTheApiKeyIsMissing() {
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, defaultTools(new AtomicReference<>()))) {
			Map<String, String> environment = Map.of(BaizhiMcpResearchConfig.SERVER_URI_ENV, server.url(),
					BaizhiMcpResearchConfig.API_KEY_ENV, "  ");

			assertThatThrownBy(() -> BaizhiMcpResearchConfig.fromEnvironment(environment))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(BaizhiMcpResearchConfig.API_KEY_ENV);
			assertThat(server.requestCount()).isZero();
		}
	}

	@Test
	void rejectsAWrongApiKeyDuringInitialization() {
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, defaultTools(new AtomicReference<>()))) {
			assertThatThrownBy(() -> BaizhiMcpResearchClient.connect(config(server, "wrong-synthetic-token")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("failed");

			assertThat(server.lastAuthorizationHeader()).isEqualTo("Bearer wrong-synthetic-token");
			assertThat(server.requestCount()).isPositive();
			assertThat(server.unauthorizedRequestCount()).isPositive();
		}
	}

	@Test
	void surfacesATimeoutAsAToolExecutionFailure() {
		List<McpServerFeatures.SyncToolSpecification> slowTools = List.of(
				textTool("websearch_search", "Search the public web", Map.of("query", stringSchema()), List.of("query"),
						"search"),
				LocalBaizhiMcpServer.tool("web_scrape", "Read a public page", Map.of("url", stringSchema()),
						List.of("url"), (exchange, arguments) -> {
							try {
								Thread.sleep(3_000);
							}
							catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
							return McpSchema.CallToolResult.builder().addTextContent("late page content").build();
						}),
				textTool("web_extract", "Extract structured information",
						Map.of("url", stringSchema(), "instruction", stringSchema()), List.of("url"), "extract"));
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, slowTools);
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(
						config(server, SYNTHETIC_TOKEN, Duration.ofSeconds(1)))) {

			ToolCallback scrape = findCallback(client, "web_scrape");
			assertThatThrownBy(() -> scrape.call("{\"url\":\"https://example.com/slow\"}"))
				.isInstanceOf(ToolExecutionException.class);
		}
	}

	@Test
	void rejectsUnsupportedNonTextContentExplicitly() {
		List<McpServerFeatures.SyncToolSpecification> imageTools = List.of(
				textTool("websearch_search", "Search the public web", Map.of("query", stringSchema()), List.of("query"),
						"search"),
				LocalBaizhiMcpServer.tool("web_scrape", "Read a public page", Map.of("url", stringSchema()),
						List.of("url"), (exchange, arguments) -> McpSchema.CallToolResult.builder()
							.addContent(new McpSchema.ImageContent(null, "c3ludGhldGlj", "image/png"))
							.build()),
				textTool("web_extract", "Extract structured information",
						Map.of("url", stringSchema(), "instruction", stringSchema()), List.of("url"), "extract"));
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, imageTools);
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN))) {

			ToolCallback scrape = findCallback(client, "web_scrape");
			assertThatThrownBy(() -> scrape.call("{\"url\":\"https://example.com/image\"}"))
				.isInstanceOf(ToolExecutionException.class)
				.hasMessageContaining("non-text");
		}
	}

	@Test
	void propagatesAnMcpErrorResultWithoutTreatingItAsSuccess() {
		List<McpServerFeatures.SyncToolSpecification> failingTools = List.of(
				textTool("websearch_search", "Search the public web", Map.of("query", stringSchema()), List.of("query"),
						"search"),
				LocalBaizhiMcpServer.tool("web_scrape", "Read a public page", Map.of("url", stringSchema()),
						List.of("url"), (exchange, arguments) -> McpSchema.CallToolResult.builder()
							.addTextContent("synthetic upstream failure")
							.isError(true)
							.build()),
				textTool("web_extract", "Extract structured information",
						Map.of("url", stringSchema(), "instruction", stringSchema()), List.of("url"), "extract"));
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN, failingTools);
				BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN))) {

			ToolCallback scrape = findCallback(client, "web_scrape");
			assertThatThrownBy(() -> scrape.call("{\"url\":\"https://example.com/error\"}"))
				.isInstanceOf(ToolExecutionException.class)
				.hasMessageContaining("error result")
				.hasMessageContaining("synthetic upstream failure");
		}
	}

	@Test
	void closesTheSessionAndToleratesRepeatedClose() {
		try (LocalBaizhiMcpServer server = new LocalBaizhiMcpServer(SYNTHETIC_TOKEN,
				defaultTools(new AtomicReference<>()))) {
			BaizhiMcpResearchClient client = BaizhiMcpResearchClient.connect(config(server, SYNTHETIC_TOKEN));
			ToolCallback search = findCallback(client, "websearch_search");

			client.close();
			client.close();

			assertThatThrownBy(() -> search.call("{\"query\":\"after close\"}"))
				.isInstanceOf(ToolExecutionException.class);
		}
	}

	@Test
	void rejectsMoreThanThreeExplicitTools() {
		assertThatThrownBy(() -> new BaizhiMcpResearchConfig(URI.create("https://example.com/mcp"), "synthetic-token",
				List.of("a", "b", "c", "d"), Duration.ofSeconds(1), Duration.ofSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("between 1 and 3");
	}

	@Test
	void rejectsPlainHttpForNonLoopbackServersBeforeNetworkUse() {
		assertThatThrownBy(() -> new BaizhiMcpResearchConfig(URI.create("http://example.com/mcp"), "synthetic-token",
				REQUIRED_TOOLS, Duration.ofSeconds(1), Duration.ofSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("HTTPS");
	}

	@Test
	void rejectsCredentialsEmbeddedInTheServerUrl() {
		assertThatThrownBy(() -> new BaizhiMcpResearchConfig(URI.create("https://user:secret@example.com/mcp"),
				"synthetic-token", REQUIRED_TOOLS, Duration.ofSeconds(1), Duration.ofSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("embed credentials");
	}

	private static BaizhiMcpResearchConfig config(LocalBaizhiMcpServer server, String token) {
		return config(server, token, Duration.ofSeconds(10));
	}

	private static BaizhiMcpResearchConfig config(LocalBaizhiMcpServer server, String token,
			Duration requestTimeout) {
		return new BaizhiMcpResearchConfig(URI.create(server.url()), token, REQUIRED_TOOLS, Duration.ofSeconds(5),
				requestTimeout);
	}

	private static ToolCallback findCallback(BaizhiMcpResearchClient client, String name) {
		return client.toolCallbacks()
			.stream()
			.filter(callback -> callback.getToolDefinition().name().equals(name))
			.findFirst()
			.orElseThrow();
	}

	private static List<McpServerFeatures.SyncToolSpecification> defaultTools(
			AtomicReference<Map<String, Object>> scrapedArguments) {
		return List.of(
				textTool("websearch_search", "Search the public web", Map.of("query", stringSchema()), List.of("query"),
						"search"),
				LocalBaizhiMcpServer.tool("web_scrape", "Read a public page", Map.of("url", stringSchema()),
						List.of("url"), (exchange, arguments) -> {
							scrapedArguments.set(arguments);
							return McpSchema.CallToolResult.builder()
								.addTextContent("Page content for " + arguments.get("url"))
								.build();
						}),
				textTool("web_extract", "Extract structured information",
						Map.of("url", stringSchema(), "instruction", stringSchema()), List.of("url"), "extract"),
				textTool("unused_extra_tool", "Discoverable but not selected", Map.of(), List.of(), "unused"));
	}

	private static McpServerFeatures.SyncToolSpecification textTool(String name, String description,
			Map<String, Object> properties, List<String> required, String resultPrefix) {
		return LocalBaizhiMcpServer.tool(name, description, properties, required,
				(exchange, arguments) -> McpSchema.CallToolResult.builder()
					.addTextContent(resultPrefix + " result for " + arguments)
					.build());
	}

	private static Map<String, Object> stringSchema() {
		return Map.of("type", "string");
	}

}
