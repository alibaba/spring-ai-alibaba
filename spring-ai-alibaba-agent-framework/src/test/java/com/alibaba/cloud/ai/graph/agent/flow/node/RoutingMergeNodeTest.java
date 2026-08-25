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

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
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

import static com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RoutingMergeNode}.
 *
 * <p>Covers the gh-4616 fix: when {@code LlmRoutingAgent} hands off to a single sub-agent,
 * the merge node must return that agent's answer verbatim instead of running it through the
 * synthesis LLM again (which produced a redundant model call and a duplicated, rephrased
 * answer for the user). Genuine multi-agent results are still synthesized.</p>
 */
class RoutingMergeNodeTest {

	/**
	 * When the router delegated to a single sub-agent, the merge node should pass that
	 * agent's answer through unchanged and must NOT call the synthesis LLM.
	 */
	@Test
	void singleRoutedResultIsPassedThroughWithoutSynthesis() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent poemAgent = mockAgent("poem_writer_agent", "poem_article");
		BaseAgent proseAgent = mockAgent("prose_writer_agent", "prose_article");

		// Router picked only the poem agent, so only its output key is present in state.
		OverAllState state = new OverAllState(Map.of(
				"poem_article", new AssistantMessage("A short modern poem about spring."),
				"messages", List.<Message>of(new UserMessage("Write a poem about spring"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(poemAgent, proseAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("A short modern poem about spring.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Single routed result must be returned verbatim, not re-synthesized");
		assertEquals(0, chatModel.callCount());
	}

	/**
	 * When multiple sub-agents produced results, the merge node should synthesize them
	 * through the LLM and return the synthesized answer.
	 */
	@Test
	void multipleResultsAreSynthesizedViaLlm() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		BaseAgent poemAgent = mockAgent("poem_writer_agent", "poem_article");
		BaseAgent proseAgent = mockAgent("prose_writer_agent", "prose_article");

		OverAllState state = new OverAllState(Map.of(
				"poem_article", new AssistantMessage("A poem."),
				"prose_article", new AssistantMessage("A prose piece."),
				"messages", List.<Message>of(new UserMessage("Write something about spring"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(poemAgent, proseAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Multiple results must be synthesized via the LLM");
		assertEquals(1, chatModel.callCount());
	}

	@Test
	void explicitListOutputPreservesAllEntries() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		BaseAgent searchAgent = mockAgent("search_agent", "search_results");
		List<String> searchResults = List.of("First search hit.", "Second search hit.");

		OverAllState state = new OverAllState(Map.of(
				"search_results", searchResults,
				"messages", List.<Message>of(new UserMessage("Search for routing results"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(searchAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals(searchResults.toString(), result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Explicit list outputs should preserve every entry, not only the last one");
		assertEquals(searchResults.toString(),
				RoutingMergeNode.extractText(Map.of("search_results", searchResults), "search_results"));
		assertEquals(searchResults.toString(),
				RoutingMergeNode.extractText(GraphResponse.done(Map.of("search_results", searchResults)),
						"search_results"));
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void wrapperPreferredOutputsUseConfiguredOrderAndIgnoreUnrelatedMessages() {
		String wrapperKey = outputKeyToParent("parallel_workflow");
		GraphResponse<Object> wrapper = GraphResponse.done(Map.of(
				"second_result", new AssistantMessage("Second result."),
				"first_result", new AssistantMessage("First result."),
				"messages", List.<Message>of(new AssistantMessage("Unrelated wrapper message."))
		));

		String text = RoutingMergeNode.extractText(wrapper, wrapperKey, false,
				List.of("first_result", "missing_result", "second_result"));

		assertEquals("First result.\n\nSecond result.", text,
				"Wrapper outputs should follow agent configuration, skip missing outputs, and not fall back to messages");
	}

	@Test
	void flowAgentResultIsResolvedFromNestedFinalOutputKey() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent draftAgent = mockAgent("draft_agent", "draft_article");
		BaseAgent reviewAgent = mockAgent("review_agent", "reviewed_article");
		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes and reviews an article")
			.subAgents(List.of(draftAgent, reviewAgent))
			.build();

		// The wrapper output can coexist with the nested final output after subgraph execution.
		// The merge node must count this as one routed source and avoid an unnecessary synthesis call.
		OverAllState state = new OverAllState(Map.of(
				"draft_article", new AssistantMessage("Draft that should not be returned."),
				"reviewed_article", new AssistantMessage("Reviewed final article."),
				outputKeyToParent("writing_workflow"), new AssistantMessage("Wrapped workflow result."),
				"messages", List.<Message>of(new UserMessage("Write an article"))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Reviewed final article.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Sequential FlowAgent result should use its final nested output");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void flowAgentResultFallsBackToMessagesForNestedAgentWithoutOutputKey() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent draftAgent = mockAgent("draft_agent", "draft_article");
		BaseAgent reviewerAgent = mockAgent("reviewer_agent", null);
		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes and reviews an article")
			.subAgents(List.of(draftAgent, reviewerAgent))
			.build();

		// AgentLlmNode and the ReactAgent subgraph adapter use messages as the default output
		// when a nested agent has no explicit outputKey.
		OverAllState state = new OverAllState(Map.of(
				"writing_workflow_input", "Write and review an article",
				"draft_article", new AssistantMessage("Draft that should not be returned."),
				"messages", List.<Message>of(
						new UserMessage("Write an article"),
						new AssistantMessage("Reviewed final article from messages."))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Reviewed final article from messages.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Sequential FlowAgent result should fall back to messages for default agent output");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void messagesFallbackDoesNotCollectUnselectedAgents() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent selectedWorkflow = SequentialAgent.builder()
			.name("selected_workflow")
			.description("Selected workflow")
			.subAgents(List.of(mockAgent("selected_final_agent", null)))
			.build();
		SequentialAgent skippedWorkflow = SequentialAgent.builder()
			.name("skipped_workflow")
			.description("Skipped workflow")
			.subAgents(List.of(mockAgent("skipped_final_agent", null)))
			.build();

		// messages is shared state, so only the routed workflow may use it as a default output.
		OverAllState state = new OverAllState(Map.of(
				"selected_workflow_input", "Run the selected workflow",
				"messages", List.<Message>of(
						new UserMessage("Run one workflow"),
						new AssistantMessage("Selected workflow answer."))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(selectedWorkflow, skippedWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Selected workflow answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared messages must not be collected for unselected agents");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void currentRouteSelectionIgnoresStaleCheckpointInputMarkers() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent selectedWorkflow = SequentialAgent.builder()
			.name("selected_workflow")
			.description("Selected workflow")
			.subAgents(List.of(mockAgent("selected_final_agent", null)))
			.build();
		SequentialAgent skippedWorkflow = SequentialAgent.builder()
			.name("skipped_workflow")
			.description("Skipped workflow")
			.subAgents(List.of(mockAgent("skipped_final_agent", null)))
			.build();

		// Checkpointed graph state can retain old <agent>_input keys across turns.
		// The explicit current-route marker must be authoritative for this run.
		OverAllState state = new OverAllState(Map.of(
				RoutingNode.ROUTED_AGENT_NAMES_KEY, List.of("selected_workflow"),
				"selected_workflow_input", "Run the selected workflow",
				"skipped_workflow_input", "Stale input from a previous checkpointed turn",
				"messages", List.<Message>of(
						new UserMessage("Run one workflow"),
						new AssistantMessage("Selected workflow answer.")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(selectedWorkflow, skippedWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Selected workflow answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Stale route inputs from checkpoints must not disable the single-agent messages fallback");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void unroutedFlowAgentsAreNotResolvedFromSharedExplicitOutputKeys() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent selectedWorkflow = SequentialAgent.builder()
			.name("selected_workflow")
			.description("Selected workflow")
			.subAgents(List.of(mockAgent("selected_final_agent", "shared_answer")))
			.build();
		SequentialAgent skippedWorkflow = SequentialAgent.builder()
			.name("skipped_workflow")
			.description("Skipped workflow")
			.subAgents(List.of(mockAgent("skipped_final_agent", "shared_answer")))
			.build();

		// Top-level routing markers define which workflow actually ran; unselected workflows
		// must not resolve shared parent-state keys even when their nested outputKey matches.
		OverAllState state = new OverAllState(Map.of(
				"selected_workflow_input", "Run the selected workflow",
				"shared_answer", new AssistantMessage("Selected workflow answer."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(selectedWorkflow, skippedWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Selected workflow answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Unrouted workflows must not collect shared explicit output keys");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void sharedExplicitOutputKeyIsCollectedOnlyOnceForMultipleRoutedAgents() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(mockAgent("first_final_agent", "shared_answer")))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(mockAgent("second_final_agent", "shared_answer")))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				"shared_answer", new AssistantMessage("Only one shared answer exists in state."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Only one shared answer exists in state.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"A shared explicit output key represents one parent-state value and must not be duplicated");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void multipleRoutedWorkflowsPreferWrapperOverSharedChildOutputKey() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(mockAgent("first_final_agent", "shared_answer")))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(mockAgent("second_final_agent", "shared_answer")))
			.build();

		// The child output key is shared and replace-merged in the parent state, while each
		// workflow wrapper carries the answer produced by that workflow branch.
		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				"shared_answer", new AssistantMessage("Shared child value that must not be attributed."),
				outputKeyToParent("first_workflow"), new AssistantMessage("First workflow wrapper answer."),
				outputKeyToParent("second_workflow"), new AssistantMessage("Second workflow wrapper answer."),
				"messages", List.<Message>of(new UserMessage("Run both workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Multiple routed workflows should synthesize their namespaced wrapper outputs");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First workflow wrapper answer."));
		assertTrue(promptContent.contains("Second workflow wrapper answer."));
		assertFalse(promptContent.contains("Shared child value that must not be attributed."));
	}

	@Test
	void ordinaryWorkflowWrappersIgnoreInheritedParentMergedResult() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(mockAgent("first_final_agent", null)))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(mockAgent("second_final_agent", null)))
			.build();

		// Ordinary workflow subgraphs inherit the parent's merged_result from checkpointed
		// state. Only nested routing wrappers may treat merged_result as their own answer.
		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				outputKeyToParent("first_workflow"), GraphResponse.done(Map.of(
						DEFAULT_MERGED_OUTPUT_KEY, "Previous parent merged answer.",
						"messages", List.<Message>of(new AssistantMessage("Current first workflow answer.")))),
				outputKeyToParent("second_workflow"), GraphResponse.done(Map.of(
						DEFAULT_MERGED_OUTPUT_KEY, "Previous parent merged answer.",
						"messages", List.<Message>of(new AssistantMessage("Current second workflow answer.")))),
				"messages", List.<Message>of(new UserMessage("Run both workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Ordinary workflow wrappers should synthesize their current message outputs");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("Current first workflow answer."));
		assertTrue(promptContent.contains("Current second workflow answer."));
		assertFalse(promptContent.contains("Previous parent merged answer."));
	}

	@Test
	void workflowWrapperMapsWithoutVisibleOutputAreIgnored() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writing workflow")
			.subAgents(List.of(mockAgent("writer_agent", "final_answer")))
			.build();

		// Wrapper snapshots can contain parent inputs, route markers, and user-only messages
		// after checkpoint merging. Those internal values are not attributable agent answers.
		OverAllState state = new OverAllState(Map.of(
				"writing_workflow_input", "Write an article",
				outputKeyToParent("writing_workflow"), GraphResponse.done(Map.of(
						"input", "Write an article",
						"writer_agent_input", "Write an article",
						"_routing_selected_agents_router", List.of("writing_workflow"),
						outputKeyToParent("older_workflow"), Map.of("previous", "snapshot"),
						"messages", List.<Message>of(new UserMessage("Write an article")))),
				"messages", List.<Message>of(new UserMessage("Write an article")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("No results found from any knowledge source.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Internal wrapper state must not be stringified as an agent answer");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void workflowWrapperMapsUseSingleVisibleOutputValue() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writing workflow")
			.subAgents(List.of(mockAgent("writer_agent", "final_answer")))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"writing_workflow_input", "Write an article",
				outputKeyToParent("writing_workflow"), GraphResponse.done(Map.of(
						"input", "Write an article",
						"writer_agent_input", "Write an article",
						"final_answer", new AssistantMessage("Current workflow answer."),
						"messages", List.<Message>of(new UserMessage("Write an article")))),
				"messages", List.<Message>of(new UserMessage("Write an article")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Current workflow answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"A wrapper snapshot with one visible output should still be usable");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferMergeOutputKeyOverWrapper() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		ParallelAgent firstParallel = ParallelAgent.builder()
			.name("first_parallel")
			.description("First parallel workflow")
			.subAgents(List.of(
					mockAgent("first_parallel_search", "first_search_result"),
					mockAgent("first_parallel_summary", "first_summary_result")))
			.mergeOutputKey("first_merged_result")
			.build();
		ParallelAgent secondParallel = ParallelAgent.builder()
			.name("second_parallel")
			.description("Second parallel workflow")
			.subAgents(List.of(
					mockAgent("second_parallel_search", "second_search_result"),
					mockAgent("second_parallel_summary", "second_summary_result")))
			.mergeOutputKey("second_merged_result")
			.build();

		// A ParallelAgent with mergeOutputKey has an explicit aggregate result. The workflow
		// wrapper may also be present, but it must not hide that aggregate output.
		OverAllState state = new OverAllState(Map.of(
				"first_parallel_input", "Run the first parallel workflow",
				"second_parallel_input", "Run the second parallel workflow",
				"first_merged_result", new AssistantMessage("First merged answer."),
				"second_merged_result", new AssistantMessage("Second merged answer."),
				outputKeyToParent("first_parallel"), new AssistantMessage("First wrapper answer that must not be used."),
				outputKeyToParent("second_parallel"), new AssistantMessage("Second wrapper answer that must not be used."),
				"messages", List.<Message>of(new UserMessage("Run both parallel workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstParallel, secondParallel));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Multiple routed parallel workflows should synthesize their mergeOutputKey results");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First merged answer."));
		assertTrue(promptContent.contains("Second merged answer."));
		assertFalse(promptContent.contains("First wrapper answer that must not be used."));
		assertFalse(promptContent.contains("Second wrapper answer that must not be used."));
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferWrapperWhenMergeOutputKeyIsShared() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		ParallelAgent firstParallel = ParallelAgent.builder()
			.name("first_parallel")
			.description("First parallel workflow")
			.subAgents(List.of(
					mockAgent("first_parallel_search", "first_search_result"),
					mockAgent("first_parallel_summary", "first_summary_result")))
			.mergeOutputKey("shared_merged_result")
			.build();
		ParallelAgent secondParallel = ParallelAgent.builder()
			.name("second_parallel")
			.description("Second parallel workflow")
			.subAgents(List.of(
					mockAgent("second_parallel_search", "second_search_result"),
					mockAgent("second_parallel_summary", "second_summary_result")))
			.mergeOutputKey("shared_merged_result")
			.build();

		// Reused mergeOutputKey values are shared in the parent state, so each workflow must
		// fall back to its namespaced wrapper to avoid attributing one result to both sources.
		OverAllState state = new OverAllState(Map.of(
				"first_parallel_input", "Run the first parallel workflow",
				"second_parallel_input", "Run the second parallel workflow",
				"shared_merged_result", new AssistantMessage("Shared merge value that must not be attributed."),
				outputKeyToParent("first_parallel"), GraphResponse.done(Map.of(
						"shared_merged_result", new AssistantMessage("First parallel aggregate answer."),
						"messages", List.<Message>of(
								new AssistantMessage("First child response that must not be used."),
								new AssistantMessage("First last child response that must not be used.")))),
				outputKeyToParent("second_parallel"), GraphResponse.done(Map.of(
						"shared_merged_result", new AssistantMessage("Second parallel aggregate answer."),
						"messages", List.<Message>of(
								new AssistantMessage("Second child response that must not be used."),
								new AssistantMessage("Second last child response that must not be used.")))),
				"messages", List.<Message>of(new UserMessage("Run both parallel workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstParallel, secondParallel));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared mergeOutputKey values should be resolved through workflow wrappers");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First parallel aggregate answer."));
		assertTrue(promptContent.contains("Second parallel aggregate answer."));
		assertFalse(promptContent.contains("Shared merge value that must not be attributed."));
		assertFalse(promptContent.contains("First last child response that must not be used."));
		assertFalse(promptContent.contains("Second last child response that must not be used."));
	}

	@Test
	void multipleRoutedSequentialWorkflowsPreferFinalParallelMergeKeyInsideWrappers() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		ParallelAgent firstFinalParallel = ParallelAgent.builder()
			.name("first_final_parallel")
			.description("First final parallel")
			.subAgents(List.of(
					mockAgent("first_final_search", "first_search_result"),
					mockAgent("first_final_summary", "first_summary_result")))
			.mergeOutputKey("shared_merged_result")
			.build();
		ParallelAgent secondFinalParallel = ParallelAgent.builder()
			.name("second_final_parallel")
			.description("Second final parallel")
			.subAgents(List.of(
					mockAgent("second_final_search", "second_search_result"),
					mockAgent("second_final_summary", "second_summary_result")))
			.mergeOutputKey("shared_merged_result")
			.build();
		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(firstFinalParallel))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(secondFinalParallel))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				"shared_merged_result", new AssistantMessage("Shared merge value that must not be attributed."),
				outputKeyToParent("first_workflow"), GraphResponse.done(Map.of(
						"shared_merged_result", new AssistantMessage("First workflow aggregate answer."),
						"messages", List.<Message>of(
								new AssistantMessage("First workflow child response that must not be used.")))),
				outputKeyToParent("second_workflow"), GraphResponse.done(Map.of(
						"shared_merged_result", new AssistantMessage("Second workflow aggregate answer."),
						"messages", List.<Message>of(
								new AssistantMessage("Second workflow child response that must not be used.")))),
				"messages", List.<Message>of(new UserMessage("Run both workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Sequential workflows ending in ParallelAgent should read the wrapper aggregate output");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First workflow aggregate answer."));
		assertTrue(promptContent.contains("Second workflow aggregate answer."));
		assertFalse(promptContent.contains("Shared merge value that must not be attributed."));
		assertFalse(promptContent.contains("First workflow child response that must not be used."));
		assertFalse(promptContent.contains("Second workflow child response that must not be used."));
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferWrapperWhenChildOutputKeysAreShared() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		ParallelAgent firstParallel = ParallelAgent.builder()
			.name("first_parallel")
			.description("First parallel workflow")
			.subAgents(List.of(
					mockAgent("first_parallel_search", "shared_search_result"),
					mockAgent("first_parallel_summary", "shared_summary_result")))
			.build();
		ParallelAgent secondParallel = ParallelAgent.builder()
			.name("second_parallel")
			.description("Second parallel workflow")
			.subAgents(List.of(
					mockAgent("second_parallel_search", "shared_search_result"),
					mockAgent("second_parallel_summary", "shared_summary_result")))
			.build();

		// Without a mergeOutputKey, ParallelAgent exposes child outputs. If those keys are
		// reused by another routed workflow, only the workflow wrapper is safely attributable.
		OverAllState state = new OverAllState(Map.of(
				"first_parallel_input", "Run the first parallel workflow",
				"second_parallel_input", "Run the second parallel workflow",
				"shared_search_result", new AssistantMessage("Shared search value that must not be attributed."),
				"shared_summary_result", new AssistantMessage("Shared summary value that must not be attributed."),
				outputKeyToParent("first_parallel"), GraphResponse.done(Map.of(
						"shared_search_result", new AssistantMessage("First parallel search answer."),
						"shared_summary_result", new AssistantMessage("First parallel summary answer."),
						"messages", List.<Message>of(
								new AssistantMessage("First unrelated wrapper message.")))),
				outputKeyToParent("second_parallel"), GraphResponse.done(Map.of(
						"shared_search_result", new AssistantMessage("Second parallel search answer."),
						"shared_summary_result", new AssistantMessage("Second parallel summary answer."),
						"messages", List.<Message>of(
								new AssistantMessage("Second unrelated wrapper message.")))),
				"messages", List.<Message>of(new UserMessage("Run both parallel workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstParallel, secondParallel));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared child output keys should be resolved through workflow wrappers");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First parallel search answer."));
		assertTrue(promptContent.contains("First parallel summary answer."));
		assertTrue(promptContent.contains("Second parallel search answer."));
		assertTrue(promptContent.contains("Second parallel summary answer."));
		assertFalse(promptContent.contains("Shared search value that must not be attributed."));
		assertFalse(promptContent.contains("Shared summary value that must not be attributed."));
		assertFalse(promptContent.contains("First unrelated wrapper message."));
		assertFalse(promptContent.contains("Second unrelated wrapper message."));
	}

	@Test
	void multipleRoutedSequentialWorkflowsPreserveFinalParallelChildOutputsInsideWrappers() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		ParallelAgent firstFinalParallel = ParallelAgent.builder()
			.name("first_final_parallel")
			.description("First final parallel")
			.subAgents(List.of(
					mockAgent("first_search", "shared_search_result"),
					mockAgent("first_summary", "shared_summary_result")))
			.build();
		ParallelAgent secondFinalParallel = ParallelAgent.builder()
			.name("second_final_parallel")
			.description("Second final parallel")
			.subAgents(List.of(
					mockAgent("second_search", "shared_search_result"),
					mockAgent("second_summary", "shared_summary_result")))
			.build();
		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(firstFinalParallel))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(secondFinalParallel))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				"shared_search_result", new AssistantMessage("Shared search value that must not be attributed."),
				"shared_summary_result", new AssistantMessage("Shared summary value that must not be attributed."),
				outputKeyToParent("first_workflow"), GraphResponse.done(Map.of(
						"shared_search_result", new AssistantMessage("First workflow search answer."),
						"shared_summary_result", new AssistantMessage("First workflow summary answer."),
						"messages", List.<Message>of(new AssistantMessage("First unrelated wrapper message.")))),
				outputKeyToParent("second_workflow"), GraphResponse.done(Map.of(
						"shared_search_result", new AssistantMessage("Second workflow search answer."),
						"shared_summary_result", new AssistantMessage("Second workflow summary answer."),
						"messages", List.<Message>of(new AssistantMessage("Second unrelated wrapper message.")))),
				"messages", List.<Message>of(new UserMessage("Run both workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY));
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First workflow search answer."));
		assertTrue(promptContent.contains("First workflow summary answer."));
		assertTrue(promptContent.contains("Second workflow search answer."));
		assertTrue(promptContent.contains("Second workflow summary answer."));
		assertFalse(promptContent.contains("Shared search value that must not be attributed."));
		assertFalse(promptContent.contains("Shared summary value that must not be attributed."));
		assertFalse(promptContent.contains("First unrelated wrapper message."));
		assertFalse(promptContent.contains("Second unrelated wrapper message."));
	}

	@Test
	void parallelAgentWithoutMergeOutputKeyCollectsAllChildOutputs() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent firstAgent = mockAgent("first_agent", "first_answer");
		BaseAgent secondAgent = mockAgent("second_agent", "second_answer");
		ParallelAgent parallelWorkflow = ParallelAgent.builder()
			.name("parallel_workflow")
			.description("Runs child agents in parallel")
			.subAgents(List.of(firstAgent, secondAgent))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"parallel_workflow_input", "Run the parallel workflow",
				"first_answer", new AssistantMessage("First child answer."),
				"second_answer", new AssistantMessage("Second child answer."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(parallelWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("First child answer.\n\nSecond child answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"ParallelAgent without mergeOutputKey should expose all child outputs as one routed source");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void sequentialAgentWithFinalParallelAgentCollectsAllChildOutputs() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent draftAgent = mockAgent("draft_agent", "draft_answer");
		BaseAgent styleReviewer = mockAgent("style_reviewer", "style_answer");
		BaseAgent factReviewer = mockAgent("fact_reviewer", "fact_answer");
		ParallelAgent reviewParallel = ParallelAgent.builder()
			.name("review_parallel")
			.description("Runs final reviewers in parallel")
			.subAgents(List.of(styleReviewer, factReviewer))
			.build();
		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes and reviews an article")
			.subAgents(List.of(draftAgent, reviewParallel))
			.build();

		// A SequentialAgent's effective result comes from its final agent. If that final
		// agent is a ParallelAgent without a mergeOutputKey, all child outputs form one
		// routed source and none of them should be dropped.
		OverAllState state = new OverAllState(Map.of(
				"writing_workflow_input", "Write and review an article",
				"draft_answer", new AssistantMessage("Draft that should not be returned."),
				"style_answer", new AssistantMessage("Style review."),
				"fact_answer", new AssistantMessage("Fact review."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Style review.\n\nFact review.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"SequentialAgent should preserve all outputs from a final ParallelAgent");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void messagesFallbackIsNotUsedForMultipleRoutedAgents() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent firstWorkflow = SequentialAgent.builder()
			.name("first_workflow")
			.description("First workflow")
			.subAgents(List.of(mockAgent("first_final_agent", null)))
			.build();
		SequentialAgent secondWorkflow = SequentialAgent.builder()
			.name("second_workflow")
			.description("Second workflow")
			.subAgents(List.of(mockAgent("second_final_agent", null)))
			.build();

		// With more than one routed workflow, messages cannot be attributed to a single source.
		OverAllState state = new OverAllState(Map.of(
				"first_workflow_input", "Run the first workflow",
				"second_workflow_input", "Run the second workflow",
				"messages", List.<Message>of(
						new UserMessage("Run both workflows"),
						new AssistantMessage("Shared last answer."))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstWorkflow, secondWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("No results found from any knowledge source.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared messages must not be duplicated across multiple routed agents");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void messagesFallbackIgnoresUserOnlyHistory() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		SequentialAgent writingWorkflow = SequentialAgent.builder()
			.name("writing_workflow")
			.description("Writes an article")
			.subAgents(List.of(mockAgent("writer_agent", null)))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"writing_workflow_input", "Write an article",
				"messages", List.<Message>of(new UserMessage("Write an article")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(writingWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("No results found from any knowledge source.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"User-only message history must not be treated as an agent answer");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void opaqueCustomFlowUsesCurrentWrapperMessageInsteadOfConfiguredBranchOutputs() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		BaseAgent firstBranch = mockAgent("first_branch", "first_answer");
		BaseAgent secondBranch = mockAgent("second_branch", "second_answer");
		FlowAgent conditionalWorkflow = new StubConditionalFlowAgent("conditional_workflow",
				List.of(firstBranch, secondBranch));

		// The first branch output is inherited from a checkpoint while only the second
		// branch ran now. Configured children are not proof that both branches executed.
		OverAllState state = new OverAllState(Map.of(
				"conditional_workflow_input", "Run the selected branch",
				"first_answer", new AssistantMessage("Stale first-branch answer."),
				"second_answer", new AssistantMessage("Current second-branch answer."),
				outputKeyToParent("conditional_workflow"), GraphResponse.done(Map.of(
						"first_answer", new AssistantMessage("Stale first-branch answer."),
						"second_answer", new AssistantMessage("Current second-branch answer."),
						"messages", List.<Message>of(
								new UserMessage("Run the selected branch"),
								new AssistantMessage("Current second-branch answer."))))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(conditionalWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Current second-branch answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Custom flows should expose their current wrapper answer without probing stale branch keys");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void opaqueCustomFlowExposesSingleExplicitWrapperOutput() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		FlowAgent conditionalWorkflow = new StubConditionalFlowAgent("conditional_workflow",
				List.of(
						mockAgent("first_branch", "first_answer"),
						mockAgent("second_branch", "second_answer")));

		// A custom graph may end in a deterministic node that writes an explicit state key
		// without appending an assistant message. The single visible wrapper value is the
		// attributable result; the stale parent-state branch key must not be probed.
		OverAllState state = new OverAllState(Map.of(
				"conditional_workflow_input", "Run the selected branch",
				"first_answer", new AssistantMessage("Stale first-branch answer."),
				outputKeyToParent("conditional_workflow"), GraphResponse.done(Map.of(
						"input", "Run the selected branch",
						"deterministic_result", "Deterministic branch result.",
						"messages", List.<Message>of(new UserMessage("Run the selected branch"))))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(conditionalWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("Deterministic branch result.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Custom flows ending in deterministic nodes should expose their explicit wrapper output");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void opaqueCustomFlowRejectsAmbiguousWrapperOutputsWithoutExecutionEvidence() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		FlowAgent conditionalWorkflow = new StubConditionalFlowAgent("conditional_workflow",
				List.of(
						mockAgent("first_branch", "first_answer"),
						mockAgent("second_branch", "second_answer")));

		OverAllState state = new OverAllState(Map.of(
				"conditional_workflow_input", "Run the selected branch",
				"first_answer", new AssistantMessage("Stale first-branch answer."),
				"second_answer", new AssistantMessage("Current second-branch answer."),
				outputKeyToParent("conditional_workflow"), GraphResponse.done(Map.of(
						"first_answer", new AssistantMessage("Stale first-branch answer."),
						"second_answer", new AssistantMessage("Current second-branch answer."),
						"messages", List.<Message>of(
								new UserMessage("Previous request"),
								new AssistantMessage("Stale previous answer."),
								new UserMessage("Run the selected branch"))))
		));

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(conditionalWorkflow));
		Map<String, Object> result = node.apply(state);

		assertEquals("No results found from any knowledge source.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Ambiguous custom-flow state must not be guessed or concatenated across checkpointed branches");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void loopAgentUsesItsRepeatedChildOutput() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		LoopAgent loopAgent = LoopAgent.builder()
			.name("revision_loop")
			.description("Revises an answer")
			.loopStrategy(LoopMode.count(2))
			.subAgent(mockAgent("revision_agent", "revised_answer"))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"revision_loop_input", "Revise twice",
				"revised_answer", new AssistantMessage("Final revised answer."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(loopAgent));
		Map<String, Object> result = node.apply(state);

		assertEquals("Final revised answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY));
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void nestedRoutingAgentResultUsesItsMergedOutputBeforeChildOutputs() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent firstAgent = mockAgent("first_agent", "first_answer");
		BaseAgent secondAgent = mockAgent("second_agent", "second_answer");
		LlmRoutingAgent childRouter = LlmRoutingAgent.builder()
			.name("child_router")
			.description("Routes to child agents")
			.model(chatModel)
			.subAgents(List.of(firstAgent, secondAgent))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"child_router_input", "Route inside the child router",
				DEFAULT_MERGED_OUTPUT_KEY, "Child router synthesized answer.",
				"first_answer", new AssistantMessage("First child raw answer."),
				"second_answer", new AssistantMessage("Second child raw answer."))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(childRouter));
		Map<String, Object> result = node.apply(state);

		assertEquals("Child router synthesized answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Nested routing agents should expose their own merged output to the parent merge");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void routingMergedOutputIsNotDuplicatedForMultipleNestedRoutingAgents() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		LlmRoutingAgent firstRouter = LlmRoutingAgent.builder()
			.name("first_router")
			.description("First child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("first_agent", "first_answer")))
			.build();
		LlmRoutingAgent secondRouter = LlmRoutingAgent.builder()
			.name("second_router")
			.description("Second child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("second_agent", "second_answer")))
			.build();

		OverAllState state = new OverAllState(Map.of(
				"first_router_input", "Run the first router",
				"second_router_input", "Run the second router",
				DEFAULT_MERGED_OUTPUT_KEY, "Shared routing merge answer.")
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstRouter, secondRouter));
		Map<String, Object> result = node.apply(state);

		assertEquals("No results found from any knowledge source.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared routing merge output must not be duplicated across multiple routed child routers");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void multipleNestedRoutingAgentsUseNamespacedWrapperResults() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel("SYNTHESIZED ANSWER");

		LlmRoutingAgent firstRouter = LlmRoutingAgent.builder()
			.name("first_router")
			.description("First child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("first_agent", "first_answer")))
			.build();
		LlmRoutingAgent secondRouter = LlmRoutingAgent.builder()
			.name("second_router")
			.description("Second child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("second_agent", "second_answer")))
			.build();

		// Each routed subgraph has its own wrapper key, so the nested routing result can
		// be read from that wrapper map without attributing the shared parent merged_result
		// to every router.
		OverAllState state = new OverAllState(Map.of(
				"first_router_input", "Run the first router",
				"second_router_input", "Run the second router",
				DEFAULT_MERGED_OUTPUT_KEY, "Shared parent merge result that must not be used.",
				outputKeyToParent("first_router"), GraphResponse.done(Map.of(
						outputKeyToParent("first_router"), "First router raw fallback answer.",
						DEFAULT_MERGED_OUTPUT_KEY, "First router synthesized answer.",
						"first_answer", new AssistantMessage("First raw answer."))),
				outputKeyToParent("second_router"), GraphResponse.done(Map.of(
						outputKeyToParent("second_router"), "Second router raw fallback answer.",
						DEFAULT_MERGED_OUTPUT_KEY, "Second router synthesized answer.",
						"second_answer", new AssistantMessage("Second raw answer."))),
				"messages", List.<Message>of(new UserMessage("Run both routers")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstRouter, secondRouter));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Multiple nested routing results should be synthesized from namespaced wrapper outputs");
		assertEquals(1, chatModel.callCount());
		String promptContent = chatModel.lastPrompt().getContents();
		assertTrue(promptContent.contains("First router synthesized answer."));
		assertTrue(promptContent.contains("Second router synthesized answer."));
		assertFalse(promptContent.contains("Shared parent merge result that must not be used."));
		assertFalse(promptContent.contains("First router raw fallback answer."));
		assertFalse(promptContent.contains("Second router raw fallback answer."));
		assertFalse(promptContent.contains("First raw answer."));
		assertFalse(promptContent.contains("Second raw answer."));
	}

	@Test
	void parentRoutingSelectionIsNamespacedFromChildRoutingSelection() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent parentRouter = mockAgent("parent_router", null);
		LlmRoutingAgent childRouter = LlmRoutingAgent.builder()
			.name("child_router")
			.description("Child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("inner_agent", "inner_answer")))
			.build();

		// Subgraphs merge state back into the parent. The child router may update its own
		// route marker after the parent has selected child_router, so the parent merge must
		// read the marker namespaced by parent_router rather than the child's selection.
		OverAllState state = new OverAllState(Map.of(
				RoutingNode.routedAgentNamesKey("parent_router"), List.of("child_router"),
				RoutingNode.routedAgentNamesKey("child_router"), List.of("inner_agent"),
				RoutingNode.ROUTED_AGENT_NAMES_KEY, List.of("inner_agent"),
				"child_router_input", "Run child router",
				"inner_agent_input", "Run inner agent",
				outputKeyToParent("child_router"), GraphResponse.done(Map.of(
						outputKeyToParent("child_router"), "Child router raw fallback answer.",
						DEFAULT_MERGED_OUTPUT_KEY, "Child router synthesized answer.")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, parentRouter, List.of(childRouter));
		Map<String, Object> result = node.apply(state);

		assertEquals("Child router synthesized answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Parent routing merge should ignore child-router selection markers");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void sameNamedNestedRouterRestoresTheEnclosingRoutingMarker() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();
		BaseAgent innerAgent = mockAgent("inner_agent", "inner_answer");
		LlmRoutingAgent nestedRouter = LlmRoutingAgent.builder()
			.name("shared_router")
			.description("Nested router")
			.model(chatModel)
			.subAgents(List.of(innerAgent))
			.build();
		SequentialAgent outerWorkflow = SequentialAgent.builder()
			.name("outer_workflow")
			.description("Workflow ending in the same-named nested router")
			.subAgents(List.of(nestedRouter))
			.build();
		BaseAgent outerRouter = mockAgent("shared_router", null);

		Map<String, Object> outerMarker = Map.of(
				RoutingNode.SELECTED_AGENT_NAMES_FIELD, List.of("outer_workflow"));
		Map<String, Object> nestedMarker = Map.of(
				RoutingNode.SELECTED_AGENT_NAMES_FIELD, List.of("inner_agent"),
				RoutingNode.PARENT_ROUTING_MARKER_FIELD, outerMarker);
		String markerKey = RoutingNode.routedAgentNamesKey("shared_router");
		OverAllState nestedState = new OverAllState(Map.of(
				markerKey, nestedMarker,
				"inner_answer", new AssistantMessage("Nested router answer.")));

		RoutingMergeNode nestedMerge = new RoutingMergeNode(chatModel, nestedRouter, List.of(innerAgent));
		Map<String, Object> nestedResult = nestedMerge.apply(nestedState);

		assertEquals("Nested router answer.", nestedResult.get(DEFAULT_MERGED_OUTPUT_KEY));
		assertEquals(outerMarker, nestedResult.get(markerKey),
				"Nested merge must restore the enclosing same-named router marker");

		OverAllState outerState = new OverAllState(Map.of(
				markerKey, nestedResult.get(markerKey),
				outputKeyToParent("outer_workflow"), GraphResponse.done(Map.of(
						DEFAULT_MERGED_OUTPUT_KEY, "Nested router answer."))));
		RoutingMergeNode outerMerge = new RoutingMergeNode(chatModel, outerRouter, List.of(outerWorkflow));
		Map<String, Object> outerResult = outerMerge.apply(outerState);

		assertEquals("Nested router answer.", outerResult.get(DEFAULT_MERGED_OUTPUT_KEY));
		assertSame(OverAllState.MARK_FOR_REMOVAL, outerResult.get(markerKey),
				"Outermost merge must clear its routing marker after collection");
		assertEquals(0, chatModel.callCount());
	}

	@Test
	void parentRoutingMergeIgnoresGlobalMarkerWhenNamespacedMarkerIsAbsent() throws Exception {
		RecordingChatModel chatModel = new RecordingChatModel();

		BaseAgent parentRouter = mockAgent("parent_router", null);
		LlmRoutingAgent childRouter = LlmRoutingAgent.builder()
			.name("child_router")
			.description("Child router")
			.model(chatModel)
			.subAgents(List.of(mockAgent("inner_agent", "inner_answer")))
			.build();

		// The production merge node knows its parent router name. If an old or nested
		// un-namespaced marker is present without the parent marker, it must not be treated
		// as the parent router's current selection.
		OverAllState state = new OverAllState(Map.of(
				RoutingNode.ROUTED_AGENT_NAMES_KEY, List.of("inner_agent"),
				"child_router_input", "Run child router",
				"inner_agent_input", "Run inner agent",
				outputKeyToParent("child_router"), GraphResponse.done(Map.of(
						DEFAULT_MERGED_OUTPUT_KEY, "Child router synthesized answer.")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, parentRouter, List.of(childRouter));
		Map<String, Object> result = node.apply(state);

		assertEquals("Child router synthesized answer.", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Parent routing merge should fall back to top-level route inputs, not stale global markers");
		assertEquals(0, chatModel.callCount());
	}

	private static BaseAgent mockAgent(String name, String outputKey) {
		return new StubBaseAgent(name, outputKey);
	}

	private static final class StubBaseAgent extends BaseAgent {

		private StubBaseAgent(String name, String outputKey) {
			super(name, "Test agent", false, false, outputKey, null);
		}

		@Override
		public Node asNode(boolean includeContents, boolean returnReasoningContents) {
			throw new UnsupportedOperationException("RoutingMergeNodeTest only needs agent metadata");
		}

		@Override
		protected StateGraph initGraph() throws GraphStateException {
			throw new UnsupportedOperationException("RoutingMergeNodeTest only needs agent metadata");
		}

	}

	/**
	 * Metadata-only flow used to model a custom conditional workflow. Graph construction
	 * is outside the scope of merge-node unit tests.
	 */
	private static final class StubConditionalFlowAgent extends FlowAgent {

		/**
		 * Creates a conditional workflow with the supplied possible branch agents.
		 * @param name workflow name used for routing and wrapper keys
		 * @param subAgents child agents that may be selected by the workflow
		 */
		private StubConditionalFlowAgent(String name, List<Agent> subAgents) {
			super(name, "Test conditional workflow", null, subAgents);
		}

		@Override
		protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) {
			throw new UnsupportedOperationException("RoutingMergeNodeTest only needs agent metadata");
		}

	}

	private static final class RecordingChatModel implements ChatModel {

		private final String response;

		private final List<Prompt> prompts = new ArrayList<>();

		private RecordingChatModel() {
			this("UNUSED");
		}

		private RecordingChatModel(String response) {
			this.response = response;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private int callCount() {
			return prompts.size();
		}

		private Prompt lastPrompt() {
			return prompts.get(prompts.size() - 1);
		}

	}

}
