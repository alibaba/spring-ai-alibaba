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
import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.BodyData;
import com.alibaba.cloud.ai.graph.node.HttpNode.BodyType;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.utils.InMemoryFileStorage;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpNodeTest {

	private MockWebServer mockWebServer;

	private WebClient webClient;

	private String baseUrl;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		baseUrl = mockWebServer.url("/").toString();
		webClient = WebClient.create(baseUrl);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void testHttpGetSuccess() throws Exception {
		mockWebServer.enqueue(new MockResponse().setBody("{\"message\":\"success\"}")
			.setHeader(HttpHeaders.CONTENT_TYPE, "application/json"));

		String url = mockWebServer.url("/test").toString();
		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.GET)
			.url(url)
			.body(new HttpRequestNodeBody())
			.build();

		OverAllState state = new OverAllState();
		Map<String, Object> result = node.apply(state);

		Map<String, Object> messages = (Map<String, Object>) result.get("messages");
		assertEquals(HttpStatus.OK.value(), messages.get("status"));
		Map<String, Object> body = (Map<String, Object>) messages.get("body");
		assertEquals("success", body.get("message"));

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("GET", request.getMethod());
		assertEquals("/test", request.getPath());
	}

	@Test
	void testVariableReplacement() throws Exception {
		mockWebServer.enqueue(new MockResponse().setBody("OK"));

		String url = baseUrl + "${pathVar}";
		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.GET)
			.url(url)
			.header("X-Header", "${headerVal}")
			.queryParam("param", "${queryVal}")
			.build();

		OverAllState state = new OverAllState(
				Map.of("pathVar", "users", "headerVal", "test-header", "queryVal", "test-query"));

		node.apply(state);

		RecordedRequest request = mockWebServer.takeRequest();
		HttpUrl httpUrl = request.getRequestUrl();
		assertEquals("/users", httpUrl.encodedPath());
		assertEquals("test-query", httpUrl.queryParameter("param"));
		assertEquals("test-header", request.getHeader("X-Header"));
	}

	@Test
	void testRawTextBodyVariableReplacement() throws Exception {
		mockWebServer.enqueue(new MockResponse().setBody("OK"));

		String url = mockWebServer.url("/echo").toString();
		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.POST)
			.url(url)
			.body(HttpRequestNodeBody.from("Hello ${name}"))
			.build();

		OverAllState state = new OverAllState(Map.of("name", "Alice"));
		node.apply(state);

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("Hello Alice", request.getBody().readUtf8());
		assertEquals("text/plain", request.getHeader("Content-Type"));
	}

	@Test
	void testFormUrlencodedBodyVariableReplacement() throws Exception {
		mockWebServer.enqueue(new MockResponse().setBody("OK"));

		HttpRequestNodeBody formBody = new HttpRequestNodeBody();
		formBody.setType(BodyType.X_WWW_FORM_URLENCODED);

		BodyData d1 = new BodyData();
		d1.setType(BodyType.X_WWW_FORM_URLENCODED);
		d1.setKey("field1");
		d1.setValue("${val1}");
		BodyData d2 = new BodyData();
		d2.setType(BodyType.X_WWW_FORM_URLENCODED);
		d2.setKey("field2");
		d2.setValue("${val2}");
		formBody.setData(List.of(d1, d2));

		String url = mockWebServer.url("/form").toString();
		HttpNode node = HttpNode.builder().webClient(webClient).method(HttpMethod.POST).url(url).body(formBody).build();

		OverAllState state = new OverAllState(Map.of("val1", "v1", "val2", "v2"));
		node.apply(state);

		RecordedRequest request = mockWebServer.takeRequest();

		assertEquals("field1=v1&field2=v2", request.getBody().readUtf8());
		assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
	}

	@Test
	void testPlainTextResponse() throws Exception {
		mockWebServer
			.enqueue(new MockResponse().setBody("plain response").setHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));

		String url = mockWebServer.url("/plain").toString();
		HttpNode node = HttpNode.builder().webClient(webClient).method(HttpMethod.GET).url(url).build();

		Map<String, Object> result = node.apply(new OverAllState());
		Map<String, Object> messages = (Map<String, Object>) result.get("messages");

		assertEquals(HttpStatus.OK.value(), messages.get("status"));
		assertEquals("plain response", messages.get("body"));
	}

	@Test
	void testNon2xxResponse() throws Exception {
		mockWebServer.enqueue(new MockResponse().setResponseCode(404)
			.setBody("{\"error\":\"Not Found\"}")
			.setHeader(HttpHeaders.CONTENT_TYPE, "application/json"));

		String url = mockWebServer.url("/notfound").toString();
		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.GET)
			.url(url)
			.retryConfig(new RetryConfig(0, 0, false))
			.build();

		Map<String, Object> result = node.apply(new OverAllState());
		Map<String, Object> messages = (Map<String, Object>) result.get("messages");

		assertEquals(404, messages.get("status"));
		Map<String, Object> body = (Map<String, Object>) messages.get("body");
		assertEquals("Not Found", body.get("error"));
	}

	@Test
	void testApply_WithFileResponse() throws Exception {
		InputStream is = getClass().getResourceAsStream("/test.png");
		assertNotNull(is, "测试资源 test.png 未找到，请将文件放在 src/test/resources/ 根目录下");
		byte[] fileBytes = is.readAllBytes();
		MockResponse mockResponse = new MockResponse().setResponseCode(200)
			.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.png\"")
			.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
			.setBody(new okio.Buffer().write(fileBytes));
		mockWebServer.enqueue(mockResponse);

		String url = mockWebServer.url("/test.png").toString();
		HttpNode node = HttpNode.builder().webClient(webClient).url(url).build();

		Map<String, Object> result = node.apply(new OverAllState());
		Map<String, Object> messages = (Map<String, Object>) result.get("messages");

		assertTrue(messages.containsKey("files"), "应包含 files 键");
		@SuppressWarnings("unchecked")
		List<String> files = (List<String>) messages.get("files");
		String fileId = files.get(0);
		assertNotNull(fileId, "应有 File ID");

		InMemoryFileStorage.FileRecord record = InMemoryFileStorage.get(fileId);
		assertNotNull(record, "应能通过 ID 获取缓存的 FileRecord");
		assertEquals("test.png", record.getName(), "record 名称应为 test.png");
		assertArrayEquals(fileBytes, record.getContent(), "缓存的内容应与原始字节一致");
		InMemoryFileStorage.clear();
	}

	@Test
	void testBasicAuth() throws Exception {
		mockWebServer.enqueue(new MockResponse().setBody("OK"));

		String url = mockWebServer.url("/secure").toString();
		AuthConfig authConfig = AuthConfig.basic("user", "pass");

		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.GET)
			.url(url)
			.auth(authConfig)
			.build();

		node.apply(new OverAllState());

		RecordedRequest request = mockWebServer.takeRequest();
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		assertNotNull(authHeader);
		assertTrue(authHeader.startsWith("Basic "));
	}

	@Test
	void testRetryOnNetworkFailure() {
		mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
		mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
		mockWebServer.enqueue(new MockResponse().setBody("OK").setHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));

		String url = mockWebServer.url("/retry-fail").toString();
		HttpNode node = HttpNode.builder()
			.webClient(webClient)
			.method(HttpMethod.GET)
			.url(url)
			.retryConfig(new RetryConfig(3, 1000, true))
			.build();

		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(new OverAllState()));
		Map<String, Object> messages = (Map<String, Object>) result.get("messages");
		assertEquals(HttpStatus.OK.value(), messages.get("status"));

		assertEquals(3, mockWebServer.getRequestCount());
	}

	@Test
	void testJsonBodyAndVariableReplace() throws Exception {
		mockWebServer.enqueue(new MockResponse().setResponseCode(200)
			.setBody("OK")
			.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain"));
		OverAllState state = new OverAllState();
		state.registerKeyAndStrategy("key1", new ReplaceStrategy());
		state.registerKeyAndStrategy("key2", new ReplaceStrategy());
		state.registerKeyAndStrategy("key3", new ReplaceStrategy());
		String nestedJson = """
				{
				  "user": {
				    "id": 123,
				    "name": "Alice",
				    "contact": {
				      "email": "alice@example.com",
				      "phones": [
				        {"type": "mobile", "number": "123-4567"},
				        {"type": "work", "number": "987-6543"}
				      ]
				    }
				  },
				  "order": {
				    "orderId": "ORD-001",
				    "items": [
				      {"sku": "A123", "qty": 2, "price": 50.0},
				      {"sku": "B456", "qty": 1, "price": 25.5}
				    ]
				  }
				}""";

		state.updateState(Map.of("key1", "value1", "key2",
				"```json\n{\"person\":{\"name\":\"Tom\",\"age\":28,\"address\":{\"city\":\"Beijing\",\"zipcode\":\"100000\"}}}\n```",
				"key3", nestedJson));

		String myJson = "{" + "\"type\": \"JSON\", " + "\"data\": {" + "\"key1out\": \"${key1}\", "
				+ "\"key2out\": \"${key2}\", " + "\"key3out\": \"${key3}\"" + "}" + "}";

		HttpNode node = HttpNode.builder()
			.url(mockWebServer.url("/mock").toString())
			.method(HttpMethod.POST)
			.header("Content-Type", "application/json")
			.body(HttpRequestNodeBody.fromJson(myJson))
			.retryConfig(new RetryConfig(3, 100, true))
			.outputKey("http_node_output")
			.build();

		Map<String, Object> result = assertDoesNotThrow(() -> node.apply(state));
		Map<String, Object> messages = (Map<String, Object>) result.get("messages");
		assertEquals(HttpStatus.OK.value(), messages.get("status"));

		RecordedRequest request = mockWebServer.takeRequest();
		assertEquals("/mock", request.getPath());
		assertEquals("POST", request.getMethod());
		assertEquals("application/json", request.getHeader("Content-Type"));

		String expectedBody = "{" + "\"key1out\": \"value1\", "
				+ "\"key2out\": {\"person\":{\"name\":\"Tom\",\"age\":28,\"address\":{\"city\":\"Beijing\",\"zipcode\":\"100000\"}}}, "
				+ "\"key3out\": " + nestedJson + "}";
		assertEquals(expectedBody.replaceAll("\\s+", ""), request.getBody().readUtf8().replaceAll("\\s+", ""));

	}

}
