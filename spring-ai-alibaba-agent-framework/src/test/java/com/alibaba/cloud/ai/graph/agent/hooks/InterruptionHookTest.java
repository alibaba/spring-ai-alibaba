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
package com.alibaba.cloud.ai.graph.agent.hooks;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.InterruptionHook;
import com.alibaba.cloud.ai.graph.agent.tools.PoemTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_HOOK_NAME_PREFIX;

public class InterruptionHookTest {

	private InterruptionHook hook;
	private OverAllState state;
	private ReactAgent mockAgent;

	/**
	 * Mock ChatModel for unit tests that don't require actual API calls.
	 */
	private static class MockChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("Mock response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(new Generation(new org.springframework.ai.chat.messages.AssistantMessage("Mock stream response")))));
		}
	}

	@BeforeEach
	void setUp() {
		hook = InterruptionHook.builder().build();
		state = new OverAllState(Map.of("messages", List.of(new UserMessage("Initial message"))));
		
		// Create a mock ReactAgent for testing
		mockAgent = ReactAgent.builder()
				.name("test-agent")
				.model(new MockChatModel())
				.saver(new MemorySaver())
				.build();
		
		// Set the agent to the hook
		hook.setAgent(mockAgent);
	}

	@Test
	public void testInterrupt_EmptyList_ShouldReturnInterruptionMetadata() {
		// Given: INTERRUPTION_FEEDBACK_KEY is set as empty list in agent thread state
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-empty-list";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set empty list in agent thread state
		mockAgent.updateAgentState(List.of(), config);

		// When: interrupt is called
		Optional<InterruptionMetadata> result = hook.interrupt("test-node", testState, config);

		// Then: should return InterruptionMetadata
		Assertions.assertTrue(result.isPresent(), "Should return InterruptionMetadata when INTERRUPTION_FEEDBACK_KEY is empty list");
		InterruptionMetadata metadata = result.get();
		Assertions.assertEquals("test-node", metadata.node(), "Node ID should match");
		Assertions.assertNotNull(metadata.state(), "State should not be null");
		Assertions.assertTrue(metadata.metadata("interruption_requested").isPresent(), 
				"Should have interruption_requested metadata");
		Assertions.assertEquals(true, metadata.metadata("interruption_requested").get(), 
				"interruption_requested should be true");
	}

	@Test
	public void testInterrupt_NonEmptyList_ShouldReturnEmpty() {
		// Given: INTERRUPTION_FEEDBACK_KEY is set as non-empty list in agent thread state
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-non-empty-list";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set non-empty list in agent thread state
		mockAgent.updateAgentState(List.of(new UserMessage("feedback")), config);

		// When: interrupt is called
		Optional<InterruptionMetadata> result = hook.interrupt("test-node", testState, config);

		// Then: should return empty (continue normal execution)
		Assertions.assertFalse(result.isPresent(), 
				"Should return empty when INTERRUPTION_FEEDBACK_KEY has non-empty list");
	}

	@Test
	public void testInterrupt_NoInterruptionFeedbackKey_ShouldReturnEmpty() {
		// Given: No INTERRUPTION_FEEDBACK_KEY in agent thread state
		String threadId = "test-thread-no-feedback";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// When: interrupt is called
		Optional<InterruptionMetadata> result = hook.interrupt("test-node", state, config);

		// Then: should return empty
		Assertions.assertFalse(result.isPresent(), 
				"Should return empty when INTERRUPTION_FEEDBACK_KEY is not set in state");
	}

	@Test
	public void testInterrupt_NonListValue_ShouldReturnEmpty() {
		// Given: INTERRUPTION_FEEDBACK_KEY is set as non-list value in agent thread state
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-non-list";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set non-list value in agent thread state
		mockAgent.updateAgentState("not a list", config);

		// When: interrupt is called
		Optional<InterruptionMetadata> result = hook.interrupt("test-node", testState, config);

		// Then: should return empty (continue normal execution)
		Assertions.assertFalse(result.isPresent(), 
				"Should return empty when INTERRUPTION_FEEDBACK_KEY is not a list");
	}

	@Test
	public void testApply_WithStringFeedback_ShouldAddUserMessage() throws Exception {
		// Given: INTERRUPTION_FEEDBACK_KEY is set with String value in agent thread state
		String feedbackText = "This is feedback";
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-string-feedback";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set feedback in agent thread state
		mockAgent.updateAgentState(feedbackText, config);

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(testState, config);
		Map<String, Object> result = future.get();

		// Then: should add UserMessage to messages list
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.containsKey("messages"), "Result should contain messages key");
		
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) result.get("messages");
		Assertions.assertNotNull(messages, "Messages list should not be null");
		Assertions.assertEquals(1, messages.size(), "Should have 1 message (feedback only, not appending to existing)");
		
		Message lastMessage = messages.get(messages.size() - 1);
		Assertions.assertInstanceOf(UserMessage.class, lastMessage, "Last message should be UserMessage");
		Assertions.assertEquals(feedbackText, lastMessage.getText(), "Message text should match feedback");
		
		// Verify INTERRUPTION_FEEDBACK_KEY has been removed from agent thread state
		Map<String, Object> threadState = mockAgent.getThreadState(threadId);
		Assertions.assertNull(threadState.get(InterruptionHook.INTERRUPTION_FEEDBACK_KEY), 
				"INTERRUPTION_FEEDBACK_KEY should be removed from agent thread state");
	}

	@Test
	public void testApply_WithUserMessageFeedback_ShouldAddUserMessage() throws Exception {
		// Given: INTERRUPTION_FEEDBACK_KEY is set with UserMessage value in agent thread state
		UserMessage feedbackMessage = new UserMessage("This is UserMessage feedback");
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-user-message-feedback";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set feedback in agent thread state
		mockAgent.updateAgentState(feedbackMessage, config);

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(testState, config);
		Map<String, Object> result = future.get();

		// Then: should add UserMessage to messages list
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.containsKey("messages"), "Result should contain messages key");
		
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) result.get("messages");
		Assertions.assertNotNull(messages, "Messages list should not be null");
		Assertions.assertEquals(1, messages.size(), "Should have 1 message (feedback only)");
		
		Message lastMessage = messages.get(messages.size() - 1);
		Assertions.assertInstanceOf(UserMessage.class, lastMessage, "Last message should be UserMessage");
		Assertions.assertEquals(feedbackMessage.getText(), lastMessage.getText(), 
				"Message text should match feedback");
		
		// Verify INTERRUPTION_FEEDBACK_KEY has been removed from agent thread state
		Map<String, Object> threadState = mockAgent.getThreadState(threadId);
		Assertions.assertNull(threadState.get(InterruptionHook.INTERRUPTION_FEEDBACK_KEY), 
				"INTERRUPTION_FEEDBACK_KEY should be removed from agent thread state");
	}

	@Test
	public void testApply_WithListMessageFeedback_ShouldAddAllMessages() throws Exception {
		// Given: INTERRUPTION_FEEDBACK_KEY is set with List<Message> value in agent thread state
		UserMessage feedbackMessage1 = new UserMessage("First feedback message");
		UserMessage feedbackMessage2 = new UserMessage("Second feedback message");
		List<Message> feedbackMessages = List.of(feedbackMessage1, feedbackMessage2);
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-list-message-feedback";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set feedback in agent thread state
		mockAgent.updateAgentState(feedbackMessages, config);

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(testState, config);
		Map<String, Object> result = future.get();

		// Then: should add all messages from the list
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.containsKey("messages"), "Result should contain messages key");
		
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) result.get("messages");
		Assertions.assertNotNull(messages, "Messages list should not be null");
		Assertions.assertEquals(2, messages.size(), "Should have 2 messages from feedback list");
		
		Assertions.assertEquals(feedbackMessage1.getText(), messages.get(0).getText(), 
				"First message should match first feedback");
		Assertions.assertEquals(feedbackMessage2.getText(), messages.get(1).getText(), 
				"Second message should match second feedback");
		
		// Verify INTERRUPTION_FEEDBACK_KEY has been removed from agent thread state
		Map<String, Object> threadState = mockAgent.getThreadState(threadId);
		Assertions.assertNull(threadState.get(InterruptionHook.INTERRUPTION_FEEDBACK_KEY), 
				"INTERRUPTION_FEEDBACK_KEY should be removed from agent thread state");
	}

	@Test
	public void testApply_NoFeedback_ShouldReturnEmpty() throws Exception {
		// Given: No INTERRUPTION_FEEDBACK_KEY is set in agent thread state
		String threadId = "test-thread-no-feedback-apply";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(state, config);
		Map<String, Object> result = future.get();

		// Then: should return empty map
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.isEmpty(), "Result should be empty when no feedback is provided");
	}

	@Test
	public void testApply_InvalidFeedbackType_ShouldReturnEmpty() throws Exception {
		// Given: INTERRUPTION_FEEDBACK_KEY is set with invalid type (Integer) in agent thread state
		OverAllState testState = new OverAllState(Map.of(
				"messages", List.of(new UserMessage("Initial message"))
		));
		String threadId = "test-thread-invalid-feedback-type";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set invalid feedback type in agent thread state
		mockAgent.updateAgentState(123, config);

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(testState, config);
		Map<String, Object> result = future.get();

		// Then: should return empty map (invalid type is ignored and removed)
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.isEmpty(), "Result should be empty when feedback type is invalid");
		
		// Verify INTERRUPTION_FEEDBACK_KEY has been removed from agent thread state
		Map<String, Object> threadState = mockAgent.getThreadState(threadId);
		Assertions.assertNull(threadState.get(InterruptionHook.INTERRUPTION_FEEDBACK_KEY), 
				"INTERRUPTION_FEEDBACK_KEY should be removed from agent thread state even for invalid type");
	}

	@Test
	public void testApply_EmptyMessagesList_ShouldAddMessage() throws Exception {
		// Given: State with empty messages list and INTERRUPTION_FEEDBACK_KEY in agent thread state
		String feedbackText = "Feedback for empty list";
		OverAllState emptyState = new OverAllState(Map.of(
				"messages", List.of()
		));
		String threadId = "test-thread-empty-messages-list";
		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
		
		// Set feedback in agent thread state
		mockAgent.updateAgentState(feedbackText, config);

		// When: apply is called
		CompletableFuture<Map<String, Object>> future = hook.apply(emptyState, config);
		Map<String, Object> result = future.get();

		// Then: should add UserMessage to messages list
		Assertions.assertNotNull(result, "Result should not be null");
		Assertions.assertTrue(result.containsKey("messages"), "Result should contain messages key");
		
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) result.get("messages");
		Assertions.assertNotNull(messages, "Messages list should not be null");
		Assertions.assertEquals(1, messages.size(), "Should have 1 message (feedback)");
		
		Message lastMessage = messages.get(0);
		Assertions.assertInstanceOf(UserMessage.class, lastMessage, "Message should be UserMessage");
		Assertions.assertEquals(feedbackText, lastMessage.getText(), "Message text should match feedback");
		
		// Verify INTERRUPTION_FEEDBACK_KEY has been removed from agent thread state
		Map<String, Object> threadState = mockAgent.getThreadState(threadId);
		Assertions.assertNull(threadState.get(InterruptionHook.INTERRUPTION_FEEDBACK_KEY), 
				"INTERRUPTION_FEEDBACK_KEY should be removed from agent thread state");
	}

	@Test
	public void testGetName() {
		// When: getName is called
		String name = Hook.getFullHookName(hook);

		// Then: should return correct name
		Assertions.assertEquals(AGENT_HOOK_NAME_PREFIX + InterruptionHook.INTERRUPTION_NODE_NAME, name,
				"Name should match INTERRUPTION_NODE_NAME constant");
	}

	@Test
	public void testCanJumpTo() {
		// When: canJumpTo is called
		List<?> jumpTo = hook.canJumpTo();

		// Then: should return empty list
		Assertions.assertNotNull(jumpTo, "canJumpTo should not return null");
		Assertions.assertTrue(jumpTo.isEmpty(), "canJumpTo should return empty list");
	}

	/**
	 * Integration test with ReactAgent for long-running task interruption.
	 * Tests interrupting a long-running multi-round reasoning task using agent.interrupt() in a separate thread.
	 * Scenario 1: interrupt() without parameters (empty list)
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	public void testInterruptLongRunningTask_WithoutParameters() throws Exception {
		// Setup: Create ChatModel
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Setup: Create ReactAgent with InterruptionHook and tools for multi-round reasoning
		ReactAgent agent = ReactAgent.builder()
				.name("long_running_interruption_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.hooks(InterruptionHook.builder().build())
				.methodTools(new PoemTool())
				.instruction("You are a helpful assistant. When asked to write, use the poem tool to create content.")
				.build();

		String threadId = "test-thread-long-running-no-params";
		String userQuery = "请帮我写一篇100字左右的现代诗，然后总结一下主要内容";

		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// Track interruption and stream outputs
		AtomicBoolean interruptionDetected = new AtomicBoolean(false);
		AtomicReference<NodeOutput> lastOutput = new AtomicReference<>();
		CountDownLatch streamStarted = new CountDownLatch(1);
		CountDownLatch interruptionComplete = new CountDownLatch(1);

		// Start long-running task in a separate thread
		Thread streamThread = new Thread(() -> {
			try {
				Flux<NodeOutput> stream = agent.stream(userQuery, config);
				streamStarted.countDown();
				
				stream.doOnNext(output -> {
					lastOutput.set(output);
					System.out.println("Stream output: " + output.node() + ", type: " + output.getClass().getSimpleName());
					
					if (output instanceof InterruptionMetadata) {
						interruptionDetected.set(true);
						interruptionComplete.countDown();
					}
				}).blockLast();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		streamThread.start();

		// Wait for stream to start
		Assertions.assertTrue(streamStarted.await(5, TimeUnit.SECONDS), "Stream should start within 5 seconds");

		// Wait a bit for agent to start processing
		Thread.sleep(2000);

		// Interrupt from separate thread without parameters
		Thread interruptThread = new Thread(() -> {
			try {
				System.out.println("Calling agent.interrupt() without parameters...");
				agent.interrupt(config);
				System.out.println("Interrupt call completed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		interruptThread.start();
		interruptThread.join();

		// Wait for interruption to be detected
		Assertions.assertTrue(interruptionComplete.await(30, TimeUnit.SECONDS), 
				"Interruption should be detected within 30 seconds");

		// Verify interruption occurred
		Assertions.assertTrue(interruptionDetected.get(), "Interruption should be detected");
		Assertions.assertNotNull(lastOutput.get(), "Last output should not be null");
		Assertions.assertInstanceOf(InterruptionMetadata.class, lastOutput.get(),
				"Last output should be InterruptionMetadata");

		streamThread.join(5000);
	}

	/**
	 * Integration test with ReactAgent for long-running task interruption.
	 * Tests interrupting a long-running multi-round reasoning task using agent.interrupt() in a separate thread.
	 * Scenario 2: interrupt() with parameters (feedback message)
	 * Note: When interrupt() is called with parameters, it sets INTERRUPTION_FEEDBACK_KEY to a non-empty list,
	 * which means interrupt() method will return empty (no interruption), but apply() will process the feedback.
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	public void testInterruptLongRunningTask_WithParameters() throws Exception {
		// Setup: Create ChatModel
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();
		ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

		// Setup: Create ReactAgent with InterruptionHook and tools for multi-round reasoning
		ReactAgent agent = ReactAgent.builder()
				.name("long_running_interruption_agent_with_params")
				.model(chatModel)
				.saver(new MemorySaver())
				.hooks(InterruptionHook.builder().build())
				.methodTools(new PoemTool())
				.instruction("You are a helpful assistant. When asked to write, use the poem tool to create content.")
				.build();

		String threadId = "test-thread-long-running-with-params";
		String userQuery = "请帮我写一篇100字左右的现代诗，然后总结一下主要内容";
		String feedbackMessage = "请修改为200字左右的散文";

		RunnableConfig config = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// Track stream outputs and feedback processing
		AtomicBoolean feedbackProcessed = new AtomicBoolean(false);
		AtomicReference<NodeOutput> lastOutput = new AtomicReference<>();
		List<NodeOutput> allOutputs = new ArrayList<>();
		CountDownLatch streamStarted = new CountDownLatch(1);
		CountDownLatch feedbackProcessedLatch = new CountDownLatch(1);

		// Start long-running task in a separate thread
		Thread streamThread = new Thread(() -> {
			try {
				Flux<NodeOutput> stream = agent.stream(userQuery, config);
				streamStarted.countDown();
				
				stream.doOnNext(output -> {
					lastOutput.set(output);
					allOutputs.add(output);
					System.out.println("Stream output: " + output.node() + ", type: " + output.getClass().getSimpleName());
					
					// Check if feedback message was processed (should appear in messages)
					if (output.state() != null) {
						@SuppressWarnings("unchecked")
						List<Message> messages = (List<Message>) output.state().value("messages").orElse(List.of());
						boolean hasFeedback = messages.stream()
								.filter(msg -> msg instanceof UserMessage)
								.anyMatch(msg -> msg.getText().contains(feedbackMessage));
						if (hasFeedback) {
							feedbackProcessed.set(true);
							feedbackProcessedLatch.countDown();
						}
					}
				}).blockLast();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		streamThread.start();

		// Wait for stream to start
		Assertions.assertTrue(streamStarted.await(5, TimeUnit.SECONDS), "Stream should start within 5 seconds");

		// Wait a bit for agent to start processing (allow some rounds of reasoning)
		Thread.sleep(3000);

		// Interrupt from separate thread with feedback message
		Thread interruptThread = new Thread(() -> {
			try {
				System.out.println("Calling agent.interrupt() with feedback message: " + feedbackMessage);
				agent.interrupt(feedbackMessage, config);
				System.out.println("Interrupt call completed - feedback should be processed in next iteration");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		interruptThread.start();
		interruptThread.join();

		// Wait for feedback to be processed (should happen in apply() method)
		Assertions.assertTrue(feedbackProcessedLatch.await(30, TimeUnit.SECONDS), 
				"Feedback should be processed within 30 seconds");

		// Verify feedback was processed
		Assertions.assertTrue(feedbackProcessed.get(), "Feedback message should be processed and added to messages");
		Assertions.assertFalse(allOutputs.isEmpty(), "Should have received some stream outputs");

		streamThread.join(10000);
	}
}

