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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingNodeTest {

	@Test
	void selectedFlowAgentWrapperOutputIsClearedBeforeRouteExecution() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(
				List.of("{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write and review article\"}]}"));
		SequentialAgent writingWorkflow = workflow("writing_workflow", chatModel);
		LlmRoutingAgent routingAgent = routingAgent(chatModel, List.of(writingWorkflow));

		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writingWorkflow));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("Please write an article")),
				outputKeyToParent("writing_workflow"), "stale wrapper answer"));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());

		assertEquals(List.of("writing_workflow"), command.gotoNodes());
		assertEquals(List.of("writing_workflow"),
				command.update().get(RoutingNode.routedAgentNamesKey("routing_agent")));
		assertEquals("write and review article", command.update().get("writing_workflow_input"));
		assertSame(OverAllState.MARK_FOR_REMOVAL,
				command.update().get(outputKeyToParent("writing_workflow")));
		assertTrue(command.update().containsKey(outputKeyToParent("writing_workflow")),
				"Selected FlowAgent wrapper output must be cleared so checkpointed wrappers are not reused");
	}

	@Test
	void staleSelectedFlowAgentWrapperIsRemovedFromCheckpointedState() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(
				List.of("{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write and review article\"}]}"));
		SequentialAgent writingWorkflow = workflow("writing_workflow", chatModel);
		LlmRoutingAgent routingAgent = routingAgent(chatModel, List.of(writingWorkflow));

		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writingWorkflow));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("Please write an article")),
				outputKeyToParent("writing_workflow"), "stale wrapper answer"));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());
		state.updateState(command.update());

		assertFalse(state.value(outputKeyToParent("writing_workflow")).isPresent(),
				"The route update must remove checkpointed wrapper output before the selected subgraph runs");
		assertEquals("write and review article", state.value("writing_workflow_input").orElse(null));
		assertEquals(List.of("writing_workflow"),
				state.value(RoutingNode.routedAgentNamesKey("routing_agent")).orElse(null));
	}

	@Test
	void multipleSelectedFlowAgentWrappersAreClearedBeforeParallelRouteExecution() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(List.of("""
				{"agents":[
				  {"agent":"writing_workflow","query":"write article"},
				  {"agent":"review_workflow","query":"review article"}
				]}
				"""));
		SequentialAgent writingWorkflow = workflow("writing_workflow", chatModel);
		SequentialAgent reviewWorkflow = workflow("review_workflow", chatModel);
		LlmRoutingAgent routingAgent = routingAgent(chatModel, List.of(writingWorkflow, reviewWorkflow));

		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writingWorkflow, reviewWorkflow));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("Write and review")),
				outputKeyToParent("writing_workflow"), "old writing wrapper",
				outputKeyToParent("review_workflow"), "old review wrapper"));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());

		assertEquals(List.of("writing_workflow", "review_workflow"), command.gotoNodes());
		assertSame(OverAllState.MARK_FOR_REMOVAL,
				command.update().get(outputKeyToParent("writing_workflow")));
		assertSame(OverAllState.MARK_FOR_REMOVAL,
				command.update().get(outputKeyToParent("review_workflow")));
		assertEquals("write article", command.update().get("writing_workflow_input"));
		assertEquals("review article", command.update().get("review_workflow_input"));
	}

	@Test
	void unselectedFlowAgentWrapperOutputIsNotTouched() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(
				List.of("{\"agents\":[{\"agent\":\"writing_workflow\",\"query\":\"write article\"}]}"));
		SequentialAgent writingWorkflow = workflow("writing_workflow", chatModel);
		SequentialAgent skippedWorkflow = workflow("skipped_workflow", chatModel);
		LlmRoutingAgent routingAgent = routingAgent(chatModel, List.of(writingWorkflow, skippedWorkflow));

		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writingWorkflow, skippedWorkflow));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("Write only")),
				outputKeyToParent("writing_workflow"), "old writing wrapper",
				outputKeyToParent("skipped_workflow"), "old skipped wrapper"));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());

		assertSame(OverAllState.MARK_FOR_REMOVAL,
				command.update().get(outputKeyToParent("writing_workflow")));
		assertFalse(command.update().containsKey(outputKeyToParent("skipped_workflow")),
				"Unselected workflow wrappers are ignored by the merge marker and do not need mutation");
		assertFalse(command.update().containsKey("skipped_workflow_input"));
	}

	@Test
	void selectedBaseAgentDoesNotEmitSubgraphWrapperRemoval() throws Exception {
		ScriptedChatModel chatModel = new ScriptedChatModel(
				List.of("{\"agents\":[{\"agent\":\"writer_agent\",\"query\":\"write article\"}]}"));
		ReactAgent writerAgent = reactAgent("writer_agent", chatModel);
		LlmRoutingAgent routingAgent = routingAgent(chatModel, List.of(writerAgent));

		RoutingNode node = new RoutingNode(chatModel, routingAgent, List.of(writerAgent));
		OverAllState state = new OverAllState(Map.of(
				"messages", List.<Message>of(new UserMessage("Please write"))));

		MultiCommand command = node.apply(state, RunnableConfig.builder().build());

		assertEquals(List.of("writer_agent"), command.gotoNodes());
		assertEquals("write article", command.update().get("writer_agent_input"));
		assertFalse(command.update().containsKey(outputKeyToParent("writer_agent")),
				"Only FlowAgent subgraphs have parent wrapper output to clear");
	}

	private static ReactAgent reactAgent(String name, ChatModel chatModel) {
		return ReactAgent.builder()
			.name(name)
			.description("Scripted base agent")
			.model(chatModel)
			.instruction("Return a scripted answer")
			.outputKey(name + "_answer")
			.build();
	}

	private static SequentialAgent workflow(String name, ChatModel chatModel) {
		return SequentialAgent.builder()
			.name(name)
			.description("Scripted workflow")
			.subAgents(List.of(reactAgent(name + "_writer", chatModel)))
			.build();
	}

	private static LlmRoutingAgent routingAgent(ChatModel chatModel, List<Agent> subAgents) {
		return LlmRoutingAgent.builder()
			.name("routing_agent")
			.description("Routes writing tasks")
			.model(chatModel)
			.subAgents(subAgents)
			.build();
	}

	private static final class ScriptedChatModel implements ChatModel {

		private final List<String> responses;

		private final AtomicInteger calls = new AtomicInteger();

		private ScriptedChatModel(List<String> responses) {
			this.responses = responses;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
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

	}

}
