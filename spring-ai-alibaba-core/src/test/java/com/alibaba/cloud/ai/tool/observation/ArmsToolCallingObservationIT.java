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
package com.alibaba.cloud.ai.tool.observation;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.cloud.ai.tool.MockWeatherService;
import com.alibaba.cloud.ai.tool.MockWeatherService.Request;
import com.alibaba.cloud.ai.tool.MockWeatherService.Response;
import com.alibaba.cloud.ai.tool.ObservableToolCallingManager;
import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationDocumentation.HighCardinalityKeyNames;
import com.alibaba.cloud.ai.tool.observation.ArmsToolCallingObservationDocumentation.LowCardinalityKeyNames;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

/**
 * Integration tests for DashScope Chat functionality. These tests will only run if
 * AI_DASHSCOPE_API_KEY environment variable is set.
 *
 * @author Lumian
 * @since 1.0.0-M6.1
 */
@Tag("integration")
@SpringBootTest(classes = ArmsToolCallingObservationIT.Config.class)
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ArmsToolCallingObservationIT {

	// Test constants
	private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode";

	private static final String TEST_MODEL = "qwen-max-latest";

	private static final String API_KEY_ENV = "AI_DASHSCOPE_API_KEY";

	private static final Logger logger = LoggerFactory.getLogger(ArmsToolCallingObservationIT.class);

	@Autowired
	TestObservationRegistry observationRegistry;

	@Autowired
	ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Get API key from environment variable
		String apiKey = System.getenv(API_KEY_ENV);
		// Skip tests if API key is not set
		Assumptions.assumeTrue(apiKey != null && !apiKey.trim().isEmpty(),
				"Skipping tests because " + API_KEY_ENV + " environment variable is not set");
	}

	@Test
	void functionCallSupplier() {

		Map<String, Object> state = new ConcurrentHashMap<>();

	// @formatter:off
    String response = ChatClient.create(this.chatModel).prompt()
        .user("Turn the light on in the living room")
				.toolCallbacks(FunctionToolCallback.builder("turnsLightOnInTheLivingRoom", () -> state.put("Light", "ON"))
						.description("Get the weather in location")
						.inputType(MockWeatherService.Request.class)
						.build())
        .call()
        .content();
    // @formatter:on

		logger.info("Response: {}", response);
		assertThat(state).containsEntry("Light", "ON");

		validate();
	}

	@Test
	void functionCallTest() {
		functionCallTest(OpenAiChatOptions.builder()
			.model(TEST_MODEL)
			.toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", new MockWeatherService())
				.description("Get the weather in location")
				.inputType(MockWeatherService.Request.class)
				.build()))
			.build());
	}

	@Test
	void functionCallWithToolContextTest() {

		var biFunction = new BiFunction<Request, ToolContext, Response>() {

			@Override
			public Response apply(Request request, ToolContext toolContext) {

				assertThat(toolContext.getContext()).containsEntry("sessionId", "123");

				double temperature = 0;
				if (request.location().contains("Paris")) {
					temperature = 15;
				}
				else if (request.location().contains("Tokyo")) {
					temperature = 10;
				}
				else if (request.location().contains("San Francisco")) {
					temperature = 30;
				}

				return new MockWeatherService.Response(temperature, 15, 20, 2, 53, 45, MockWeatherService.Unit.C);
			}

		};

		functionCallTest(OpenAiChatOptions.builder()
			.model(TEST_MODEL)
			.toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", biFunction)
				.description("Get the weather in location")
				.inputType(MockWeatherService.Request.class)
				.build()))
			.toolContext(Map.of("sessionId", "123"))
			.build());
	}

	@Test
	void streamFunctionCallTest() {

		streamFunctionCallTest(OpenAiChatOptions.builder()
			.model(TEST_MODEL)
			.toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", new MockWeatherService())
				.description("Get the weather in location")
				.inputType(MockWeatherService.Request.class)
				.build()))
			.build());
	}

	@Test
	void streamFunctionCallWithToolContextTest() {

		var biFunction = new BiFunction<MockWeatherService.Request, ToolContext, MockWeatherService.Response>() {

			@Override
			public Response apply(Request request, ToolContext toolContext) {

				assertThat(toolContext.getContext()).containsEntry("sessionId", "123");

				double temperature = 0;
				if (request.location().contains("Paris")) {
					temperature = 15;
				}
				else if (request.location().contains("Tokyo")) {
					temperature = 10;
				}
				else if (request.location().contains("San Francisco")) {
					temperature = 30;
				}

				return new MockWeatherService.Response(temperature, 15, 20, 2, 53, 45, MockWeatherService.Unit.C);
			}

		};

		OpenAiChatOptions promptOptions = OpenAiChatOptions.builder()
			.toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", biFunction)
				.description("Get the weather in location")
				.inputType(MockWeatherService.Request.class)
				.build()))
			.toolContext(Map.of("sessionId", "123"))
			.build();

		streamFunctionCallTest(promptOptions);
	}

	void streamFunctionCallTest(OpenAiChatOptions promptOptions) {

		UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");

		List<Message> messages = new ArrayList<>(List.of(userMessage));

		Flux<ChatResponse> response = this.chatModel.stream(new Prompt(messages, promptOptions));

		String content = response.collectList()
			.block()
			.stream()
			.map(ChatResponse::getResults)
			.flatMap(List::stream)
			.map(Generation::getOutput)
			.map(AssistantMessage::getText)
			.collect(Collectors.joining());
		logger.info("Response: {}", content);

		assertThat(content).contains("30", "10", "15");

		validate();
	}

	void functionCallTest(OpenAiChatOptions promptOptions) {

		UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");

		List<Message> messages = new ArrayList<>(List.of(userMessage));

		ChatResponse response = this.chatModel.call(new Prompt(messages, promptOptions));

		logger.info("Response: {}", response);

		assertThat(response.getResult().getOutput().getText()).contains("30", "10", "15");

		validate();
	}

	private void validate() {
		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.doesNotHaveAnyRemainingCurrentObservation()
			.hasObservationWithNameEqualTo(ArmsToolCallingObservationConvention.DEFAULT_OPERATION_NAME)
			.that()
			.hasContextualNameEqualTo(
					ArmsToolCallingObservationConvention.DEFAULT_OPERATION_NAME + " getCurrentWeather")
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
					ArmsToolCallingObservationConvention.DEFAULT_OPERATION_NAME)
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.GEN_AI_SPAN_KIND.asString(),
					ArmsToolCallingObservationConvention.SPAN_KIND)
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.GEN_AI_FRAMEWORK.asString(),
					ArmsToolCallingObservationConvention.FRAMEWORK)
			.hasHighCardinalityKeyValueWithKey(HighCardinalityKeyNames.GEN_AI_TOOL_CALL_ID.asString())
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.GEN_AI_TOOL_NAME.asString(), "getCurrentWeather")
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.TOOL_NAME.asString(), "getCurrentWeather")
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.TOOL_DESCRIPTION.asString(),
					"Get the weather in location")
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.TOOL_RETURN_DIRECT.asString(), "false")
			.hasHighCardinalityKeyValueWithKey(HighCardinalityKeyNames.TOOL_PARAMETERS.asString())
			.hasHighCardinalityKeyValueWithKey(HighCardinalityKeyNames.OUTPUT_VALUE.asString())
			.hasBeenStarted()
			.hasBeenStopped();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public OpenAiApi chatApi() {
			return OpenAiApi.builder()
				.baseUrl(BASE_URL)
				.apiKey(new SimpleApiKey(System.getenv(API_KEY_ENV)))
				.completionsPath("/v1/chat/completions")
				.embeddingsPath("/v1/embeddings")
				.build();
		}

		@Bean
		public OpenAiChatModel openAiClient(OpenAiApi openAiApi, ToolCallingManager toolCallingManager,
				TestObservationRegistry observationRegistry) {
			return OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(OpenAiChatOptions.builder().model(TEST_MODEL).topP(0.7).build())
				.toolCallingManager(toolCallingManager)
				.observationRegistry(observationRegistry)
				.build();
		}

		@Bean
		public ToolCallingManager toolCallingManager(TestObservationRegistry observationRegistry) {
			return ObservableToolCallingManager.builder().observationRegistry(observationRegistry).build();
		}

		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

	}

}
