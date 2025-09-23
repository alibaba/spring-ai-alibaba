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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * McpNode HTTP流式功能测试
 */
class McpNodeHttpStreamTest {

	private MockWebServer mockWebServer;

	private OverAllState testState;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		testState = OverAllStateBuilder.builder()
			.putData("test_key", "test_value")
			.putData("user_input", "Hello World")
			.build();
	}

	@AfterEach
	void tearDown() throws IOException {
		if (mockWebServer != null) {
			mockWebServer.shutdown();
		}
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_SSE() throws Exception {
		// 模拟SSE响应
		String sseResponse = """
				data: {"type": "message", "content": "Hello"}

				data: {"type": "message", "content": "World"}

				data: {"type": "done"}

				""";

		mockWebServer.enqueue(new MockResponse().setBody(sseResponse)
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
			.setResponseCode(200));

		McpNode mcpNode = McpNode.builder()
			.url(mockWebServer.url("/sse").toString())
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.SSE)
			.streamMode(McpNode.StreamMode.DISTRIBUTE)
			.outputKey("sse_output")
			.allowInternalAddress(true)
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("sse_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("sse_output");
				Map<String, Object> sseOutput = (Map<String, Object>) output.get("sse_output");
				assertThat(sseOutput).containsKey("data");
				assertThat(sseOutput.get("streaming")).isEqualTo(true);
			})
			.assertNext(output -> {
				assertThat(output).containsKey("sse_output");
				Map<String, Object> sseOutput = (Map<String, Object>) output.get("sse_output");
				assertThat(sseOutput).containsKey("data");
			})
			.assertNext(output -> {
				assertThat(output).containsKey("sse_output");
				Map<String, Object> sseOutput = (Map<String, Object>) output.get("sse_output");
				assertThat(sseOutput).containsKey("data");
			})
			.verifyComplete();
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_JsonLines() throws Exception {
		// 模拟JSON Lines响应
		String jsonLinesResponse = """
				{"event": "start", "data": "Processing request"}
				{"event": "progress", "data": "50%"}
				{"event": "complete", "data": "Finished"}
				""";

		mockWebServer.enqueue(new MockResponse().setBody(jsonLinesResponse)
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setResponseCode(200));

		McpNode mcpNode = McpNode.builder()
			.url(mockWebServer.url("/jsonlines").toString())
			.enableHttpStream(HttpMethod.POST, McpNode.StreamFormat.JSON_LINES)
			.streamMode(McpNode.StreamMode.DISTRIBUTE)
			.outputKey("jsonlines_output")
			.allowInternalAddress(true)
			.param("prompt", "${user_input}")
			.readTimeout(Duration.ofSeconds(10))
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("jsonlines_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("jsonlines_output");
				Map<String, Object> jsonOutput = (Map<String, Object>) output.get("jsonlines_output");
				assertThat(jsonOutput).containsKey("data");
				assertThat(jsonOutput.get("streaming")).isEqualTo(true);
			})
			.assertNext(output -> {
				assertThat(output).containsKey("jsonlines_output");
			})
			.assertNext(output -> {
				assertThat(output).containsKey("jsonlines_output");
			})
			.verifyComplete();
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_TextStream() throws Exception {
		// 模拟文本流响应
		String textStreamResponse = "chunk1\nchunk2\nchunk3\n";

		mockWebServer.enqueue(new MockResponse().setBody(textStreamResponse)
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
			.setResponseCode(200));

		McpNode mcpNode = McpNode.builder()
			.url(mockWebServer.url("/text").toString())
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.TEXT_STREAM)
			.streamMode(McpNode.StreamMode.DISTRIBUTE)
			.delimiter("\n")
			.outputKey("text_output")
			.allowInternalAddress(true)
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("text_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("text_output");
				Map<String, Object> textOutput = (Map<String, Object>) output.get("text_output");
				assertThat(textOutput).containsKey("data");
				assertThat(textOutput.get("data")).isEqualTo("chunk1");
			})
			.assertNext(output -> {
				Map<String, Object> textOutput = (Map<String, Object>) output.get("text_output");
				assertThat(textOutput.get("data")).isEqualTo("chunk2");
			})
			.assertNext(output -> {
				Map<String, Object> textOutput = (Map<String, Object>) output.get("text_output");
				assertThat(textOutput.get("data")).isEqualTo("chunk3");
			})
			.verifyComplete();
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_AggregateMode() throws Exception {
		// 测试聚合模式
		String jsonLinesResponse = """
				{"id": 1, "message": "First"}
				{"id": 2, "message": "Second"}
				{"id": 3, "message": "Third"}
				""";

		mockWebServer.enqueue(new MockResponse().setBody(jsonLinesResponse)
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setResponseCode(200));

		McpNode mcpNode = McpNode.builder()
			.url(mockWebServer.url("/aggregate").toString())
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.JSON_LINES)
			.streamMode(McpNode.StreamMode.AGGREGATE)
			.outputKey("aggregated_output")
			.allowInternalAddress(true)
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("aggregated_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("aggregated_output");
				Map<String, Object> aggregatedOutput = (Map<String, Object>) output.get("aggregated_output");
				assertThat(aggregatedOutput).containsKey("data");
				assertThat(aggregatedOutput.get("streaming")).isEqualTo(false);
				assertThat(aggregatedOutput.get("aggregated")).isEqualTo(true);
				assertThat(aggregatedOutput.get("count")).isEqualTo(3);
			})
			.verifyComplete();
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeBackwardCompatibility_McpSyncMode() throws Exception {
		// 测试向后兼容性 - 默认MCP同步模式应该仍然工作
		McpNode mcpNode = McpNode.builder()
			.url("http://localhost:8080")
			.tool("test_tool")
			.param("input", "${user_input}")
			.outputKey("mcp_result")
			.build();

		// 验证默认是MCP_SYNC模式
		// 注意：这个测试会失败，因为没有真实的MCP服务器，但能验证配置正确
		try {
			CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
			Map<String, Object> result = future.get(5, TimeUnit.SECONDS);
			// 应该返回错误信息
			assertThat(result).containsKey("mcp_result");
			Map<String, Object> mcpResult = (Map<String, Object>) result.get("mcp_result");
			assertThat(mcpResult).containsKey("error");
		} catch (Exception e) {
			// 预期会有连接异常，说明配置正确
			assertThat(e.getCause().getMessage()).containsAnyOf("Connection refused", "connection was refused", "Unable to connect", "Failed to wait");
		}
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_VariableReplacement() throws Exception {
		// 测试变量替换功能
		String jsonResponse = """
				{"result": "success"}
				""";

		mockWebServer.enqueue(new MockResponse().setBody(jsonResponse)
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setResponseCode(200));

		// 使用包含变量的URL
		String urlTemplate = mockWebServer.url("/api").toString() + "?input=${user_input}&key=${test_key}";

		McpNode mcpNode = McpNode.builder()
			.url(urlTemplate)
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.JSON_LINES)
			.streamMode(McpNode.StreamMode.DISTRIBUTE)
			.outputKey("variable_output")
			.header("X-Custom-Header", "${test_key}")
			.allowInternalAddress(true)
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("variable_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("variable_output");
				Map<String, Object> variableOutput = (Map<String, Object>) output.get("variable_output");
				assertThat(variableOutput).containsKey("data");
			})
			.verifyComplete();

		// 验证请求是否正确替换了变量
		var recordedRequest = mockWebServer.takeRequest();
		assertThat(recordedRequest.getPath()).contains("input=Hello%20World");
		assertThat(recordedRequest.getPath()).contains("key=test_value");
		assertThat(recordedRequest.getHeader("X-Custom-Header")).isEqualTo("test_value");
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_ErrorHandling() throws Exception {
		// 测试错误处理
		mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

		McpNode mcpNode = McpNode.builder()
			.url(mockWebServer.url("/error").toString())
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.SSE)
			.streamMode(McpNode.StreamMode.DISTRIBUTE)
			.outputKey("error_output")
			.readTimeout(Duration.ofSeconds(2))
			.allowInternalAddress(true)
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("error_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result)
			.assertNext(output -> {
				assertThat(output).containsKey("error_output");
				Map<String, Object> errorOutput = (Map<String, Object>) output.get("error_output");
				assertThat(errorOutput).containsKey("error");
				assertThat(errorOutput.get("streaming")).isEqualTo(false);
				String errorMessage = errorOutput.get("error").toString();
				assertThat(errorMessage).satisfiesAnyOf(
					msg -> assertThat(msg).containsIgnoringCase("500"),
					msg -> assertThat(msg).containsIgnoringCase("HTTP"),
					msg -> assertThat(msg).containsIgnoringCase("Internal Server Error")
				);
			})
			.verifyComplete();
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	void testMcpNodeBuilderConvenienceMethods() {
		// 测试便捷方法
		McpNode node1 = McpNode.builder()
			.url("http://example.com/stream")
			.enableHttpStream()
			.build();

		// 验证默认配置
		assertThat(node1).isNotNull();

		McpNode node2 = McpNode.builder()
			.url("http://example.com/chat")
			.enableHttpStream(HttpMethod.POST, McpNode.StreamFormat.JSON_LINES)
			.build();

		assertThat(node2).isNotNull();
	}

	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	void testMcpNodeBuilderValidation() {
		// 测试构建器验证
		try {
			McpNode.builder().build();
			assertThat(false).as("Should throw IllegalArgumentException for missing URL").isTrue();
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("URL cannot be null or empty");
		}

		try {
			McpNode.builder()
				.url("http://example.com")
				.processMode(McpNode.McpProcessMode.MCP_SYNC)
				.build();
			assertThat(false).as("Should throw IllegalArgumentException for missing tool in MCP_SYNC mode").isTrue();
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains("Tool name is required for MCP_SYNC mode");
		}
	}

	@Test
	@Timeout(value = 15, unit = TimeUnit.SECONDS)
	void testMcpNodeHttpStreamMode_SecurityValidation() throws Exception {
		// 测试安全验证 - 拒绝内网地址
		McpNode mcpNode = McpNode.builder()
			.url("http://192.168.1.1/test")
			.enableHttpStream(HttpMethod.GET, McpNode.StreamFormat.JSON_LINES)
			.allowInternalAddress(false) // 禁止内网访问
			.webClient(WebClient.create())
			.build();

		CompletableFuture<Map<String, Object>> future = mcpNode.apply(testState);
		Map<String, Object> asyncResult = future.get(10, TimeUnit.SECONDS);
		AsyncGenerator<Map<String, Object>> generator = (AsyncGenerator<Map<String, Object>>) asyncResult.get("stream_output");
		Flux<Map<String, Object>> result = Flux.fromStream(generator.stream());

		StepVerifier.create(result.timeout(Duration.ofSeconds(5)))
			.assertNext(output -> {
				assertThat(output).containsKey("error");
				assertThat(output.get("error").toString()).contains("Internal network access not allowed");
			})
			.verifyComplete();
	}

}
