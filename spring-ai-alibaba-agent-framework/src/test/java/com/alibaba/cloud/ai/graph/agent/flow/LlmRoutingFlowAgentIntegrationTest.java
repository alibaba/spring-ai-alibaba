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

package com.alibaba.cloud.ai.graph.agent.flow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for routing to nested flow agents.
 */
class LlmRoutingFlowAgentIntegrationTest {

	@Test
	void llmRoutingAgentCanInvokeSequentialFlowAgentSubAgent() throws Exception {
		// The scripted responses drive the full graph without a remote model:
		// route to the workflow, generate a draft, then return the reviewed final answer.
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write and review article\"}]}",
				"Draft article",
				"Reviewed article"));

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("Writes draft articles")
			.instruction("Write a draft for: {writing_workflow_input}")
			.outputKey("draft_article")
			.build();

		ReactAgent reviewerAgent = ReactAgent.builder()
			.name("reviewer_agent")
			.model(chatModel)
			.description("Reviews draft articles")
			.instruction("Review this draft: {draft_article}")
			.outputKey("reviewed_article")
			.build();

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes and reviews an article")
			.subAgents(List.of(writerAgent, reviewerAgent))
			.build();

		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(writingWorkflow))
			.build();

		Optional<OverAllState> result = routingAgent.invoke("Please write an article");

		assertTrue(result.isPresent());
		// These assertions prove the routed SequentialAgent actually ran both nested agents.
		assertMessageText(result.get().value("draft_article"), "Draft article");
		assertMessageText(result.get().value("reviewed_article"), "Reviewed article");
		// The routing merge should expose the workflow's final nested output as the merged result.
		assertEquals("Reviewed article", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		assertEquals(3, chatModel.callCount());
	}

	@Test
	void routedDefaultOutputFlowAgentIgnoresCheckpointedWrapperOutput() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write article\"}]}",
				"Current workflow answer"));

		ReactAgent writerAgent = ReactAgent.builder()
			.name("writer_agent")
			.model(chatModel)
			.description("Writes articles")
			.instruction("Write the current answer")
			.build();
		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes an article")
			.subAgents(List.of(writerAgent))
			.build();
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(writingWorkflow))
			.build();

		Optional<OverAllState> result = routingAgent.invoke(Map.of(
				"input", "Please write an article",
				"messages", List.<Message>of(new UserMessage("Please write an article")),
				outputKeyToParent("writing_workflow"), "Stale wrapper answer"));

		assertTrue(result.isPresent());
		assertEquals("Current workflow answer", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		assertFalse(result.get().data().containsValue("Stale wrapper answer"),
				"Checkpointed workflow wrappers must not be reused as current routed output");
	}

	@Test
	void routedDefaultOutputFlowAgentIgnoresWrapperOutputFromPreviousCheckpointTurn() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write old article\"}]}",
				"Old workflow answer",
				"{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write current article\"}]}",
				"Current workflow answer"));

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes an article")
			.subAgents(List.of(defaultOutputAgent("writer_agent", chatModel)))
			.build();
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(writingWorkflow))
			.saver(new MemorySaver())
			.build();
		RunnableConfig sameThread = RunnableConfig.builder().threadId("routing-thread").build();

		Optional<OverAllState> first = routingAgent.invoke("Please write the old article", sameThread);
		Optional<OverAllState> second = routingAgent.invoke("Please write the current article", sameThread);

		assertTrue(first.isPresent());
		assertTrue(second.isPresent());
		assertEquals("Old workflow answer", first.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		assertEquals("Current workflow answer", second.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
	}

	@Test
	void multipleRoutedDefaultOutputFlowAgentsIgnoreCheckpointedWrapperOutputs() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"""
				{"agents":[
				  {"agent":"first_workflow","query":"write first"},
				  {"agent":"second_workflow","query":"write second"}
				]}
				""",
				"Current first answer",
				"Current second answer",
				"Merged current answer"));

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("Writes the first answer")
			.subAgents(List.of(defaultOutputAgent("first_writer", chatModel)))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Writes the second answer")
			.subAgents(List.of(defaultOutputAgent("second_writer", chatModel)))
			.build();
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(firstWorkflow, secondWorkflow))
			.build();

		Optional<OverAllState> result = routingAgent.invoke(Map.of(
				"input", "Please write both answers",
				"messages", List.<Message>of(new UserMessage("Please write both answers")),
				outputKeyToParent("first_workflow"), "Stale first wrapper",
				outputKeyToParent("second_workflow"), "Stale second wrapper"));

		assertTrue(result.isPresent());
		assertEquals("Merged current answer", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		String synthesizedPrompt = chatModel.prompts().get(chatModel.prompts().size() - 1).getContents();
		assertTrue(synthesizedPrompt.contains("Current first answer"));
		assertTrue(synthesizedPrompt.contains("Current second answer"));
		assertFalse(synthesizedPrompt.contains("Stale first wrapper"));
		assertFalse(synthesizedPrompt.contains("Stale second wrapper"));
	}

	@Test
	void multipleRoutedWorkflowsWithSharedOutputKeyIgnoreCheckpointedWrapperOutputs() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"""
				{"agents":[
				  {"agent":"first_workflow","query":"write first"},
				  {"agent":"second_workflow","query":"write second"}
				]}
				""",
				"Current first explicit answer",
				"Current second explicit answer",
				"Merged explicit answer"));

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("Writes the first answer")
			.subAgents(List.of(outputAgent("first_writer", chatModel, "shared_answer")))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Writes the second answer")
			.subAgents(List.of(outputAgent("second_writer", chatModel, "shared_answer")))
			.build();
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes writing tasks")
			.subAgents(List.of(firstWorkflow, secondWorkflow))
			.build();

		Optional<OverAllState> result = routingAgent.invoke(Map.of(
				"input", "Please write both answers",
				"messages", List.<Message>of(new UserMessage("Please write both answers")),
				outputKeyToParent("first_workflow"), "Stale first explicit wrapper",
				outputKeyToParent("second_workflow"), "Stale second explicit wrapper"));

		assertTrue(result.isPresent());
		assertEquals("Merged explicit answer", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		String synthesizedPrompt = chatModel.prompts().get(chatModel.prompts().size() - 1).getContents();
		assertTrue(synthesizedPrompt.contains("Current first explicit answer"));
		assertTrue(synthesizedPrompt.contains("Current second explicit answer"));
		assertFalse(synthesizedPrompt.contains("Stale first explicit wrapper"));
		assertFalse(synthesizedPrompt.contains("Stale second explicit wrapper"));
	}

	@Test
	void routedNestedRoutingAgentIgnoresCheckpointedWrapperOutput() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of(
				"{\"agents\":[{\"agent\":\"child_router\",\"query\":\"route inside child\"}]}",
				"""
				{"agents":[
				  {"agent":"first_inner_agent","query":"answer first"},
				  {"agent":"second_inner_agent","query":"answer second"}
				]}
				""",
				"Current first inner answer",
				"Current second inner answer",
				"Current child merged answer"));

		LlmRoutingAgent childRouter = LlmRoutingAgent.builder()
			.name("child_router")
			.model(chatModel)
			.description("Routes inside the selected workflow")
			.subAgents(List.of(
					outputAgent("first_inner_agent", chatModel, "first_inner_answer"),
					outputAgent("second_inner_agent", chatModel, "second_inner_answer")))
			.build();
		LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
			.name("routing_agent")
			.model(chatModel)
			.description("Routes to a nested router")
			.subAgents(List.of(childRouter))
			.build();

		Optional<OverAllState> result = routingAgent.invoke(Map.of(
				"input", "Please route inside child",
				"messages", List.<Message>of(new UserMessage("Please route inside child")),
				outputKeyToParent("child_router"), "Stale child router wrapper"));

		assertTrue(result.isPresent());
		assertEquals("Current child merged answer", result.get().value(DEFAULT_MERGED_OUTPUT_KEY).orElse(null));
		assertFalse(result.get().data().containsValue("Stale child router wrapper"),
				"Nested router wrappers from earlier checkpoint turns must not be reused");
	}

	private static void assertMessageText(Optional<Object> value, String expectedText) {
		assertTrue(value.isPresent());
		AssistantMessage message = assertInstanceOf(AssistantMessage.class, value.get());
		assertEquals(expectedText, message.getText());
	}

	private static ReactAgent defaultOutputAgent(String name, ChatModel chatModel) {
		return outputAgent(name, chatModel, null);
	}

	private static ReactAgent outputAgent(String name, ChatModel chatModel, String outputKey) {
		return ReactAgent.builder()
			.name(name)
			.model(chatModel)
			.description("Returns a scripted answer")
			.instruction("Return a scripted answer")
			.outputKey(outputKey)
			.build();
	}

	private static final class ScriptedChatModel implements ChatModel {

		private final List<String> responses;

		private final AtomicInteger calls = new AtomicInteger();

		private final List<Prompt> prompts = new ArrayList<>();

		private ScriptedChatModel(List<String> responses) {
			this.responses = responses;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			int index = calls.getAndIncrement();
			if (index >= responses.size()) {
				throw new IllegalStateException("No scripted response for call " + index);
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.get(index)))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return calls.get();
		}

		private List<Prompt> prompts() {
			return prompts;
		}

	}

}
