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
package com.alibaba.cloud.ai.memory.redis.serializer;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.memory.redis.JedisRedisChatMemoryRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.MimeTypeUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * MediaSerializerTest
 *
 * @author benym
 * @since 2025/9/2 22:21
 */
@SpringBootTest(classes = MediaSerializerTest.TestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
@Testcontainers
public class MediaSerializerTest {

	private static final Logger logger = LoggerFactory.getLogger(MediaSerializerTest.class);

	private ChatClient chatClient;

	@Autowired
	private ChatMemoryRepository chatMemoryRepository;

	private String conversationId;

	private ObjectMapper objectMapper;

	private static final int REDIS_PORT = 6379;

	// Define and start Redis container
	@Container
	private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
		.withExposedPorts(REDIS_PORT);

	/**
	 * Dynamically configure Redis properties
	 */
	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.redis.host", redisContainer::getHost);
		registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
		// Create DashScope ChatModel instance
		ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
		// Bound memory
		this.conversationId = UUID.randomUUID().toString();
		this.chatClient = ChatClient.builder(chatModel)
			.defaultAdvisors(MessageChatMemoryAdvisor
				.builder(MessageWindowChatMemory.builder()
					.chatMemoryRepository(chatMemoryRepository)
					.maxMessages(100)
					.build())
				.conversationId(conversationId)
				.build())
			.build();
		this.objectMapper = JsonMapper.builder()
			.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
			.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.build();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
	}

	/**
	 * 天气工具类，用于演示工具的实际调用
	 */
	public static class WeatherTool {

		@Tool(name = "weather_tool", description = "获取指定城市的天气信息")
		public String getWeather(@ToolParam(description = "城市名称") String city,
				@ToolParam(description = "当前时间戳") String currentTimestamp) {
			logger.info("WeatherTool invoked with city: {}, timestamp: {}", city, currentTimestamp);
			return String.format("{\"city\": \"%s\", \"temperature\": -50, \"time\": \"%s\"}", city, currentTimestamp);
		}

	}

	@Test
	public void serializeForMediaStringData() {
		var userMessage = UserMessage.builder()
			.text("Explain what do you see on this picture?")
			.media(List.of(Media.builder()
				.mimeType(MimeTypeUtils.IMAGE_PNG)
				.data(URI.create("https://docs.spring.io/spring-ai/reference/_images/multimodal.test.png"))
				.build()))
			.build();
		ObjectMapper objectMapper = JsonMapper.builder()
			.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
			.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.build();
		assertDoesNotThrow(() -> {
			objectMapper.writeValueAsString(userMessage);
		});
	}

	@Test
	public void serializeForMediaByteData() {
		var userMessage = UserMessage.builder()
			.text("Explain what do you see on this picture?")
			.media(List.of(Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data(new byte[] { 1, 2, 3 }).build()))
			.build();
		assertDoesNotThrow(() -> {
			objectMapper.writeValueAsString(userMessage);
		});
	}

	@Test
	public void serializeUseToolCall() throws GraphStateException {
		ToolCallback toolCallback = ToolCallbacks.from(new WeatherTool())[0];
		ReactAgent weatherAgent = ReactAgent.builder()
			.name("weather_agent")
			.instruction("你是一个天气专家，可以帮助用户查询天气")
			.chatClient(chatClient)
			.tools(List.of(toolCallback))
			.build();
		try {
			Optional<OverAllState> result = weatherAgent
				.invoke(Map.of("messages", List.of(new UserMessage("查询北京的天气"))));
			OverAllState overAllState = result.get();
			logger.info("result: {}", overAllState);
			Map<String, Object> data = overAllState.data();
			JsonNode messageNode = objectMapper.valueToTree(data.get("messages"));
			List<Message> deserializedMessages = objectMapper.readValue(messageNode.toString(), new TypeReference<>() {
			});
			logger.info("Deserialized messages: {}", deserializedMessages);
			Message userDeserializedMessage = deserializedMessages.get(0);
			assertThat(userDeserializedMessage.getMessageType()).isEqualTo(MessageType.USER);
			Message assistantDeserializedMessage = deserializedMessages.get(1);
			assertThat(assistantDeserializedMessage.getMessageType()).isEqualTo(MessageType.ASSISTANT);
			Message toolResponseDeserializedMessage = deserializedMessages.get(2);
			assertThat(toolResponseDeserializedMessage.getMessageType()).isEqualTo(MessageType.TOOL);
			List<Message> testMessage = chatMemoryRepository.findByConversationId(conversationId);
			logger.info("testMessage: {}", testMessage);
			assertThat(testMessage).hasSize(4);
			var userMessage = testMessage.get(0);
			Message assistantMessage = testMessage.get(1);
			assertThat(userMessage.getText()).isEqualTo("查询北京的天气");
			assertThat(assistantMessage.getMessageType()).isEqualTo(MessageType.ASSISTANT);
		}
		catch (CompletionException | GraphStateException | GraphRunnerException | JsonProcessingException e) {
			logger.error("Error occurred while invoking ReactAgent", e);
		}
	}

	@SpringBootConfiguration
	static class TestConfiguration {

		@Bean
		ChatMemoryRepository chatMemoryRepository() {
			// Use Redis connection information from container to create Redis repository
			return JedisRedisChatMemoryRepository.builder()
				.host(redisContainer.getHost())
				.port(redisContainer.getMappedPort(REDIS_PORT))
				.build();
		}

	}

}
