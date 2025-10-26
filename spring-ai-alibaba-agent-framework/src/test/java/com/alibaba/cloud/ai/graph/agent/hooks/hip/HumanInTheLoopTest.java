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
package com.alibaba.cloud.ai.graph.agent.hooks.hip;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.alibaba.cloud.ai.graph.agent.tools.PoetTool.createPoetToolCallback;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class HumanInTheLoopTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testRejected() throws Exception {
		ReactAgent agent = createAgent();

		printGraphRepresentation(agent);

		String threadId = "test-thread-123";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig);

		InterruptionMetadata feedbackMetadata = buildRejectionFeedback(interruptionMetadata);

		performSecondInvocation(agent, threadId, feedbackMetadata);

	}

	@Test
	public void testApproved() throws Exception {
		ReactAgent agent = createAgent();

		printGraphRepresentation(agent);

		String threadId = "test-thread-approved";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig);

		InterruptionMetadata feedbackMetadata = buildApprovalFeedback(interruptionMetadata);

		performSecondInvocation(agent, threadId, feedbackMetadata);

	}

	@Test
	public void testEdited() throws Exception {
		ReactAgent agent = createAgent();

		printGraphRepresentation(agent);

		String threadId = "test-thread-edited";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig);

		InterruptionMetadata feedbackMetadata = buildEditedFeedback(interruptionMetadata);

		performSecondInvocation(agent, threadId, feedbackMetadata);

	}

	private ReactAgent createAgent() throws GraphStateException {
		return ReactAgent.builder()
				.name("single_agent")
				.model(chatModel)
				.compileConfig(getCompileConfig())
				.tools(List.of(createPoetToolCallback()))
				.hooks(List.of(HumanInTheLoopHook.builder().approvalOn("poem", ToolConfig.builder().description("Please confirm tool execution.").build()).build()))
				.outputKey("article")
				.build();
	}

	private void printGraphRepresentation(ReactAgent agent) {
		GraphRepresentation representation = agent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());
	}

	private InterruptionMetadata performFirstInvocation(ReactAgent agent, RunnableConfig runnableConfig) throws Exception {
		// First invocation - should trigger interruption for human approval
		System.out.println("\n=== First Invocation: Expecting Interruption ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput("帮我写一篇100字左右散文。", runnableConfig);

		// Assert first invocation results in interruption
		Assertions.assertTrue(result.isPresent(), "First invocation should return a result");
		Assertions.assertInstanceOf(InterruptionMetadata.class, result.get(),
			"First invocation should return InterruptionMetadata for human approval");

		InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

		// Assert interruption metadata contains expected information
		Assertions.assertNotNull(interruptionMetadata.node(), "Interruption should have node id");
		Assertions.assertNotNull(interruptionMetadata.state(), "Interruption should have state");

		// Assert state contains expected data
		Assertions.assertNotNull(interruptionMetadata.state().data(),
			"Interruption state should have data");
		Assertions.assertFalse(interruptionMetadata.state().data().isEmpty(),
			"Interruption state data should not be empty");

		// Assert tool feedbacks are present
		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
		Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
		Assertions.assertFalse(toolFeedbacks.isEmpty(), "Tool feedbacks should not be empty");
		Assertions.assertEquals(1, toolFeedbacks.size(),
			"Should have exactly one tool feedback for the 'poem' tool");

		// Assert tool feedback details
		InterruptionMetadata.ToolFeedback firstFeedback = toolFeedbacks.get(0);
		Assertions.assertNotNull(firstFeedback.getId(), "Tool feedback should have an id");
		Assertions.assertFalse(firstFeedback.getId().isEmpty(), "Tool feedback id should not be empty");
		Assertions.assertEquals("poem", firstFeedback.getName(), "Tool name should be 'poem'");
		Assertions.assertNotNull(firstFeedback.getArguments(), "Tool feedback should have arguments");
		Assertions.assertNotNull(firstFeedback.getDescription(), "Tool feedback should have description");

		return interruptionMetadata;
	}

	private InterruptionMetadata buildRejectionFeedback(InterruptionMetadata interruptionMetadata) {
		// Build new metadata with REJECTED feedback
		InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder()
			.nodeId(interruptionMetadata.node())
			.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			InterruptionMetadata.ToolFeedback rejectedFeedback = InterruptionMetadata.ToolFeedback
				.builder(toolFeedback)
				.result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
				.description("不用使用这个工具，你自己完成写作。")
				.build();
			newBuilder.addToolFeedback(rejectedFeedback);
		});

		return newBuilder.build();
	}

	private InterruptionMetadata buildApprovalFeedback(InterruptionMetadata interruptionMetadata) {
		// Build new metadata with APPROVED feedback
		InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder()
			.nodeId(interruptionMetadata.node())
			.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			InterruptionMetadata.ToolFeedback approvedFeedback = InterruptionMetadata.ToolFeedback
				.builder(toolFeedback)
				.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
				.build();
			newBuilder.addToolFeedback(approvedFeedback);
		});

		return newBuilder.build();
	}

	private InterruptionMetadata buildEditedFeedback(InterruptionMetadata interruptionMetadata) {
		// Build new metadata with EDITED feedback
		InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder()
			.nodeId(interruptionMetadata.node())
			.state(interruptionMetadata.state());

		interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
			InterruptionMetadata.ToolFeedback editedFeedback = InterruptionMetadata.ToolFeedback
				.builder(toolFeedback)
				.arguments(toolFeedback.getArguments().replace("。\"", "。By Spring AI Alibaba\""))
				.result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
				.build();
			newBuilder.addToolFeedback(editedFeedback);
		});

		return newBuilder.build();
	}

	private void performSecondInvocation(ReactAgent agent, String threadId, InterruptionMetadata feedbackMetadata) throws Exception {
		// Resume execution with human feedback
		System.out.println("\n=== Second Invocation: Resuming with REJECTED Feedback ===");
		RunnableConfig resumeRunnableConfig = RunnableConfig.builder().threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedbackMetadata)
				.build();

		try {
			// Second invocation - should resume and complete
			Optional<NodeOutput> result = agent.invokeAndGetOutput("", resumeRunnableConfig);

			// Assert second invocation completes successfully
			Assertions.assertTrue(result.isPresent(), "Second invocation should return a result");
			NodeOutput finalOutput = result.get();
			Assertions.assertNotNull(finalOutput, "Final result should not be null");

			// Assert the result is NOT another interruption (execution should complete)
			Assertions.assertNotEquals(InterruptionMetadata.class, finalOutput.getClass(),
				"Final result should not be an InterruptionMetadata - execution should complete");

			System.out.println("Final result type: " + finalOutput.getClass().getSimpleName());
			System.out.println("Final result node: " + finalOutput.node());
			System.out.println("Final result state data keys: " + finalOutput.state().data().keySet());

			// Assert final state contains expected data
			Assertions.assertNotNull(finalOutput.state(), "Final output should have state");
			Assertions.assertNotNull(finalOutput.state().data(), "Final output state should have data");
			Assertions.assertFalse(finalOutput.state().data().isEmpty(),
				"Final output state data should not be empty");

		} catch (java.util.concurrent.CompletionException e) {
			System.err.println("ReactAgent execution failed: " + e.getMessage());
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
	}

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
				.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
				.build();
		return CompileConfig.builder().saverConfig(saverConfig).build();
	}
}
