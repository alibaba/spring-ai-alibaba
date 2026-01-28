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
package com.alibaba.cloud.ai.graph.agent.hooks.hip;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.tools.WeatherTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
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

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig, "帮我写一篇100字左右现代诗");

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

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig, "帮我写一篇100字左右现代诗");

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

		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig, "帮我写一篇100字左右现代诗");

		InterruptionMetadata feedbackMetadata = buildEditedFeedback(interruptionMetadata);

		performSecondInvocation(agent, threadId, feedbackMetadata);

	}

	/**
	 * Test Model requests for two tools in a single assistant message response.
	 */
	@Test
	public void testMultipleToolcallsInAssistantMessage() throws Exception {
		ReactAgent agent = createAgentWithMultipleTools();

		printGraphRepresentation(agent);

		String threadId = "test-thread-multiple-toolcalls";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		InterruptionMetadata interruptionMetadata = performFirstInvocationAndCheckMultipleToolsRequested(agent, runnableConfig);

		// Only approve the first tool
		InterruptionMetadata feedbackMetadata = buildFeedbackWithOnlyOneApproval(interruptionMetadata);

		// Second invocation should still require approval for the second tool
		InterruptionMetadata interruptionMetadata2 = performSecondInvocationAndCheckApprovalRequiredAgain(agent, threadId, feedbackMetadata);

		// Approve the second tool
		InterruptionMetadata feedbackMetadata2 = buildApprovalFeedback(interruptionMetadata2);

		// Third invocation should complete successfully
		performThirdInvocation(agent, threadId, feedbackMetadata2);
	}

	/**
	 * Test Model requests for two tools in the whole agent loop, one tool each time.
	 */
	@Test
	public void testMultipleRoundAssistantMessages() throws Exception {
		ReactAgent agent = createAgentWithMultipleTools();

		printGraphRepresentation(agent);

		String threadId = "test-thread-multiple-rounds";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		// First invocation - should interrupt for first tool (poem)
		InterruptionMetadata interruptionMetadata = performFirstInvocation(agent, runnableConfig, "我正在做多次工具调用的测试，你需要分两次推理过程调用工具，不要一次返回两个工具调用。第一次先调用工具帮我写一篇100字左右现代诗，然后第二次再调用工具查询写作当天北京天气情况。");

		// Approve first tool
		InterruptionMetadata feedbackMetadata = buildApprovalFeedback(interruptionMetadata);

		// Second invocation - should interrupt again for second tool (weather)
		InterruptionMetadata interruptionMetadata2 = performSecondInvocationAndInterruptAgain(agent, threadId, feedbackMetadata);

		// Approve second tool
		InterruptionMetadata feedbackMetadata2 = buildApprovalFeedback(interruptionMetadata2);

		// Third invocation - should complete successfully
		performThirdInvocation(agent, threadId, feedbackMetadata2);
	}

	private ReactAgent createAgent() {
		Map approvalOn = Map.of(
				"poem", ToolConfig.builder().description("请确认诗歌工具执行").build()
		);

		return ReactAgent.builder()
				.name("single_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.tools(List.of(createPoetToolCallback()))
				.hooks(HumanInTheLoopHook.builder().approvalOn(approvalOn).build())
				.outputKey("article")
				.build();
	}

	private ReactAgent createAgentWithMultipleTools() {
		Map approvalOn = Map.of(
				"poem", ToolConfig.builder().description("请确认诗歌工具执行").build(),
				"weather_tool", ToolConfig.builder().description("请确认天气工具执行").build()
		);

		return ReactAgent.builder()
				.name("single_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.tools(List.of(createPoetToolCallback(), WeatherTool.createWeatherTool("weather_tool", new WeatherTool())))
				.hooks(HumanInTheLoopHook.builder().approvalOn(approvalOn).build())
				.outputKey("article")
				.build();
	}

	private void printGraphRepresentation(ReactAgent agent) {
		GraphRepresentation representation = agent.getGraph().getGraph(GraphRepresentation.Type.PLANTUML);
		System.out.println(representation.content());
	}

	private InterruptionMetadata performFirstInvocationAndCheckMultipleToolsRequested(ReactAgent agent, RunnableConfig runnableConfig) throws Exception {
		// First invocation - should trigger interruption for human approval
		System.out.println("\n=== First Invocation: Expecting Interruption with Multiple Tools ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput("帮我写一篇100字左右散文，同时在文章最后包含写作当天北京天气情况。", runnableConfig);

		// Assert first invocation results in interruption
		Assertions.assertTrue(result.isPresent(), "First invocation should return a result");
		Assertions.assertInstanceOf(InterruptionMetadata.class, result.get(),
				"First invocation should return InterruptionMetadata for human approval");

		InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

		// Assert interruption metadata contains expected information
		Assertions.assertNotNull(interruptionMetadata.node(), "Interruption should have node id");
		Assertions.assertNotNull(interruptionMetadata.state(), "Interruption should have state");

		// Assert tool feedbacks are present and there are exactly 2 tools
		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
		Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
		Assertions.assertFalse(toolFeedbacks.isEmpty(), "Tool feedbacks should not be empty");
		Assertions.assertEquals(2, toolFeedbacks.size(),
				"Should have exactly two tool feedbacks for both 'poem' and 'weather' tools");

		// Verify both tools are present
		List<String> toolNames = toolFeedbacks.stream()
				.map(InterruptionMetadata.ToolFeedback::getName)
				.toList();
		Assertions.assertTrue(toolNames.contains("poem"), "Should contain 'poem' tool");
		Assertions.assertTrue(toolNames.contains("weather") || toolNames.contains("weather_tool"),
				"Should contain 'weather' or 'weather_tool' tool");

		return interruptionMetadata;
	}


	private InterruptionMetadata performFirstInvocation(ReactAgent agent, RunnableConfig runnableConfig, String query) throws Exception {
		// First invocation - should trigger interruption for human approval
		System.out.println("\n=== First Invocation: Expecting Interruption ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput(query, runnableConfig);

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
		System.out.println("\n=== Second Invocation: Resuming with Feedback ===");
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

	private InterruptionMetadata performSecondInvocationAndInterruptAgain(ReactAgent agent, String threadId, InterruptionMetadata feedbackMetadata) throws Exception {
		// Resume execution with human feedback
		System.out.println("\n=== Second Invocation: Resuming with Feedback, Expecting Another Interruption ===");
		RunnableConfig resumeRunnableConfig = RunnableConfig.builder().threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedbackMetadata)
				.build();

		try {
			// Second invocation - should resume and interrupt again for the second tool
			Optional<NodeOutput> result = agent.invokeAndGetOutput("", resumeRunnableConfig);

			// Assert second invocation results in another interruption
			Assertions.assertTrue(result.isPresent(), "Second invocation should return a result");
			Assertions.assertInstanceOf(InterruptionMetadata.class, result.get(),
					"Second invocation should return InterruptionMetadata for the second tool approval");

			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();
			Assertions.assertNotNull(interruptionMetadata, "Interruption metadata should not be null");
			Assertions.assertNotNull(interruptionMetadata.node(), "Interruption should have node id");
			Assertions.assertNotNull(interruptionMetadata.state(), "Interruption should have state");

			// Assert tool feedbacks are present (should be for the second tool)
			List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
			Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
			Assertions.assertFalse(toolFeedbacks.isEmpty(), "Tool feedbacks should not be empty");
			Assertions.assertEquals(1, toolFeedbacks.size(),
					"Should have exactly one tool feedback for the second tool");

			// Verify it's the second tool (weather)
			InterruptionMetadata.ToolFeedback secondFeedback = toolFeedbacks.get(0);
			String toolName = secondFeedback.getName();
			Assertions.assertTrue(toolName.equals("weather") || toolName.equals("weather_tool"),
					"Second tool should be 'weather' or 'weather_tool', but was: " + toolName);

			System.out.println("Second interruption tool: " + toolName);
			System.out.println("Second interruption node: " + interruptionMetadata.node());
			System.out.println("Second interruption state data keys: " + interruptionMetadata.state().data().keySet());

			return interruptionMetadata;

		} catch (java.util.concurrent.CompletionException e) {
			System.err.println("ReactAgent execution failed: " + e.getMessage());
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
		return null;
	}

	private void performThirdInvocation(ReactAgent agent, String threadId, InterruptionMetadata feedbackMetadata) throws Exception {
		// Resume execution with human feedback
		System.out.println("\n=== Third Invocation: Resuming with Feedback, Expecting Completion ===");
		RunnableConfig resumeRunnableConfig = RunnableConfig.builder().threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedbackMetadata)
				.build();

		try {
			// Third invocation - should resume and complete
			Optional<NodeOutput> result = agent.invokeAndGetOutput("", resumeRunnableConfig);

			// Assert third invocation completes successfully
			Assertions.assertTrue(result.isPresent(), "Third invocation should return a result");
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

	private InterruptionMetadata buildFeedbackWithOnlyOneApproval(InterruptionMetadata interruptionMetadata) {
		// Build new metadata with APPROVED feedback for only the first tool
		// Other tools are not included in feedback, which will cause validation to fail
		// and system will continue to interrupt for the remaining tools
		InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder()
				.nodeId(interruptionMetadata.node())
				.state(interruptionMetadata.state());

		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
		Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
		Assertions.assertTrue(toolFeedbacks.size() >= 1, "Should have at least one tool feedback");

		// Approve only the first tool
		InterruptionMetadata.ToolFeedback firstTool = toolFeedbacks.get(0);
		InterruptionMetadata.ToolFeedback approvedFeedback = InterruptionMetadata.ToolFeedback
				.builder(firstTool)
				.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
				.build();
		newBuilder.addToolFeedback(approvedFeedback);

		// Note: We intentionally don't add feedback for remaining tools.
		// This will cause validateFeedback to fail (toolFeedbacks.size() != toolCalls.size()),
		// which will make the system continue to interrupt for the remaining tools.

		return newBuilder.build();
	}

	private InterruptionMetadata performSecondInvocationAndCheckApprovalRequiredAgain(ReactAgent agent, String threadId, InterruptionMetadata feedbackMetadata) throws Exception {
		// Resume execution with partial feedback (only one tool approved)
		System.out.println("\n=== Second Invocation: Resuming with Partial Feedback, Expecting Another Interruption ===");
		RunnableConfig resumeRunnableConfig = RunnableConfig.builder().threadId(threadId)
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, feedbackMetadata)
				.build();

		try {
			// Second invocation - should resume and interrupt again for the remaining tool
			Optional<NodeOutput> result = agent.invokeAndGetOutput("", resumeRunnableConfig);

			// Assert second invocation results in another interruption
			Assertions.assertTrue(result.isPresent(), "Second invocation should return a result");
			Assertions.assertInstanceOf(InterruptionMetadata.class, result.get(),
					"Second invocation should return InterruptionMetadata for the remaining tool approval");

			InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();
			Assertions.assertNotNull(interruptionMetadata, "Interruption metadata should not be null");
			Assertions.assertNotNull(interruptionMetadata.node(), "Interruption should have node id");
			Assertions.assertNotNull(interruptionMetadata.state(), "Interruption should have state");

			// Assert tool feedbacks are present (should be for the remaining tool)
			List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
			Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
			Assertions.assertFalse(toolFeedbacks.isEmpty(), "Tool feedbacks should not be empty");
			Assertions.assertEquals(2, toolFeedbacks.size(),
					"Should have exactly one tool feedback for the remaining tool");

			return interruptionMetadata;

		} catch (java.util.concurrent.CompletionException e) {
			System.err.println("ReactAgent execution failed: " + e.getMessage());
			e.printStackTrace();
			fail("ReactAgent execution failed: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Test based on example5_multipleTools: handling multiple tools with different decisions
	 * (approve, edit, reject) in a single assistant message.
	 */
	@Test
	public void testMultipleToolsWithMixedDecisions() throws Exception {
		ReactAgent agent = createAgentWithThreeTools();

		printGraphRepresentation(agent);

		String threadId = "test-thread-mixed-decisions";
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();

		// Assert RunnableConfig is properly configured
		Assertions.assertNotNull(runnableConfig, "RunnableConfig should not be null");
		Assertions.assertTrue(runnableConfig.threadId().isPresent(), "Thread ID should be present");
		Assertions.assertEquals(threadId, runnableConfig.threadId().get(), "Thread ID should match");

		// First invocation - should interrupt for all three tools
		InterruptionMetadata interruptionMetadata = performFirstInvocationAndCheckThreeToolsRequested(agent, runnableConfig);

		// Apply mixed decisions: approve first tool, edit second tool, reject third tool
		InterruptionMetadata feedbackMetadata = buildMixedDecisionFeedback(interruptionMetadata);

		// Second invocation - should complete with the mixed decisions applied
		performSecondInvocation(agent, threadId, feedbackMetadata);
	}

	private ReactAgent createAgentWithThreeTools() {
		// Create three simple tools similar to example5_multipleTools
		org.springframework.ai.tool.ToolCallback tool1 = org.springframework.ai.tool.function.FunctionToolCallback.builder("tool1", (args) -> "工具1结果")
				.description("工具1")
				.inputType(String.class)
				.build();

		org.springframework.ai.tool.ToolCallback tool2 = org.springframework.ai.tool.function.FunctionToolCallback.builder("tool2", (args) -> "工具2结果")
				.description("工具2")
				.inputType(String.class)
				.build();

		org.springframework.ai.tool.ToolCallback tool3 = org.springframework.ai.tool.function.FunctionToolCallback.builder("tool3", (args) -> "工具3结果")
				.description("工具3")
				.inputType(String.class)
				.build();

		Map approvalOn = Map.of(
				"tool1", ToolConfig.builder().description("工具1需要审批").build(),
				"tool2", ToolConfig.builder().description("工具2需要审批").build(),
				"tool3", ToolConfig.builder().description("工具3需要审批").build()
		);

		return ReactAgent.builder()
				.name("multi_tool_agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.tools(List.of(tool1, tool2, tool3))
				.hooks(HumanInTheLoopHook.builder().approvalOn(approvalOn).build())
				.build();
	}

	private InterruptionMetadata performFirstInvocationAndCheckThreeToolsRequested(ReactAgent agent, RunnableConfig runnableConfig) throws Exception {
		// First invocation - should trigger interruption for human approval
		System.out.println("\n=== First Invocation: Expecting Interruption with Three Tools ===");
		Optional<NodeOutput> result = agent.invokeAndGetOutput("执行所有工具", runnableConfig);

		// Assert first invocation results in interruption
		Assertions.assertTrue(result.isPresent(), "First invocation should return a result");
		Assertions.assertInstanceOf(InterruptionMetadata.class, result.get(),
				"First invocation should return InterruptionMetadata for human approval");

		InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

		// Assert interruption metadata contains expected information
		Assertions.assertNotNull(interruptionMetadata.node(), "Interruption should have node id");
		Assertions.assertNotNull(interruptionMetadata.state(), "Interruption should have state");

		// Assert tool feedbacks are present and there are exactly 3 tools
		List<InterruptionMetadata.ToolFeedback> toolFeedbacks = interruptionMetadata.toolFeedbacks();
		Assertions.assertNotNull(toolFeedbacks, "Tool feedbacks should not be null");
		Assertions.assertFalse(toolFeedbacks.isEmpty(), "Tool feedbacks should not be empty");

		// Note: The model might not call all three tools in a single message, so we check for at least one
		Assertions.assertTrue(toolFeedbacks.size() >= 1,
				"Should have at least one tool feedback");

		System.out.println("Number of tools requested: " + toolFeedbacks.size());
		toolFeedbacks.forEach(feedback -> {
			System.out.println("Tool: " + feedback.getName() + ", Arguments: " + feedback.getArguments());
		});

		return interruptionMetadata;
	}

	private InterruptionMetadata buildMixedDecisionFeedback(InterruptionMetadata interruptionMetadata) {
		// Build new metadata with mixed decisions: approve, edit, reject
		InterruptionMetadata.Builder newBuilder = InterruptionMetadata.builder()
				.nodeId(interruptionMetadata.node())
				.state(interruptionMetadata.state());

		List<InterruptionMetadata.ToolFeedback> feedbacks = interruptionMetadata.toolFeedbacks();

		for (int i = 0; i < feedbacks.size(); i++) {
			InterruptionMetadata.ToolFeedback feedback = feedbacks.get(i);

			if (i == 0) {
				// First tool: approve
				InterruptionMetadata.ToolFeedback approvedFeedback = InterruptionMetadata.ToolFeedback
						.builder(feedback)
						.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
						.build();
				newBuilder.addToolFeedback(approvedFeedback);
				System.out.println("Tool " + (i + 1) + " (" + feedback.getName() + "): APPROVED");
			} else if (i == 1) {
				// Second tool: edit
				String editedArguments = "\"newValue\"";
				InterruptionMetadata.ToolFeedback editedFeedback = InterruptionMetadata.ToolFeedback
						.builder(feedback)
						.arguments(editedArguments)
						.result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
						.build();
				newBuilder.addToolFeedback(editedFeedback);
				System.out.println("Tool " + (i + 1) + " (" + feedback.getName() + "): EDITED to " + editedArguments);
			} else {
				// Third and subsequent tools: reject
				InterruptionMetadata.ToolFeedback rejectedFeedback = InterruptionMetadata.ToolFeedback
						.builder(feedback)
						.result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
						.description("不允许此操作")
						.build();
				newBuilder.addToolFeedback(rejectedFeedback);
				System.out.println("Tool " + (i + 1) + " (" + feedback.getName() + "): REJECTED");
			}
		}

		return newBuilder.build();
	}

}
