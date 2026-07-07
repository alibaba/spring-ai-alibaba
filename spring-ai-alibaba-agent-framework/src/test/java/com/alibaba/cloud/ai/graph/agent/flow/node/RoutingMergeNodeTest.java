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
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	/**
	 * When multiple sub-agents produced results, the merge node should synthesize them
	 * through the LLM and return the synthesized answer.
	 */
	@Test
	void multipleResultsAreSynthesizedViaLlm() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
		verify(chatModel, times(1)).call(any(Prompt.class));
	}

	@Test
	void explicitListOutputPreservesAllEntries() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void flowAgentResultIsResolvedFromNestedFinalOutputKey() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void flowAgentResultFallsBackToMessagesForNestedAgentWithoutOutputKey() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void messagesFallbackDoesNotCollectUnselectedAgents() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void currentRouteSelectionIgnoresStaleCheckpointInputMarkers() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void unroutedFlowAgentsAreNotResolvedFromSharedExplicitOutputKeys() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void sharedExplicitOutputKeyIsCollectedOnlyOnceForMultipleRoutedAgents() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void multipleRoutedWorkflowsPreferWrapperOverSharedChildOutputKey() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(1)).call(promptCaptor.capture());
		String promptContent = promptCaptor.getValue().getContents();
		assertTrue(promptContent.contains("First workflow wrapper answer."));
		assertTrue(promptContent.contains("Second workflow wrapper answer."));
		assertFalse(promptContent.contains("Shared child value that must not be attributed."));
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferMergeOutputKeyOverWrapper() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(1)).call(promptCaptor.capture());
		String promptContent = promptCaptor.getValue().getContents();
		assertTrue(promptContent.contains("First merged answer."));
		assertTrue(promptContent.contains("Second merged answer."));
		assertFalse(promptContent.contains("First wrapper answer that must not be used."));
		assertFalse(promptContent.contains("Second wrapper answer that must not be used."));
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferWrapperWhenMergeOutputKeyIsShared() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
				outputKeyToParent("first_parallel"), new AssistantMessage("First parallel wrapper answer."),
				outputKeyToParent("second_parallel"), new AssistantMessage("Second parallel wrapper answer."),
				"messages", List.<Message>of(new UserMessage("Run both parallel workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstParallel, secondParallel));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared mergeOutputKey values should be resolved through workflow wrappers");
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(1)).call(promptCaptor.capture());
		String promptContent = promptCaptor.getValue().getContents();
		assertTrue(promptContent.contains("First parallel wrapper answer."));
		assertTrue(promptContent.contains("Second parallel wrapper answer."));
		assertFalse(promptContent.contains("Shared merge value that must not be attributed."));
	}

	@Test
	void multipleRoutedParallelWorkflowsPreferWrapperWhenChildOutputKeysAreShared() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
				outputKeyToParent("first_parallel"), new AssistantMessage("First parallel wrapper answer."),
				outputKeyToParent("second_parallel"), new AssistantMessage("Second parallel wrapper answer."),
				"messages", List.<Message>of(new UserMessage("Run both parallel workflows")))
		);

		RoutingMergeNode node = new RoutingMergeNode(chatModel, List.of(firstParallel, secondParallel));
		Map<String, Object> result = node.apply(state);

		assertEquals("SYNTHESIZED ANSWER", result.get(DEFAULT_MERGED_OUTPUT_KEY),
				"Shared child output keys should be resolved through workflow wrappers");
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(1)).call(promptCaptor.capture());
		String promptContent = promptCaptor.getValue().getContents();
		assertTrue(promptContent.contains("First parallel wrapper answer."));
		assertTrue(promptContent.contains("Second parallel wrapper answer."));
		assertFalse(promptContent.contains("Shared search value that must not be attributed."));
		assertFalse(promptContent.contains("Shared summary value that must not be attributed."));
	}

	@Test
	void parallelAgentWithoutMergeOutputKeyCollectsAllChildOutputs() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void sequentialAgentWithFinalParallelAgentCollectsAllChildOutputs() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void messagesFallbackIsNotUsedForMultipleRoutedAgents() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void messagesFallbackIgnoresUserOnlyHistory() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void nestedRoutingAgentResultUsesItsMergedOutputBeforeChildOutputs() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void routingMergedOutputIsNotDuplicatedForMultipleNestedRoutingAgents() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void multipleNestedRoutingAgentsUseNamespacedWrapperResults() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(Prompt.class)))
				.thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("SYNTHESIZED ANSWER")))));

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
		ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel, times(1)).call(promptCaptor.capture());
		String promptContent = promptCaptor.getValue().getContents();
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
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	@Test
	void parentRoutingMergeIgnoresGlobalMarkerWhenNamespacedMarkerIsAbsent() throws Exception {
		ChatModel chatModel = mock(ChatModel.class);

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
		verify(chatModel, never()).call(any(Prompt.class));
	}

	private static BaseAgent mockAgent(String name, String outputKey) {
		BaseAgent agent = mock(BaseAgent.class);
		when(agent.name()).thenReturn(name);
		when(agent.getOutputKey()).thenReturn(outputKey);
		return agent;
	}

}
