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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for bug #3190: SubGraphNodeAdapter incorrectly overwrites child graph output
 * with parentMessages when includeContents = false.
 * 
 * This test verifies that:
 * 1. When includeContents = false and outputKeyToParent is null (default "messages"),
 *    the child graph output is NOT overwritten by parentMessages.
 * 2. When includeContents = false and outputKeyToParent is a custom key,
 *    parentMessages are correctly preserved.
 */
class SubGraphNodeAdapterIsolationTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        this.chatModel = new MockChatModel();
    }

    /**
     * Mock ChatModel for testing without external API dependency.
     */
    static class MockChatModel implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Mock response"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock stream response")))));
        }
    }

	/**
	 * Test case for bug #3190: When includeContents = false and outputKeyToParent is null,
	 * the child graph output should NOT be overwritten by parentMessages.
	 * 
	 * This is the main bug scenario where child graph output was being lost.
	 */
	@Test
	void testIncludeContentsFalse_DefaultOutputKey_ChildOutputNotOverwritten() throws Exception {
		// Create a child agent with includeContents = false and no custom outputKey
		ReactAgent childAgent = ReactAgent.builder()
				.name("child_agent")
				.model(chatModel)
				.description("Child agent that processes input independently")
				.includeContents(false) // Key: isolate child from parent messages
				.instruction("请简单回复：这是子图的输出")
				// outputKeyToParent is null, so it defaults to "messages"
				.build();

		// Create a parent agent that uses the child agent
		ReactAgent parentAgent = ReactAgent.builder()
				.name("parent_agent")
				.model(chatModel)
				.description("Parent agent that uses child agent")
				.instruction("请调用子图处理")
				.build();

		// Create a sequential agent to test the subgraph behavior
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("test_sequential")
				.description("Test sequential agent")
				.subAgents(List.of(parentAgent, childAgent))
				.compileConfig(CompileConfig.builder()
						.saverConfig(SaverConfig.builder()
								.register(new MemorySaver())
								.build())
						.build())
				.build();

		// Execute with initial messages
		Optional<OverAllState> result = sequentialAgent.invoke("测试消息");

		// Verify result is present
		assertTrue(result.isPresent(), "Result should be present");

		OverAllState state = result.get();

		// Verify that messages exist (child output should be present)
		assertTrue(state.value("messages").isPresent(), 
				"Messages should be present in state");

		@SuppressWarnings("unchecked")
		List<Object> messages = (List<Object>) state.value("messages").get();

		// Verify that we have messages (at least the child agent's output)
		assertFalse(messages.isEmpty(),
				"Messages list should not be empty - child graph output should be present");

		// Verify that there exists an AssistantMessage from child agent in messages
		boolean hasAssistant = messages.stream().anyMatch(m -> m instanceof AssistantMessage);
		assertTrue(hasAssistant,
				"Messages should contain AssistantMessage from child agent when includeContents=false and output key is 'messages'. " +
				"This verifies bug #3190 is fixed.");
	}

	/**
	 * Test case: When includeContents = false and outputKeyToParent is a custom key,
	 * parentMessages should be correctly preserved in the "messages" key.
	 */
	@Test
	void testIncludeContentsFalse_CustomOutputKey_ParentMessagesPreserved() throws Exception {
		// Create a child agent with includeContents = false and custom outputKey
		ReactAgent childAgent = ReactAgent.builder()
				.name("child_agent_custom")
				.model(chatModel)
				.description("Child agent with custom output key")
				.includeContents(false) // Isolate child from parent messages
				.instruction("请简单回复：这是子图的输出")
				.outputKey("child_output") // Custom output key
				.build();

		// Create a parent agent
		ReactAgent parentAgent = ReactAgent.builder()
				.name("parent_agent_custom")
				.model(chatModel)
				.description("Parent agent")
				.instruction("请调用子图处理")
				.build();

		// Create sequential agent
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("test_sequential_custom")
				.description("Test sequential agent with custom output key")
				.subAgents(List.of(parentAgent, childAgent))
				.compileConfig(CompileConfig.builder()
						.saverConfig(SaverConfig.builder()
								.register(new MemorySaver())
								.build())
						.build())
				.build();

		// Execute
		Optional<OverAllState> result = sequentialAgent.invoke("测试消息");

		// Verify result
		assertTrue(result.isPresent(), "Result should be present");

		OverAllState state = result.get();

		// Verify that child output is in custom key
		assertTrue(state.value("child_output").isPresent(),
				"Child output should be present in custom key 'child_output'");

		// Verify that parent messages are preserved in "messages" key
		assertTrue(state.value("messages").isPresent(),
				"Parent messages should be preserved in 'messages' key when " +
				"child output uses custom key");

		@SuppressWarnings("unchecked")
		List<Object> messages = (List<Object>) state.value("messages").get();

		// Verify messages are not empty (parent messages should be preserved)
		assertFalse(messages.isEmpty(),
				"Parent messages should be preserved when child uses custom output key");
	}

	/**
	 * Test case: When includeContents = true, parent messages should be passed to child
	 * (baseline test to ensure normal behavior still works).
	 */
	@Test
	void testIncludeContentsTrue_ParentMessagesPassedToChild() throws Exception {
		// Create a child agent with includeContents = true (default)
		ReactAgent childAgent = ReactAgent.builder()
				.name("child_agent_included")
				.model(chatModel)
				.description("Child agent with includeContents = true")
				.includeContents(true) // Include parent messages
				.instruction("请简单回复：这是子图的输出")
				.build();

		// Create a parent agent
		ReactAgent parentAgent = ReactAgent.builder()
				.name("parent_agent_included")
				.model(chatModel)
				.description("Parent agent")
				.instruction("请调用子图处理")
				.build();

		// Create sequential agent
		SequentialAgent sequentialAgent = SequentialAgent.builder()
				.name("test_sequential_included")
				.description("Test sequential agent with includeContents = true")
				.subAgents(List.of(parentAgent, childAgent))
				.compileConfig(CompileConfig.builder()
						.saverConfig(SaverConfig.builder()
								.register(new MemorySaver())
								.build())
						.build())
				.build();

		// Execute
		Optional<OverAllState> result = sequentialAgent.invoke("测试消息");

		// Verify result
		assertTrue(result.isPresent(), "Result should be present");

		OverAllState state = result.get();

		// Verify messages exist
		assertTrue(state.value("messages").isPresent(),
				"Messages should be present when includeContents = true");

		@SuppressWarnings("unchecked")
		List<Object> messages = (List<Object>) state.value("messages").get();

		// Verify we have messages (both parent and child)
		assertFalse(messages.isEmpty(),
				"Messages should contain both parent and child messages when includeContents = true");
	}
}
