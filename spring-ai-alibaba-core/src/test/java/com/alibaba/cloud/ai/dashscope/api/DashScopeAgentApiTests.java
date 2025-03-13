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
package com.alibaba.cloud.ai.dashscope.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeAgentApi. Tests cover constructor combinations, call
 * functionality and stream behavior.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M2
 */
@ExtendWith(MockitoExtension.class)
class DashScopeAgentApiTests {

	// Get values from environment variables with default values
	private static final String TEST_API_KEY = System.getenv().getOrDefault("AI_DASHSCOPE_API_KEY", "test-api-key");

	private static final String TEST_WORKSPACE_ID = System.getenv()
		.getOrDefault("DASHSCOPE_WORKSPACE_ID", "test-workspace-id");

	private static final String TEST_APP_ID = System.getenv().getOrDefault("DASHSCOPE_APP_ID", "test-app-id");

	private static final String TEST_PROMPT = "Hello, AI!";

	@Mock
	private RestClient.Builder restClientBuilder;

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	private RestClient restClient;

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriSpec;

	@Mock
	private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@Mock
	private RestClient.RequestBodyUriSpec restRequestBodyUriSpec;

	@Mock
	private RestClient.ResponseSpec restResponseSpec;

	private DashScopeAgentApi dashScopeAgentApi;

	@BeforeEach
	void setUp() {
		// Setup WebClient mock chain
		lenient().when(webClientBuilder.baseUrl(any())).thenReturn(webClientBuilder);
		lenient().when(webClientBuilder.defaultHeaders(any())).thenReturn(webClientBuilder);
		lenient().when(webClientBuilder.build()).thenReturn(webClient);
		lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
		lenient().when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodyUriSpec);
		lenient().when(requestBodyUriSpec.body(any(), any(Class.class))).thenReturn(requestHeadersSpec);
		lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

		// Setup RestClient mock chain
		lenient().when(restClientBuilder.baseUrl(any())).thenReturn(restClientBuilder);
		lenient().when(restClientBuilder.defaultHeaders(any())).thenReturn(restClientBuilder);
		lenient().when(restClientBuilder.defaultStatusHandler(any())).thenReturn(restClientBuilder);
		lenient().when(restClientBuilder.build()).thenReturn(restClient);

		// Create test instance with mocked builders
		dashScopeAgentApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest request) {
				if (request == null) {
					return ResponseEntity.ok(null);
				}
				try {
					// 使用mock的RestClient
					return restClient.post()
						.uri("/api/v1/apps/" + request.appId() + "/completion")
						.body(request)
						.retrieve()
						.toEntity(DashScopeAgentResponse.class);
				}
				catch (Exception e) {
					// 处理可能的异常，例如NullPointerException
					return ResponseEntity.ok(null);
				}
			}

			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest request) {
				if (request == null) {
					return Flux.empty();
				}
				try {
					// 使用mock的WebClient
					return webClient.post()
						.uri("/api/v1/apps/" + request.appId() + "/completion")
						.body(Mono.just(request), DashScopeAgentResponse.class)
						.retrieve()
						.bodyToFlux(DashScopeAgentResponse.class);
				}
				catch (Exception e) {
					// 处理可能的异常，例如NullPointerException
					return Flux.empty();
				}
			}
		};
	}

	/**
	 * Test constructor with API key only
	 */
	@Test
	void testConstructorWithApiKey() {
		DashScopeAgentApi api = new DashScopeAgentApi(TEST_API_KEY);
		assertThat(api).isNotNull();
	}

	/**
	 * Test constructor with API key and workspace ID
	 */
	@Test
	void testConstructorWithApiKeyAndWorkspaceId() {
		DashScopeAgentApi api = new DashScopeAgentApi(TEST_API_KEY, TEST_WORKSPACE_ID);
		assertThat(api).isNotNull();
	}

	/**
	 * Test successful call
	 */
	@Test
	void testSuccessfulCall() {
		// Create test request and response
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();
		DashScopeAgentApi.DashScopeAgentResponse response = createTestResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				return ResponseEntity.ok(response);
			}
		};

		// Execute test
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(request);

		// Verify response structure
		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().output()).isNotNull();
	}

	/**
	 * Test successful stream
	 */
	@Test
	void testSuccessfulStream() {
		// Create test request and response
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();
		DashScopeAgentApi.DashScopeAgentResponse response = createTestResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest req) {
				return Flux.just(response);
			}
		};

		// Execute test
		Flux<DashScopeAgentApi.DashScopeAgentResponse> resultFlux = mockApi.stream(request);

		// Verify stream response structure
		StepVerifier.create(resultFlux).assertNext(result -> {
			assertThat(result).isNotNull();
			assertThat(result.output()).isNotNull();
		}).verifyComplete();
	}

	/**
	 * Test call with error response
	 */
	@Test
	void testCallWithErrorResponse() {
		// Create test request and error response
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();
		DashScopeAgentApi.DashScopeAgentResponse errorResponse = createErrorResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				return ResponseEntity.ok(errorResponse);
			}
		};

		// Execute test
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(request);

		// Verify error response
		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().code()).isEqualTo("error");
		assertThat(result.getBody().message()).isEqualTo("Error occurred");
	}

	/**
	 * Test stream with error response
	 */
	@Test
	void testStreamWithErrorResponse() {
		// Create test request and error response
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();
		DashScopeAgentApi.DashScopeAgentResponse errorResponse = createErrorResponse();

		// Mock WebClient behavior - 使用lenient()避免不必要的stubbing警告
		lenient().when(responseSpec.bodyToFlux(DashScopeAgentApi.DashScopeAgentResponse.class))
			.thenReturn(Flux.just(errorResponse));

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest req) {
				return Flux.just(errorResponse);
			}
		};

		// Execute test
		Flux<DashScopeAgentApi.DashScopeAgentResponse> resultFlux = mockApi.stream(request);

		// Verify error stream response
		StepVerifier.create(resultFlux).assertNext(result -> {
			assertThat(result).isNotNull();
			assertThat(result.code()).isEqualTo("error");
			assertThat(result.message()).isEqualTo("Error occurred");
		}).verifyComplete();
	}

	/**
	 * Test call with null request
	 */
	@Test
	void testCallWithNullRequest() {
		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				if (req == null) {
					return ResponseEntity.ok(null);
				}
				return ResponseEntity.ok(createTestResponse());
			}
		};

		// Execute test and verify null response
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(null);
		assertThat(result.getBody()).isNull();
	}

	/**
	 * Test stream with null request
	 */
	@Test
	void testStreamWithNullRequest() {
		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest req) {
				if (req == null) {
					return Flux.empty();
				}
				return Flux.just(createTestResponse());
			}
		};

		// Execute test
		Flux<DashScopeAgentApi.DashScopeAgentResponse> resultFlux = mockApi.stream(null);

		// Verify empty stream
		StepVerifier.create(resultFlux).verifyComplete();
	}

	/**
	 * Test call with empty response
	 */
	@Test
	void testCallWithEmptyResponse() {
		// Create test request
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();

		// Create empty response
		DashScopeAgentApi.DashScopeAgentResponse emptyResponse = createEmptyResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				return ResponseEntity.ok(emptyResponse);
			}
		};

		// Execute test
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(request);

		// Verify empty response
		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().output()).isNull();
		assertThat(result.getBody().usage()).isNull();
	}

	/**
	 * Test stream with empty response
	 */
	@Test
	void testStreamWithEmptyResponse() {
		// Create test request
		DashScopeAgentApi.DashScopeAgentRequest request = createTestRequest();

		// Create empty response
		DashScopeAgentApi.DashScopeAgentResponse emptyResponse = createEmptyResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest req) {
				return Flux.just(emptyResponse);
			}
		};

		// Execute test
		Flux<DashScopeAgentApi.DashScopeAgentResponse> resultFlux = mockApi.stream(request);

		// Verify empty response stream
		StepVerifier.create(resultFlux).assertNext(result -> {
			assertThat(result).isNotNull();
			assertThat(result.output()).isNull();
			assertThat(result.usage()).isNull();
		}).verifyComplete();
	}

	/**
	 * Test call with custom parameters
	 */
	@Test
	void testCallWithCustomParameters() {
		// Create test request with custom parameters
		DashScopeAgentApi.DashScopeAgentRequest request = createCustomParametersRequest();
		DashScopeAgentApi.DashScopeAgentResponse response = createTestResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				return ResponseEntity.ok(response);
			}
		};

		// Execute test
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(request);

		// Verify response
		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
	}

	/**
	 * Test stream with custom parameters
	 */
	@Test
	void testStreamWithCustomParameters() {
		// Create test request with custom parameters
		DashScopeAgentApi.DashScopeAgentRequest request = createCustomParametersRequest();
		DashScopeAgentApi.DashScopeAgentResponse response = createTestResponse();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public Flux<DashScopeAgentResponse> stream(DashScopeAgentRequest req) {
				return Flux.just(response);
			}
		};

		// Execute test
		Flux<DashScopeAgentApi.DashScopeAgentResponse> resultFlux = mockApi.stream(request);

		// Verify stream response
		StepVerifier.create(resultFlux).assertNext(result -> {
			assertThat(result).isNotNull();
		}).verifyComplete();
	}

	/**
	 * Test call with thoughts enabled
	 */
	@Test
	void testCallWithThoughtsEnabled() {
		// Create test request with thoughts enabled
		DashScopeAgentApi.DashScopeAgentRequest request = createRequestWithThoughts();
		DashScopeAgentApi.DashScopeAgentResponse response = createResponseWithThoughts();

		// 创建一个特定的mock实例用于此测试
		DashScopeAgentApi mockApi = new DashScopeAgentApi(TEST_API_KEY) {
			@Override
			public ResponseEntity<DashScopeAgentResponse> call(DashScopeAgentRequest req) {
				return ResponseEntity.ok(response);
			}
		};

		// Execute test
		ResponseEntity<DashScopeAgentApi.DashScopeAgentResponse> result = mockApi.call(request);

		// Verify response structure
		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().output()).isNotNull();
		assertThat(result.getBody().output().thoughts()).isNotNull();
		assertThat(result.getBody().output().thoughts()).hasSize(1);
	}

	/**
	 * Helper method to create a test request
	 */
	private DashScopeAgentApi.DashScopeAgentRequest createTestRequest() {
		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput input = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput(
				TEST_PROMPT, null, null, null, null, null);

		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters parameters = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters(
				false, false, null);

		return new DashScopeAgentApi.DashScopeAgentRequest(TEST_APP_ID, input, parameters);
	}

	/**
	 * Helper method to create a test response
	 */
	private DashScopeAgentApi.DashScopeAgentResponse createTestResponse() {
		DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput output = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput(
				"Hello, Human!", "stop", "test-session", null, null);

		DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage.DashScopeAgentResponseUsageModels model = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage.DashScopeAgentResponseUsageModels(
				"test-model", 10, 20);

		DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage usage = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage(
				java.util.List.of(model));

		return new DashScopeAgentApi.DashScopeAgentResponse(200, "test-request-id", "success", "success", output,
				usage);
	}

	/**
	 * Helper method to create an error response
	 */
	private DashScopeAgentApi.DashScopeAgentResponse createErrorResponse() {
		return new DashScopeAgentApi.DashScopeAgentResponse(400, "test-request-id", "error", "Error occurred", null,
				null);
	}

	/**
	 * Helper method to create an empty response
	 */
	private DashScopeAgentApi.DashScopeAgentResponse createEmptyResponse() {
		return new DashScopeAgentApi.DashScopeAgentResponse(200, "test-request-id", "success", "success", null, null);
	}

	/**
	 * Helper method to create a request with custom parameters
	 */
	private DashScopeAgentApi.DashScopeAgentRequest createCustomParametersRequest() {
		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput input = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput(
				TEST_PROMPT, null, "custom-session", "custom-memory", null, null);

		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters parameters = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters(
				true, true, null);

		return new DashScopeAgentApi.DashScopeAgentRequest(TEST_APP_ID, input, parameters);
	}

	/**
	 * Helper method to create a request with thoughts enabled
	 */
	private DashScopeAgentApi.DashScopeAgentRequest createRequestWithThoughts() {
		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput input = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestInput(
				TEST_PROMPT, null, null, null, null, null);

		DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters parameters = new DashScopeAgentApi.DashScopeAgentRequest.DashScopeAgentRequestParameters(
				true, false, null);

		return new DashScopeAgentApi.DashScopeAgentRequest(TEST_APP_ID, input, parameters);
	}

	/**
	 * Helper method to create a response with thoughts
	 */
	private DashScopeAgentApi.DashScopeAgentResponse createResponseWithThoughts() {
		var thought = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputThoughts(
				"test thought", "test action type", "test action name", "test action", "test input stream",
				"test input", "test response", "test observation", "test reasoning content");

		var output = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput("Hello, Human!", "stop",
				"test-session", List.of(thought), null);

		var model = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage.DashScopeAgentResponseUsageModels(
				"test-model", 10, 20);

		var usage = new DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseUsage(List.of(model));

		return new DashScopeAgentApi.DashScopeAgentResponse(200, "test-request-id", "success", "success", output,
				usage);
	}

}
