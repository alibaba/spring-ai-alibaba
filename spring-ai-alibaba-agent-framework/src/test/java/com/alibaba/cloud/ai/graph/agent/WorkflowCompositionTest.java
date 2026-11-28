/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder.FlowGraphConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_MODEL_NAME;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowCompositionTest {

	@Test
	void streamsAnAgentWithOnlyWorkflowInputAndLeavesBusinessStateForTheNextNode() throws Exception {
		var model = new RecordingModel(prompt -> "agent:" + prompt.getUserMessage().getText());
		var worker = ReactAgent.builder().name("worker").outputKey("answer").model(model).build();
		var workflow = new StateGraph(() -> Map.of("audit", new AppendStrategy()))
				.addNode("prepare", node_async(state -> Map.of("audit", "prepared")))
				.addNode("worker", worker.asNode(true, false))
				.addNode("consume", node_async(state -> Map.of("consumed", text(state, "answer"))))
				.addEdge(START, "prepare").addEdge("prepare", "worker").addEdge("worker", "consume")
				.addEdge("consume", END).compile();

		var events = workflow.stream(Map.of("input", "workflow question", "audit", List.of("before")))
				.collectList().block(Duration.ofSeconds(6));

		assertThat(model.prompts).hasSize(1);
		assertThat(model.prompts.get(0).getUserMessage().getText()).isEqualTo("workflow question");
		assertThat(finalState(events).value("audit").orElseThrow()).isEqualTo(List.of("before", "prepared"));
		assertThat(finalState(events).value("consumed").orElseThrow()).isEqualTo("agent:workflow question");
		assertThat(modelChunks(events)).containsExactly("agent", ":workflow question");
	}

	@Test
	void passesBusinessOutputsThroughNestedSequentialAgentsWithoutReappendingHistory() throws Exception {
		var producer = ReactAgent.builder().name("producer").model(new RecordingModel(prompt -> "document-42"))
				.outputKey("document").build();
		var consumer = new RecordingModel(prompt -> "approved");
		var reviewer = ReactAgent.builder().name("reviewer").model(consumer).instruction("Review {document}")
				.includeContents(false).outputKey("decision").build();
		var nested = SequentialAgent.builder().name("nested").subAgents(List.of(producer, reviewer)).build();
		var finalizer = ReactAgent.builder().name("finalizer").model(new RecordingModel(prompt -> "final"))
				.instruction("Decision {decision}").outputKey("summary").build();
		var root = SequentialAgent.builder().name("sequence").subAgents(List.of(nested, finalizer)).build();

		var result = root.invoke("private input").orElseThrow();

		assertThat(text(result, "document")).isEqualTo("document-42");
		assertThat(text(result, "decision")).isEqualTo("approved");
		assertThat(text(result, "summary")).isEqualTo("final");
		assertThat(consumer.prompts.get(0).getInstructions()).extracting(Message::getText)
				.contains("Review document-42").doesNotContain("private input", "document-42");
		assertThat(result.<List<Message>>value("messages").orElseThrow().stream().map(Message::getText))
				.containsExactly("private input", "document-42", "approved", "final");
	}

	@Test
	void repeatsAnAgentInAConditionalWorkflowWithTheLatestBusinessVariables() throws Exception {
		var model = new RecordingModel(prompt -> "round " + lastMessage(prompt));
		var agent = ReactAgent.builder().name("worker").model(model).instruction("Try {round}").outputKey("answer").build();
		var workflow = new StateGraph()
				.addNode("prepare", node_async(state -> Map.of("round", state.value("round", 0) + 1)))
				.addNode("worker", agent.asNode(false, false))
				.addEdge(START, "prepare").addEdge("prepare", "worker")
				.addConditionalEdges("worker", edge_async(state -> state.value("round", 0) < 2 ? "again" : "done"),
						Map.of("again", "prepare", "done", END))
				.compile();

		var events = workflow.stream(Map.of("input", "initial")).collectList().block(Duration.ofSeconds(6));

		assertThat(model.prompts.stream().map(WorkflowCompositionTest::lastMessage))
				.containsExactly("Try 1", "Try 2");
		assertThat(text(finalState(events), "answer")).isEqualTo("round Try 2");
		assertThat(modelChunks(events)).containsExactly("round", " Try 1", "round", " Try 2");
	}

	@Test
	void preservesParallelOutputsWithAppendReducersAndDeterministicAggregation() throws Exception {
		var left = ReactAgent.builder().name("left").outputKey("left_result")
				.outputKeyStrategy(new AppendStrategy()).model(new RecordingModel(prompt -> "left")).build();
		var right = ReactAgent.builder().name("right").outputKey("right_result")
				.model(new RecordingModel(prompt -> "right")).build();
		var parallel = ParallelAgent.builder().name("parallel").subAgents(List.of(left, right))
				.mergeOutputKey("combined").build();

		var events = parallel.stream("work").collectList().block(Duration.ofSeconds(6));
		var state = finalState(events);

		assertThat(state.<List<Message>>value("left_result").orElseThrow()).extracting(Message::getText)
				.containsExactly("left");
		assertThat(text(state, "right_result")).isEqualTo("right");
		assertThat(state.<Map<String, Object>>value("combined").orElseThrow()).containsKeys("left_result", "right_result");
		assertThat(modelChunks(events)).containsExactlyInAnyOrder("left", "right");
	}

	@Test
	void loopHonorsTheChildContextAndReasoningOptions() throws Exception {
		var model = new RecordingModel(prompt -> "iteration");
		var worker = ReactAgent.builder().name("worker").instruction("run an iteration")
				.includeContents(false).returnReasoningContents(false).model(model).outputKey("answer").build();
		var loop = LoopAgent.builder().name("loop").subAgent(worker).loopStrategy(LoopMode.count(2)).build();

		var events = loop.stream("private input").collectList().block(Duration.ofSeconds(6));

		assertThat(model.prompts).hasSize(2).allSatisfy(prompt ->
				assertThat(prompt.getInstructions()).extracting(Message::getText).containsExactly("run an iteration"));
		assertThat(finalState(events).<List<Message>>value("messages").orElseThrow().stream().map(Message::getText))
				.containsExactly("private input", "iteration", "iteration");
	}

	@Test
	void anArrayLoopMakesEachItemTheCurrentUserMessage() throws Exception {
		var model = new RecordingModel(prompt -> "handled " + prompt.getUserMessage().getText());
		var worker = ReactAgent.builder().name("worker").model(model).outputKey("answer")
				.outputKeyStrategy(new AppendStrategy()).build();
		var loop = LoopAgent.builder().name("items").subAgent(worker).loopStrategy(LoopMode.array()).build();

		var events = loop.stream("[\"first\", \"second\"]").collectList().block(Duration.ofSeconds(6));

		assertThat(model.prompts).extracting(prompt -> prompt.getUserMessage().getText()).containsExactly("first", "second");
		assertThat(finalState(events).<List<Message>>value("answer").orElseThrow()).extracting(Message::getText)
				.containsExactly("handled first", "handled second");
	}

	@Test
	void aConditionalLoopSeesTheLatestReplyFromANestedSequence() throws Exception {
		var attempts = new AtomicInteger();
		var draft = ReactAgent.builder().name("draft").model(new RecordingModel(prompt -> "draft " + attempts.incrementAndGet()))
				.outputKey("draft").build();
		var review = ReactAgent.builder().name("review").model(new RecordingModel(prompt -> attempts.get() == 1 ? "retry" : "ready"))
				.instruction("Review {draft}").outputKey("status").build();
		var sequence = SequentialAgent.builder().name("draft_review").subAgents(List.of(draft, review)).build();
		var loop = LoopAgent.builder().name("retry").subAgent(sequence).loopStrategy(LoopMode.condition(messages ->
				!messages.isEmpty() && "ready".equals(messages.get(messages.size() - 1).getText()))).build();

		var result = loop.invoke("produce a draft").orElseThrow();

		assertThat(attempts.get()).isEqualTo(2);
		assertThat(text(result, "draft")).isEqualTo("draft 2");
		assertThat(text(result, "status")).isEqualTo("ready");
		assertThat(result.<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
				.containsExactly("produce a draft", "draft 1", "retry", "draft 2", "ready");
	}

	@Test
	void aCompiledChildReturnsOnlyItsReducerInputsInExecutionOrder() throws Exception {
		KeyStrategy add = (current, update) -> (current == null ? 0 : (Integer) current) + (Integer) update;
		var child = new StateGraph(() -> Map.of("total", add))
				.addNode("two", node_async(state -> Map.of("total", 2)))
				.addNode("three", node_async(state -> Map.of("total", 3)))
				.addEdge(START, "two").addEdge("two", "three").addEdge("three", END).compile();
		var parent = new StateGraph(() -> Map.of("total", add))
				.addNode("child", child)
				.addEdge(START, "child").addEdge("child", END).compile();

		assertThat(parent.invoke(Map.of("total", 5)).orElseThrow().value("total").orElseThrow()).isEqualTo(10);
	}

	@Test
	void theNextWorkflowTurnOnlyInheritsTheParentHistoryOnce() throws Exception {
		var saver = MemorySaver.builder().build();
		var compile = CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build()).build();
		var model = new RecordingModel(prompt -> "received " + prompt.getUserMessage().getText());
		var worker = ReactAgent.builder().name("worker").model(model).compileConfig(compile).build();
		var workflow = new StateGraph().addNode("worker", worker.asNode(true, false))
				.addEdge(START, "worker").addEdge("worker", END).compile(compile);
		var run = RunnableConfig.builder().threadId("turns").build();
		workflow.stream(Map.of("messages", List.of(new UserMessage("one"))), run).blockLast(Duration.ofSeconds(6));

		var result = workflow.stream(Map.of("messages", List.of(new UserMessage("two"))), run)
				.blockLast(Duration.ofSeconds(6)).state();

		assertThat(model.prompts.get(1).getInstructions()).extracting(Message::getText)
				.containsExactly("one", "received one", "two");
		assertThat(result.<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
				.containsExactly("one", "received one", "two", "received two");
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void resumesAnAgentNodeFromDiskAndAppliesItsBusinessWritesOnce(boolean taskCheckpoints, @TempDir Path directory)
			throws Exception {
		var writes = new AtomicInteger();
		var model = new RecordingModel(prompt -> "finished");
		var run = RunnableConfig.builder().threadId("workflow").build();

		var first = interruptibleWorkflow(directory, taskCheckpoints, model, writes)
				.stream(Map.of("input", "request", "audit", List.of("before")), run).blockLast(Duration.ofSeconds(6));

		assertThat(first).isInstanceOf(InterruptionMetadata.class);
		assertThat(model.prompts).isEmpty();
		var result = interruptibleWorkflow(directory, taskCheckpoints, model, writes)
				.stream(Map.of(), run.withResume()).blockLast(Duration.ofSeconds(6)).state();

		assertThat(writes.get()).isEqualTo(1);
		assertThat(result.value("audit").orElseThrow()).isEqualTo(List.of("before", "worker started", "worker done", "after"));
		assertThat(text(result, "answer")).isEqualTo("finished");
		assertThat(model.prompts).hasSize(1);
	}

	private static CompiledGraph interruptibleWorkflow(Path directory, boolean taskCheckpoints,
			ChatModel model, AtomicInteger writes) throws Exception {
		var saver = FileSystemSaver.builder().targetFolder(directory).build();
		var compile = CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.taskCheckpoints(taskCheckpoints).build();
		var audit = new AgentHook() {
			@Override
			public String getName() {
				return "audit";
			}

			@Override
			public Map<String, KeyStrategy> getKeyStrategys() {
				return Map.of("audit", new AppendStrategy());
			}

			@Override
			public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
				writes.incrementAndGet();
				return CompletableFuture.completedFuture(Map.of("audit", "worker started"));
			}

			@Override
			public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
				return CompletableFuture.completedFuture(Map.of("audit", "worker done"));
			}
		};
		var worker = ReactAgent.builder().name("worker").model(model).outputKey("answer").hooks(audit)
				.compileConfig(CompileConfig.builder(compile).interruptBefore(AGENT_MODEL_NAME).build()).build();
		return new StateGraph(() -> Map.of("audit", new AppendStrategy()))
				.addNode("worker", worker.asNode(true, false))
				.addNode("finish", node_async(state -> Map.of("audit", "after")))
				.addEdge(START, "worker").addEdge("worker", "finish").addEdge("finish", END).compile(compile);
	}

	@Test
	void usesBusinessStateToRouteACustomFlowAgentInsideAWorkflow() throws Exception {
		var approvalModel = new RecordingModel(prompt -> "approved " + lastMessage(prompt));
		var rejectionModel = new RecordingModel(prompt -> "rejected");
		var approve = ReactAgent.builder().name("approve").model(approvalModel)
				.instruction("Order {orderId}").outputKey("decision").build();
		var reject = ReactAgent.builder().name("reject").model(rejectionModel).outputKey("decision").build();
		var decision = new FlowAgent("decision_flow", "Routes order decisions", null, List.of(approve, reject)) {
			@Override
			protected StateGraph buildSpecificGraph(FlowGraphConfig config) throws GraphStateException {
				return new StateGraph()
						.addNode("classify", node_async(state -> Map.of("approved", state.value("orderTotal", 0) < 100)))
						.addNode("approve", approve.asNode(false, false)).addNode("reject", reject.asNode(false, false))
						.addEdge(START, "classify").addEdge("approve", END).addEdge("reject", END)
						.addConditionalEdges("classify", edge_async(state -> state.value("approved", false) ? "yes" : "no"),
								Map.of("yes", "approve", "no", "reject"));
			}
		};
		var workflow = new StateGraph()
				.addNode("decision_flow", decision.asNode())
				.addNode("record", node_async(state -> Map.of("recorded", text(state, "decision"))))
				.addEdge(START, "decision_flow").addEdge("decision_flow", "record").addEdge("record", END).compile();

		var events = workflow.stream(Map.of("orderId", "42", "orderTotal", 50)).collectList().block(Duration.ofSeconds(6));

		assertThat(finalState(events).value("recorded").orElseThrow()).isEqualTo("approved Order 42");
		assertThat(modelChunks(events)).containsExactly("appro", "ved Order 42");
		assertThat(rejectionModel.prompts).isEmpty();
	}

	@Test
	void keepsTheNestedParallelAgentsConcurrencyLimitWhenUsedInAWorkflow() throws Exception {
		var active = new AtomicInteger();
		var peak = new AtomicInteger();
		ChatModel model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return RecordingModel.response("ready");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.defer(() -> {
					peak.accumulateAndGet(active.incrementAndGet(), Math::max);
					return Mono.delay(Duration.ofMillis(60))
							.map(ignored -> {
								active.decrementAndGet();
								return call(prompt);
							}).flux();
				});
			}
		};
		var left = ReactAgent.builder().name("left").outputKey("left_result").model(model).build();
		var right = ReactAgent.builder().name("right").outputKey("right_result").model(model).build();
		var parallel = ParallelAgent.builder().name("workers").maxConcurrency(1)
				.subAgents(List.of(left, right)).mergeOutputKey("combined").build();
		var workflow = new StateGraph()
				.addNode("workers", parallel.asNode())
				.addEdge(START, "workers").addEdge("workers", END).compile();

		var result = workflow.stream(Map.of("input", "execute")).blockLast(Duration.ofSeconds(6)).state();

		assertThat(peak.get()).isEqualTo(1);
		assertThat(result.<Map<String, Object>>value("combined").orElseThrow()).containsKeys("left_result", "right_result");
	}

	@Test
	void emitsRoutingSynthesisChunksWithTheMergeNodeAndStoresTheSingleFinalAnswer() throws Exception {
		var routerModel = new RecordingModel(prompt -> {
			if (prompt.getSystemMessage().getText().startsWith("Synthesize these search results")) {
				assertThat(prompt.getSystemMessage().getText()).contains("original question");
				assertThat(prompt.getUserMessage().getText()).contains("left answer", "right answer");
				assertThat(prompt.getUserMessage().getText()).doesNotContain("AssistantMessage");
				return "final answer";
			}
			return """
					{"agents":[{"agent":"left","query":"left query"},{"agent":"right","query":"right query"}]}
					""";
		});
		var left = ReactAgent.builder().name("left").description("Left source").model(new RecordingModel(p -> "left answer"))
				.instruction("{left_input}").outputKey("left_result").outputKeyStrategy(new AppendStrategy()).build();
		var right = ReactAgent.builder().name("right").description("Right source").model(new RecordingModel(p -> "right answer"))
				.instruction("{right_input}").outputKey("right_result").build();
		var router = LlmRoutingAgent.builder().name("routing").model(routerModel).subAgents(List.of(left, right)).build();

		var events = router.stream("original question").collectList().block(Duration.ofSeconds(6));

		assertThat(events.stream().filter(event -> "routing_merge".equals(event.node()))
				.filter(StreamingOutput.class::isInstance).map(StreamingOutput.class::cast)
				.map(event -> event.message().getText())).containsExactly("final", " answer");
		assertThat(finalState(events).value("merged_result").orElseThrow()).isEqualTo("final answer");
	}

	private static OverAllState finalState(List<NodeOutput> events) {
		return events.get(events.size() - 1).state();
	}

	private static String text(OverAllState state, String key) {
		return state.<Message>value(key).orElseThrow().getText();
	}

	private static String lastMessage(Prompt prompt) {
		return prompt.getInstructions().get(prompt.getInstructions().size() - 1).getText();
	}

	private static List<String> modelChunks(List<NodeOutput> events) {
		return events.stream().filter(StreamingOutput.class::isInstance).map(StreamingOutput.class::cast)
				.filter(output -> output.getOutputType() == OutputType.AGENT_MODEL_STREAMING)
				.map(output -> output.message().getText()).toList();
	}

	private static final class RecordingModel implements ChatModel {

		private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
		private final Function<Prompt, String> answer;

		private RecordingModel(Function<Prompt, String> answer) {
			this.answer = answer;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			return response(answer.apply(prompt));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			prompts.add(prompt);
			String text = answer.apply(prompt);
			return text.length() <= 5 ? Flux.just(response(text))
					: Flux.just(response(text.substring(0, 5)), response(text.substring(5)));
		}

		private static ChatResponse response(String text) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
		}

	}

}
