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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.context.CheckpointContextCompaction;
import com.alibaba.cloud.ai.graph.agent.context.ToolResultArchive;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.Message;

import static com.alibaba.cloud.ai.examples.documentation.framework.advanced.OpenAiContextManagementExample.COORDINATOR;
import static com.alibaba.cloud.ai.examples.documentation.framework.advanced.OpenAiContextManagementExample.OLD_NOTE;
import static com.alibaba.cloud.ai.examples.documentation.framework.advanced.OpenAiContextManagementExample.TASK;
import static org.assertj.core.api.Assertions.assertThat;

class OpenAiContextManagementExampleTest {

	@Test
	void runsTheAgentChainAcrossNativeOpenAiFallbackAndADurableRestart(@TempDir Path workspace) throws Exception {
		try (var wire = new OpenAiEndpoint()) {
			try {
				var settings = new OpenAiContextManagementExample.Settings(wire.url(), "fixture", "small", "wide", 16_000);
				InterruptionMetadata pause;
				try (var before = new OpenAiContextManagementExample(workspace, settings)) {
					pause = before.start("context-acceptance");
					assertThat(pause.toolFeedbacks()).hasSize(1);
					assertThat(pause.toolFeedbacks().get(0).getName()).isEqualTo("record_audit");
					assertThat(before.routeSelections).hasValue(1);
					assertThat(before.providerOverflows.get()).isPositive();
					assertThat(before.reportFetches).hasValue(1);
					assertThat(before.auditRecordings).hasValue(0);
					assertThat(wire.coordinatorCalls).isEqualTo(4);
					assertThat(wire.analystCalls).isEqualTo(3);
				}

				try (var restarted = new OpenAiContextManagementExample(workspace, settings)) {
					var result = restarted.resume("context-acceptance", OpenAiContextManagementExample.approve(pause));
					assertThat(result).isNotNull().isNotInstanceOf(InterruptionMetadata.class);
					assertThat(restarted.routeSelections).hasValue(0);
					assertThat(restarted.providerOverflows.get()).isPositive();
					assertThat(restarted.reportFetches).hasValue(0);
					assertThat(restarted.auditRecordings).hasValue(1);
					assertThat(result.state().value(CheckpointContextCompaction.stateKey(COORDINATOR))).isPresent();
					assertThat(result.state().<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
						.contains(OLD_NOTE, "Case CX-42 closed after approval and archive review.");
					assertThat(result.state().value("audit_result")).isEmpty();
				}
				assertThat(wire.summaryCalls).isEqualTo(1);
				assertThat(wire.primaryCalls).isGreaterThanOrEqualTo(7);
				assertThat(wire.selectionCalls).isPositive();
				assertThat(wire.coordinatorCalls).isEqualTo(7);
				assertThat(wire.analystCalls).isEqualTo(6);
			}
			finally {
				wire.assertHealthy();
			}
		}
	}

	private static final class OpenAiEndpoint implements AutoCloseable {

		private static final ObjectMapper JSON = new ObjectMapper();

		private static final Pattern ARCHIVE_REFERENCE = Pattern.compile("reference \"([a-f0-9]{64})\"");

		private static final String SUMMARY = "Earlier inventory work is closed.";

		private final HttpServer server;

		private final AtomicReference<Throwable> failure = new AtomicReference<>();

		private volatile int summaryCalls;

		private volatile int primaryCalls;

		private volatile int selectionCalls;

		private volatile int coordinatorCalls;

		private volatile int analystCalls;

		private int primaryAtLastFallback;

		private String archiveReference;

		OpenAiEndpoint() throws IOException {
			this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			this.server.createContext("/v1/chat/completions", this::handle);
			this.server.start();
		}

		String url() {
			return "http://127.0.0.1:" + this.server.getAddress().getPort() + "/v1";
		}

		private void handle(HttpExchange exchange) throws IOException {
			try (exchange) {
				try {
					assertThat(exchange.getRequestMethod()).isEqualTo("POST");
					assertThat(exchange.getRequestURI().getPath()).isEqualTo("/v1/chat/completions");
					assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer fixture");
					var request = JSON.readTree(exchange.getRequestBody());
					String system = text(request, "system");
					if (request.path("model").asText().equals("small")) {
						assertThat(system).contains("You coordinate a report audit");
						assertThat(request.path("stream").asBoolean()).isTrue();
						assertThat(request.path("max_tokens").asInt()).isEqualTo(200);
						this.primaryCalls++;
						send(exchange, 400, "application/json", """
								{"error":{"message":"Context too long","type":"invalid_request_error",
								 "param":"messages","code":"context_length_exceeded"}}
								""");
						return;
					}
					assertThat(request.path("model").asText()).isEqualTo("wide");
					assertThat(request.path("max_tokens").asInt()).isEqualTo(600);
					Reply reply;
					if (system.contains("Summarize conversation history")) {
						this.summaryCalls++;
						assertThat(text(request, "user")).contains(OLD_NOTE).doesNotContain(TASK);
						reply = Reply.answer(SUMMARY);
					}
					else if (system.contains("Rank tools by relevance")) {
						this.selectionCalls++;
						assertThat(text(request, "user")).contains("fetch_report", "lookup_reference");
						reply = Reply.answer("{\"tools\":[\"fetch_report\"]}");
					}
					else if (system.contains("You are the audit analyst")) {
						reply = analyst(request);
					}
					else {
						assertThat(system).contains("You coordinate a report audit");
						reply = coordinator(request);
					}
					complete(exchange, request.path("stream").asBoolean(), reply);
				}
				catch (Exception | AssertionError error) {
					this.failure.compareAndSet(null, error);
					send(exchange, 500, "application/json", "{\"error\":{\"message\":\"Unexpected fixture request\"}}");
				}
			}
		}

		private Reply coordinator(JsonNode request) throws IOException {
			assertThat(request.path("stream").asBoolean()).isTrue();
			assertThat(this.primaryCalls).isGreaterThan(this.primaryAtLastFallback);
			this.primaryAtLastFallback = this.primaryCalls;
			assertThat(text(request, "user")).contains(TASK, SUMMARY).doesNotContain(OLD_NOTE);
			assertThat(text(request, "tool")).doesNotContain("End of synthetic report");
			assertThat(tools(request)).contains("audit_analyst", ToolResultArchive.READ_TOOL_NAME);
			return switch (this.coordinatorCalls++) {
				case 0 -> {
					assertThat(tools(request)).contains("search_tools", "load_tools").doesNotContain("fetch_report");
					yield Reply.tool("search-tool", "search_tools", Map.of("query", "inventory"));
				}
				case 1 -> {
					assertThat(result(request, "search-tool")).contains("fetch_report").doesNotContain("inputSchema");
					assertThat(tools(request)).doesNotContain("fetch_report");
					yield Reply.tool("load-tool", "load_tools", Map.of("names", List.of("fetch_report")));
				}
				case 2 -> {
					assertThat(result(request, "load-tool")).contains("fetch_report");
					assertThat(tools(request)).contains("fetch_report").doesNotContain("lookup_reference");
					yield Reply.tool("fetch-report", "fetch_report", Map.of());
				}
				case 3 -> {
					var archived = result(request, "fetch-report");
					assertThat(archived).contains("Tool result archived").doesNotContain("End of synthetic report");
					var matcher = ARCHIVE_REFERENCE.matcher(archived);
					assertThat(matcher.find()).isTrue();
					this.archiveReference = matcher.group(1);
					yield Reply.tool("delegate", "audit_analyst", Map.of("input", "Audit the delegated inventory case."));
				}
				case 4 -> {
					var result = JSON.readTree(result(request, "delegate"));
					assertThat(result.path("answer").asText()).contains("policy-42");
					assertThat(result.path("outputs").path("audit_result").asText()).contains("policy-42");
					assertThat(result.path("outputs").size()).isEqualTo(1);
					assertThat(result.toString()).doesNotContain("delivery_policy", "Report audit", "parent-internal");
					yield Reply.tool("page-one", ToolResultArchive.READ_TOOL_NAME,
							Map.of("reference", this.archiveReference, "page", 1));
				}
				case 5 -> {
					var page = JSON.readTree(result(request, "page-one"));
					assertThat(page.path("text").asText()).contains("Inventory result for case CX-42");
					assertThat(page.path("nextPage").asInt()).isEqualTo(2);
					yield Reply.tool("page-two", ToolResultArchive.READ_TOOL_NAME,
							Map.of("reference", page.path("reference").asText(), "page", page.path("nextPage").asInt()));
				}
				case 6 -> {
					var page = JSON.readTree(result(request, "page-two"));
					assertThat(page.path("page").asInt()).isEqualTo(2);
					assertThat(page.path("text").asText()).contains("15 units verified");
					yield Reply.answer("Case CX-42 closed after approval and archive review.");
				}
				default -> throw new AssertionError("Unexpected coordinator request");
			};
		}

		private Reply analyst(JsonNode request) throws IOException {
			assertThat(request.path("stream").asBoolean()).isFalse();
			assertThat(text(request, "user")).contains("Audit the delegated inventory case.", "\"case_id\":\"CX-42\"")
				.doesNotContain(TASK, OLD_NOTE, "parent-internal", "demo-tenant");
			assertThat(tools(request)).contains("search_skills", "read_skill", "search_memory", "read_memory")
				.doesNotContain("fetch_report", "audit_analyst");
			return switch (this.analystCalls++) {
				case 0 -> {
					assertThat(text(request, "tool")).isEmpty();
					assertThat(text(request, "system")).doesNotContain("Review an inventory report", "policy-42");
					yield Reply.tool("search-skill", "search_skills", Map.of("query", "audit"));
				}
				case 1 -> {
					assertThat(result(request, "search-skill")).contains("report-audit")
						.doesNotContain("After approval, use the case delivery label");
					yield Reply.tool("read-skill", "read_skill", Map.of("skill_name", "report-audit"));
				}
				case 2 -> {
					assertThat(result(request, "read-skill")).contains("After approval, use the case delivery label");
					yield Reply.tool("record", "record_audit", Map.of());
				}
				case 3 -> {
					assertThat(result(request, "record")).contains("Approved audit recorded for CX-42");
					assertThat(text(request, "tool")).doesNotContain("policy-42");
					yield Reply.tool("search-memory", "search_memory", Map.of("query", "delivery label"));
				}
				case 4 -> {
					assertThat(result(request, "search-memory")).isEqualTo("[\"delivery_policy\"]");
					yield Reply.tool("read-memory", "read_memory", Map.of("key", "delivery_policy"));
				}
				case 5 -> {
					var page = JSON.readTree(result(request, "read-memory"));
					assertThat(JSON.readTree(page.path("content").asText()).path("label").asText()).isEqualTo("policy-42");
					yield Reply.answer("Audit recorded for CX-42 with delivery label policy-42.");
				}
				default -> throw new AssertionError("Unexpected analyst request");
			};
		}

		private static List<String> tools(JsonNode request) {
			List<String> tools = new ArrayList<>();
			request.path("tools").forEach(tool -> tools.add(tool.path("function").path("name").asText()));
			return tools;
		}

		private static String result(JsonNode request, String id) {
			for (JsonNode message : request.path("messages")) {
				if ("tool".equals(message.path("role").asText()) && id.equals(message.path("tool_call_id").asText())) {
					return content(message.path("content"));
				}
			}
			throw new AssertionError("Missing tool result for " + id);
		}

		private static String text(JsonNode request, String role) {
			var texts = new ArrayList<String>();
			for (JsonNode message : request.path("messages")) {
				if (role.equals(message.path("role").asText())) {
					texts.add(content(message.path("content")));
				}
			}
			return String.join("\n", texts);
		}

		private static String content(JsonNode content) {
			if (content.isArray()) {
				var text = new StringBuilder();
				content.forEach(part -> text.append(part.path("text").asText()));
				return text.toString();
			}
			return content.asText("");
		}

		private static void complete(HttpExchange exchange, boolean stream, Reply reply) throws IOException {
			var message = JSON.createObjectNode().put("role", "assistant");
			String finishReason = "stop";
			if (reply.name() == null) {
				message.put("content", reply.text());
			}
			else {
				finishReason = "tool_calls";
				var call = message.putArray("tool_calls").addObject().put("id", reply.id()).put("type", "function");
				if (stream) {
					call.put("index", 0);
				}
				call.putObject("function").put("name", reply.name())
					.put("arguments", JSON.writeValueAsString(reply.arguments()));
			}
			if (stream) {
				String first = completion("chat.completion.chunk",
						JSON.createObjectNode().put("index", 0).putNull("finish_reason").set("delta", message));
				String last = completion("chat.completion.chunk",
						JSON.createObjectNode().put("index", 0).put("finish_reason", finishReason)
							.set("delta", JSON.createObjectNode()));
				send(exchange, 200, "text/event-stream", "data: " + first + "\n\ndata: " + last + "\n\ndata: [DONE]\n\n");
			}
			else {
				send(exchange, 200, "application/json", completion("chat.completion",
						JSON.createObjectNode().put("index", 0).put("finish_reason", finishReason).set("message", message)));
			}
		}

		private static String completion(String kind, JsonNode choice) throws IOException {
			return JSON.writeValueAsString(Map.of("id", "chatcmpl-acceptance", "object", kind,
					"created", 1, "model", "wide", "choices", List.of(choice)));
		}

		private static void send(HttpExchange exchange, int status, String type, String payload) throws IOException {
			byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", type);
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
		}

		void assertHealthy() {
			if (this.failure.get() != null) {
				throw new AssertionError("Unexpected request at the local OpenAI endpoint", this.failure.get());
			}
		}

		@Override
		public void close() {
			this.server.stop(0);
		}

	}

	private record Reply(String text, String id, String name, Map<String, Object> arguments) {

		static Reply answer(String text) {
			return new Reply(text, null, null, Map.of());
		}

		static Reply tool(String id, String name, Map<String, Object> arguments) {
			return new Reply(null, id, name, arguments);
		}

	}

}
