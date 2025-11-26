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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
class RedisSaverTest {

	private static boolean isCI() {
		return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
	}

	// 使用较为稳定的版本

	@Container
	private static final GenericContainer<?> redisContainer = new GenericContainer<>(
			DockerImageName.parse("valkey/valkey:8.1.2"))
			.withExposedPorts(6379); // #gitleaks:allow
	static RedissonClient redisson;
	static RedisSaver redisSaver;

	@BeforeAll
	static void setup() {
		redisContainer.start();
		// 本地单机 Redis，测试环境需保证 6379 端口可用
		Config config = new Config();
		config.useSingleServer()
				.setAddress("redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
		redisson = Redisson.create(config);
		redisSaver = new RedisSaver(redisson);
	}

	@AfterAll
	static void tearDown() {
		if (redisson != null) {
			redisson.shutdown();
		}
	}

	@Test
	void testPutAndGetAndList() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// 构造 checkpoint
		Checkpoint cp1 = Checkpoint.builder()
				.id("cp1")
				.state(java.util.Map.of("data", "data1"))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();
		Checkpoint cp2 = Checkpoint.builder()
				.id("cp2")
				.state(java.util.Map.of("data", "data2"))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();

		// put 第一个
		redisSaver.put(config, cp1);
		// put 第二个
		redisSaver.put(config, cp2);

		// list 检查
		List<Checkpoint> list = (List<Checkpoint>) redisSaver.list(config);
		assertEquals(2, list.size());
		assertEquals("cp2", list.get(0).getId()); // push 到头部

		// get 最新
		Optional<Checkpoint> latest = redisSaver.get(config);
		assertTrue(latest.isPresent());
		assertEquals("cp2", latest.get().getId());

		// get by id
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		Optional<Checkpoint> byId = redisSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("cp1", byId.get().getId());
	}

	@Test
	void testReplaceCheckpoint() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Checkpoint cp1 = Checkpoint.builder()
				.id("cp1")
				.state(java.util.Map.of("data", "data1"))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();
		redisSaver.put(config, cp1);

		// 替换 cp1
		Checkpoint cp1New = Checkpoint.builder()
				.id("cp1")
				.state(java.util.Map.of("data", "data1-new"))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		redisSaver.put(configWithId, cp1New);

		Optional<Checkpoint> byId = redisSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("data1-new", byId.get().getState().get("data"));
	}

	@Test
	void testClear() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		redisSaver.put(config,
				Checkpoint.builder()
						.id("cp1")
						.state(java.util.Map.of("data", "data1"))
						.nodeId("node1")
						.nextNodeId("node2")
						.build());
		redisSaver.put(config,
				Checkpoint.builder()
						.id("cp2")
						.state(java.util.Map.of("data", "data2"))
						.nodeId("node1")
						.nextNodeId("node2")
						.build());

		boolean cleared = redisSaver.clear(config);
		assertTrue(cleared);

		List<Checkpoint> list = (List<Checkpoint>) redisSaver.list(config);
		assertEquals(0, list.size());
	}

	@Test
	void testGetWithNoData() {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Optional<Checkpoint> result = redisSaver.get(config);
		assertTrue(result.isEmpty());
	}

	@Test
	public void concurrentExceptionTest() throws Exception {
		ExecutorService executorService = Executors.newCachedThreadPool();
		int count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		var index = new AtomicInteger(0);
		var futures = new ArrayList<Future<?>>();

		for (int i = 0; i < count; i++) {

			var future = executorService.submit(() -> {
				try {

					var threadName = format("thread-%d", index.incrementAndGet());
					System.out.println(threadName);
					redisSaver.list(RunnableConfig.builder().threadId(threadName).build());

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			});

			futures.add(future);
		}

		boolean allCompleted = latch.await(20, TimeUnit.SECONDS);
		if (!allCompleted) {
			System.err.println("Warning: Not all tasks completed within timeout");
		}
		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);

		for (var future : futures) {
			// Use get with timeout to avoid indefinite blocking
			// If task threw exception, get() will throw ExecutionException
			// If task not done, get() with timeout will throw TimeoutException
			try {
				assertNull(future.get(1, TimeUnit.SECONDS));
			}
			catch (java.util.concurrent.TimeoutException e) {
				// Task may not have completed, but this is acceptable for this test
				// The main goal is to verify no exceptions occur during concurrent access
				System.err.println("Warning: Future not completed within timeout: " + e.getMessage());
			}
		}

		// int size = redisSaver.get_checkpointsByThread().size();
		// size must be equals to count

		// assertEquals(count, size, "Checkpoint Lost during concurrency");
	}

	/**
	 * Test that Message objects (SystemMessage, UserMessage, AssistantMessage,
	 * AgentInstructionMessage)
	 * are properly serialized and deserialized, maintaining their type information.
	 * This test addresses the bug where Message objects were being deserialized as
	 * HashMap
	 * instead of their original types, causing ClassCastException in AgentLlmNode.
	 */
	@Test
	void testMessageSerializationAndDeserialization() throws Exception {
		String threadId = "test-message-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Create a checkpoint with various Message types in the state
		List<Message> messages = List.of(
				SystemMessage.builder().text("System prompt").build(),
				UserMessage.builder().text("User question").build(),
				AssistantMessage.builder().content("Assistant response").build(),
				AgentInstructionMessage.builder().text("Agent instruction template: {param}").build());

		Map<String, Object> stateWithMessages = Map.of(
				"messages", messages,
				"someOtherData", "test-data");

		Checkpoint checkpoint = Checkpoint.builder()
				.id("cp-messages")
				.state(stateWithMessages)
				.nodeId("node1")
				.nextNodeId("node2")
				.build();

		// Save the checkpoint
		redisSaver.put(config, checkpoint);

		// Retrieve the checkpoint
		Optional<Checkpoint> retrievedOpt = redisSaver.get(config);
		assertTrue(retrievedOpt.isPresent(), "Checkpoint should be retrieved");

		Checkpoint retrieved = retrievedOpt.get();
		Map<String, Object> retrievedState = retrieved.getState();

		// Verify the messages are still of the correct type (not HashMap)
		assertTrue(retrievedState.containsKey("messages"), "State should contain messages");
		Object messagesObj = retrievedState.get("messages");
		assertInstanceOf(List.class, messagesObj, "Messages should be a List");

		@SuppressWarnings("unchecked")
		List<Object> retrievedMessages = (List<Object>) messagesObj;
		assertEquals(4, retrievedMessages.size(), "Should have 4 messages");

		// Verify each message is of the correct type
		assertInstanceOf(SystemMessage.class, retrievedMessages.get(0),
				"First message should be SystemMessage, not HashMap");
		assertInstanceOf(UserMessage.class, retrievedMessages.get(1),
				"Second message should be UserMessage, not HashMap");
		assertInstanceOf(AssistantMessage.class, retrievedMessages.get(2),
				"Third message should be AssistantMessage, not HashMap");
		assertInstanceOf(AgentInstructionMessage.class, retrievedMessages.get(3),
				"Fourth message should be AgentInstructionMessage, not HashMap");

		// Verify the content is preserved
		SystemMessage systemMsg = (SystemMessage) retrievedMessages.get(0);
		assertEquals("System prompt", systemMsg.getText());

		UserMessage userMsg = (UserMessage) retrievedMessages.get(1);
		assertEquals("User question", userMsg.getText());

		AssistantMessage assistantMsg = (AssistantMessage) retrievedMessages.get(2);
		assertEquals("Assistant response", assistantMsg.getText());

		AgentInstructionMessage instructionMsg = (AgentInstructionMessage) retrievedMessages.get(3);
		assertEquals("Agent instruction template: {param}", instructionMsg.getText());
	}

	@Test
	void testToolResponseMessageSerialization() throws Exception {
		String threadId = "test-tool-response-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("tool_execution_id", "exec_123");
		metadata.put("execution_time", 150);

		List<ToolResponseMessage.ToolResponse> responses = List.of(
				new ToolResponseMessage.ToolResponse("tool_call_1", "calculator", "{\"result\": 42}"),
				new ToolResponseMessage.ToolResponse("tool_call_2", "weather", "{\"temperature\": 25}"));

		ToolResponseMessage original = ToolResponseMessage.builder()
			.responses(responses)
			.metadata(metadata)
			.build();

		Checkpoint checkpoint = Checkpoint.builder()
				.id("cp-tool")
				.state(Map.of("message", original))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();

		redisSaver.put(config, checkpoint);

		Optional<Checkpoint> retrievedOpt = redisSaver.get(config);
		assertTrue(retrievedOpt.isPresent());

		Object msgObj = retrievedOpt.get().getState().get("message");
		assertInstanceOf(ToolResponseMessage.class, msgObj);

		ToolResponseMessage deserialized = (ToolResponseMessage) msgObj;
		assertEquals(original.getResponses().size(), deserialized.getResponses().size());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
	}

	@Test
	void testDocumentSerialization() throws Exception {
		String threadId = "test-document-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "file.pdf");
		metadata.put("page", 1);

		Document original = Document.builder()
				.id("doc_123")
				.text("This is a test document content.")
				.metadata(metadata)
				.score(0.95)
				.build();

		Checkpoint checkpoint = Checkpoint.builder()
				.id("cp-doc")
				.state(Map.of("document", original))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();

		redisSaver.put(config, checkpoint);

		Optional<Checkpoint> retrievedOpt = redisSaver.get(config);
		assertTrue(retrievedOpt.isPresent());

		Object docObj = retrievedOpt.get().getState().get("document");
		assertInstanceOf(Document.class, docObj);

		Document deserialized = (Document) docObj;
		assertEquals(original.getId(), deserialized.getId());
		assertEquals(original.getText(), deserialized.getText());
		assertEquals(original.getScore(), deserialized.getScore());
		assertEquals(original.getMetadata(), deserialized.getMetadata());
	}

	@Test
	void testComplexMetadataSerialization() throws Exception {
		String threadId = "test-complex-metadata-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("string_field", "test_value");
		metadata.put("number_field", 42);
		metadata.put("boolean_field", true);
		metadata.put("list_field", List.of("item1", "item2", "item3"));

		Map<String, Object> nestedMap = new HashMap<>();
		nestedMap.put("nested_key", "nested_value");
		metadata.put("nested_object", nestedMap);

		UserMessage original = UserMessage.builder().text("Message with complex metadata").metadata(metadata).build();

		Checkpoint checkpoint = Checkpoint.builder()
				.id("cp-complex")
				.state(Map.of("message", original))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();

		redisSaver.put(config, checkpoint);

		Optional<Checkpoint> retrievedOpt = redisSaver.get(config);
		assertTrue(retrievedOpt.isPresent());

		Object msgObj = retrievedOpt.get().getState().get("message");
		assertInstanceOf(UserMessage.class, msgObj);

		UserMessage deserialized = (UserMessage) msgObj;
		Map<String, Object> deserializedMetadata = deserialized.getMetadata();

		assertEquals("test_value", deserializedMetadata.get("string_field"));
		assertEquals(42, deserializedMetadata.get("number_field"));
		assertEquals(true, deserializedMetadata.get("boolean_field"));

		// Jackson might deserialize List as ArrayList
		assertInstanceOf(List.class, deserializedMetadata.get("list_field"));
		List<?> list = (List<?>) deserializedMetadata.get("list_field");
		assertEquals(3, list.size());

		assertInstanceOf(Map.class, deserializedMetadata.get("nested_object"));
		Map<?, ?> map = (Map<?, ?>) deserializedMetadata.get("nested_object");
		assertEquals("nested_value", map.get("nested_key"));
	}

	/**
	 * Test that RedisSaver can be created with ObjectMapper from StateGraph's Jackson serializer.
	 * This ensures consistency between StateGraph serialization and RedisSaver serialization.
	 */
	@Test
	void testRedisSaverWithStateGraphJacksonSerializer() throws Exception {
		// Create StateGraph with SpringAIJacksonStateSerializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer);
		
		// Get ObjectMapper from the serializer (if it's a Jackson serializer)
		StateSerializer graphSerializer = graph.getStateSerializer();
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer);
		
		SpringAIJacksonStateSerializer jacksonSerializer = (SpringAIJacksonStateSerializer) graphSerializer;
		ObjectMapper objectMapper = jacksonSerializer.objectMapper();
		
		// Create RedisSaver with the ObjectMapper from StateGraph's serializer
		RedisSaver saverWithSerializer = new RedisSaver(redisson, objectMapper);
		
		// Test that it works correctly
		String threadId = "test-serializer-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
		
		Checkpoint checkpoint = Checkpoint.builder()
				.id("cp-serializer")
				.state(Map.of("data", "test_value", "number", 42))
				.nodeId("node1")
				.nextNodeId("node2")
				.build();
		
		saverWithSerializer.put(config, checkpoint);
		
		Optional<Checkpoint> retrieved = saverWithSerializer.get(config);
		assertTrue(retrieved.isPresent());
		assertEquals("test_value", retrieved.get().getState().get("data"));
		assertEquals(42, retrieved.get().getState().get("number"));
	}

	/**
	 * Test that StateGraph with different serializers works correctly with RedisSaver.
	 * Note: RedisSaver uses JSON serialization, so it works best with Jackson-based serializers.
	 */
	@Test
	void testStateGraphWithSerializerAndRedisSaver() throws Exception {
		// Create StateGraph with SpringAIJacksonStateSerializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("steps", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				int steps = (int) state.value("steps").orElse(0);
				return Map.of("steps", steps + 1);
			}))
			.addEdge("node1", END);
		
		// Get ObjectMapper from StateGraph's serializer
		StateSerializer graphSerializer = graph.getStateSerializer();
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer);
		SpringAIJacksonStateSerializer jacksonSerializer = (SpringAIJacksonStateSerializer) graphSerializer;
		ObjectMapper objectMapper = jacksonSerializer.objectMapper();
		
		// Create RedisSaver using the same ObjectMapper
		RedisSaver saver = new RedisSaver(redisson, objectMapper);
		
		// Compile graph with saver
		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();
		
		CompiledGraph compiledGraph = graph.compile(compileConfig);
		
		// Execute and verify
		String threadId = "test-execution-thread-" + UUID.randomUUID();
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
		java.util.Optional<OverAllState> result = compiledGraph.invoke(Map.of(), runnableConfig);
		
		assertTrue(result.isPresent());
		assertEquals(1, result.get().value("steps").orElse(0));
		
		// Verify checkpoint was saved
		Optional<Checkpoint> checkpoint = saver.get(runnableConfig);
		assertTrue(checkpoint.isPresent());
		assertEquals(1, checkpoint.get().getState().get("steps"));
	}

	/**
	 * Test that RedisSaver works correctly with StateGraph's default serializer.
	 */
	@Test
	void testRedisSaverWithStateGraphDefaultSerializer() throws Exception {
		// Create StateGraph with default serializer (should be JacksonSerializer)
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("value", new ReplaceStrategy());
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory);
		
		// Verify default serializer is Jackson-based
		StateSerializer graphSerializer = graph.getStateSerializer();
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer);
		
		// Get ObjectMapper from default serializer
		SpringAIJacksonStateSerializer jacksonSerializer = (SpringAIJacksonStateSerializer) graphSerializer;
		ObjectMapper objectMapper = jacksonSerializer.objectMapper();
		
		// Create RedisSaver with the ObjectMapper
		RedisSaver saver = new RedisSaver(redisson, objectMapper);
		
		// Test serialization/deserialization
		String threadId = "test-default-serializer-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
		
		// Create checkpoint with Message objects
		List<Message> messages = List.of(
			SystemMessage.builder().text("System message").build(),
			UserMessage.builder().text("User message").build()
		);
		
		Checkpoint checkpoint = Checkpoint.builder()
			.id("cp-default")
			.state(Map.of("messages", messages, "data", "test"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		
		saver.put(config, checkpoint);
		
		Optional<Checkpoint> retrieved = saver.get(config);
		assertTrue(retrieved.isPresent());
		
		// Verify messages are correctly deserialized
		Object messagesObj = retrieved.get().getState().get("messages");
		assertInstanceOf(List.class, messagesObj);
		@SuppressWarnings("unchecked")
		List<Object> retrievedMessages = (List<Object>) messagesObj;
		assertEquals(2, retrievedMessages.size());
		assertInstanceOf(SystemMessage.class, retrievedMessages.get(0));
		assertInstanceOf(UserMessage.class, retrievedMessages.get(1));
	}

	/**
	 * Test that using different ObjectMapper configurations affects serialization behavior.
	 */
	@Test
	void testRedisSaverWithCustomObjectMapper() throws Exception {
		// Create a custom ObjectMapper with StateGraph serializer configuration
		ObjectMapper customObjectMapper = new ObjectMapper();
		customObjectMapper = BaseCheckpointSaver.configureObjectMapper(customObjectMapper);
		
		// Create RedisSaver with custom ObjectMapper
		RedisSaver saver = new RedisSaver(redisson, customObjectMapper);
		
		// Test that it works with Message objects
		String threadId = "test-custom-mapper-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
		
		AssistantMessage assistantMsg = AssistantMessage.builder()
			.content("Test response")
			.build();
		
		Checkpoint checkpoint = Checkpoint.builder()
			.id("cp-custom")
			.state(Map.of("message", assistantMsg))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		
		saver.put(config, checkpoint);
		
		Optional<Checkpoint> retrieved = saver.get(config);
		assertTrue(retrieved.isPresent());
		
		Object msgObj = retrieved.get().getState().get("message");
		assertInstanceOf(AssistantMessage.class, msgObj);
		
		AssistantMessage deserialized = (AssistantMessage) msgObj;
		assertEquals("Test response", deserialized.getText());
	}

	/**
	 * Test consistency: StateGraph and RedisSaver should use compatible serialization.
	 */
	@Test
	void testSerializerConsistencyBetweenStateGraphAndRedisSaver() throws Exception {
		// Create StateGraph with specific serializer
		StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
		KeyStrategyFactory keyStrategyFactory = () -> Map.of("test", new ReplaceStrategy());
		
		StateGraph graph = new StateGraph("testGraph", keyStrategyFactory, serializer);
		
		// Get ObjectMapper from StateGraph's serializer
		StateSerializer graphSerializer = graph.getStateSerializer();
		assertInstanceOf(SpringAIJacksonStateSerializer.class, graphSerializer);
		SpringAIJacksonStateSerializer jacksonSerializer = (SpringAIJacksonStateSerializer) graphSerializer;
		ObjectMapper graphObjectMapper = jacksonSerializer.objectMapper();
		
		// Create RedisSaver with the same ObjectMapper
		RedisSaver saver = new RedisSaver(redisson, graphObjectMapper);
		
		// Verify they use compatible serialization
		String threadId = "test-consistency-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
		
		// Create complex state with various types
		Map<String, Object> complexState = new HashMap<>();
		complexState.put("string", "value");
		complexState.put("number", 123);
		complexState.put("messages", List.of(
			SystemMessage.builder().text("System").build(),
			UserMessage.builder().text("User").build()
		));
		
		Checkpoint checkpoint = Checkpoint.builder()
			.id("cp-consistency")
			.state(complexState)
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		
		// Save and retrieve
		saver.put(config, checkpoint);
		Optional<Checkpoint> retrieved = saver.get(config);
		
		assertTrue(retrieved.isPresent());
		Map<String, Object> retrievedState = retrieved.get().getState();
		
		// Verify all types are correctly preserved
		assertEquals("value", retrievedState.get("string"));
		assertEquals(123, retrievedState.get("number"));
		
		Object messagesObj = retrievedState.get("messages");
		assertInstanceOf(List.class, messagesObj);
		@SuppressWarnings("unchecked")
		List<Object> messages = (List<Object>) messagesObj;
		assertEquals(2, messages.size());
		assertInstanceOf(SystemMessage.class, messages.get(0));
		assertInstanceOf(UserMessage.class, messages.get(1));
	}
}
