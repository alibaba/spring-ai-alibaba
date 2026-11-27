/*
 * Copyright 2025-2026 the original author or authors.
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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.context.CheckpointContextCompaction;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckpointContextCompactionTest {

	private static final String AGENT = "worker";

	private static final String STATE_KEY = CheckpointContextCompaction.stateKey(AGENT);

	private static final String EARLY = "First long requirement. ".repeat(80);

	private static final String LATER = "Later long requirement. ".repeat(80);

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@TempDir
	Path directory;

	@Test
	void convertsAnOldCheckpointThenRestoresAndCompactsAgainWithoutChangingHistory() throws Exception {
		var config = RunnableConfig.builder().threadId("thread").build();
		var seed = ReactAgent.builder().name(AGENT).model(prompt -> answer("old answer")).saver(saver()).build();
		seed.call(List.of(new SystemMessage("original instructions"), new UserMessage(EARLY)), config);
		assertThat(seed.getAndCompileGraph().getState(config).state().value(STATE_KEY)).isEmpty();

		var mainRequests = new CopyOnWriteArrayList<Prompt>();
		var summaryRequests = new CopyOnWriteArrayList<Prompt>();
		ChatModel mainModel = prompt -> {
			mainRequests.add(prompt);
			return answer("done");
		};
		var policy = CheckpointContextCompaction.builder(prompt -> {
			summaryRequests.add(prompt);
			return answer("summary version " + summaryRequests.size());
		}, 140).keepRecentTurns(1).build();

		var first = agent(mainModel, policy);
		assertThat(first.call("start next turn", config).getText()).isEqualTo("done");
		assertThat(texts(mainRequests.get(0))).contains("original instructions", "start next turn")
			.anyMatch(text -> text.contains("summary version 1")).doesNotContain(EARLY);
		var firstState = first.getAndCompileGraph().getState(config).state();
		assertThat(firstState.value(STATE_KEY)).isPresent();
		assertThat(firstState.<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
			.contains("original instructions", EARLY, "old answer", "start next turn", "done");

		var restarted = agent(mainModel, policy);
		restarted.call("short follow-up", config);
		assertThat(summaryRequests).hasSize(1);
		assertThat(texts(mainRequests.get(1))).anyMatch(text -> text.contains("summary version 1"))
			.contains("short follow-up").doesNotContain(EARLY);

		restarted.call(List.of(new UserMessage(LATER), new AssistantMessage("later answer"),
				new UserMessage("new task")), config);
		assertThat(summaryRequests).hasSize(2);
		assertThat(texts(summaryRequests.get(1))).anyMatch(text -> text.contains("summary version 1") && text.contains(LATER))
			.noneMatch(text -> text.contains(EARLY));
		assertThat(texts(mainRequests.get(2))).anyMatch(text -> text.contains("summary version 2"))
			.contains("new task").doesNotContain(EARLY, LATER);

		var finalState = restarted.getAndCompileGraph().getState(config).state();
		assertThat(finalState.<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
			.contains(EARLY, LATER, "short follow-up", "new task")
			.noneMatch(text -> text != null && text.startsWith("Summary of earlier conversation"));
	}

	@Test
	void preservesToolPairingAndResumesApprovalAfterTheProcessRestarts() throws Exception {
		var toolCalls = new AtomicInteger();
		var agentCalls = new AtomicInteger();
		var summaryCalls = new AtomicInteger();
		var tool = FunctionToolCallback.builder("approve_tool", (Map<String, Object> input) -> {
			toolCalls.incrementAndGet();
			return "tool done";
		}).description("Requires approval").inputType(Map.class)
			.toolMetadata(ToolMetadata.builder().requiresConfirmation(true).build()).build();
		var policy = CheckpointContextCompaction.builder(prompt -> {
			summaryCalls.incrementAndGet();
			return answer("earlier decisions");
		}, 140).keepRecentTurns(1).build();
		var model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				agentCalls.incrementAndGet();
				assertThat(texts(prompt)).anyMatch(text -> text != null && text.contains("earlier decisions"))
					.doesNotContain(EARLY);
				var toolResult = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast).findFirst();
				if (toolResult.isPresent()) {
					assertThat(toolResult.get().getResults()).singleElement().satisfies(result ->
						assertThat(result.id()).isEqualTo("approval-call"));
					return answer("done");
				}
				var call = new AssistantMessage.ToolCall("approval-call", "function", "approve_tool", "{}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.just(call(prompt));
			}
		};

		var config = RunnableConfig.builder().threadId("approval").build();
		var beforeRestart = ReactAgent.builder().name(AGENT).model(model).saver(saver()).tools(tool)
			.checkpointContextCompaction(policy).build();
		var pause = beforeRestart.stream(history(), config).blockLast(TIMEOUT);
		assertThat(pause).isInstanceOf(InterruptionMetadata.class);
		assertThat(beforeRestart.getAndCompileGraph().getState(config).state().value(STATE_KEY)).isPresent();
		assertThat(toolCalls).hasValue(0);
		var interruption = (InterruptionMetadata) pause;
		var feedback = InterruptionMetadata.builder(interruption).addToolFeedback(
			ToolFeedback.builder(interruption.toolFeedbacks().get(0)).result(FeedbackResult.APPROVED).build()).build();
		var resumed = RunnableConfig.builder(config).addHumanFeedback(feedback).build();

		var afterRestart = ReactAgent.builder().name(AGENT).model(model).saver(saver()).tools(tool)
			.checkpointContextCompaction(policy).build();
		var state = afterRestart.stream(Map.of(), resumed).blockLast(TIMEOUT).state();
		assertThat(state.<List<Message>>value("messages").orElseThrow()).extracting(Message::getText)
			.contains(EARLY, "done");
		assertThat(summaryCalls).hasValue(1);
		assertThat(toolCalls).hasValue(1);
		assertThat(agentCalls).hasValue(2);
	}

	@Test
	void keepsThePriorSummaryWhenGenerationFailsAndIgnoresAStaleBoundary() {
		var failure = new AtomicBoolean();
		var policy = CheckpointContextCompaction.builder(prompt -> {
			if (failure.get()) {
				throw new IllegalStateException("summary unavailable");
			}
			return answer("prior compacted context");
		}, 140).keepRecentTurns(1).build();
		var first = policy.prepare(history(), null);
		assertThat(first.checkpointUpdate()).isNotEmpty();
		failure.set(true);

		var added = new ArrayList<>(history());
		added.add(new AssistantMessage("follow-up answer"));
		added.add(new UserMessage(LATER));
		added.add(new AssistantMessage("later answer"));
		added.add(new UserMessage("next question"));
		var fallback = policy.prepare(added, first.checkpointUpdate());
		assertThat(fallback.checkpointUpdate()).isEmpty();
		assertThat(fallback.messages()).extracting(Message::getText)
			.anyMatch(text -> text.contains("prior compacted context")).contains(LATER).doesNotContain(EARLY);

		added.set(0, new UserMessage("edited old requirement"));
		var stale = policy.prepare(added, first.checkpointUpdate());
		assertThat(stale.checkpointUpdate()).isEmpty();
		assertThat(stale.messages()).isEqualTo(added);
	}

	@Test
	void keepsIncompleteToolExchangesAndChecksTheSummarizerInputBudget() {
		var main = CheckpointContextCompaction.builder(prompt -> {
			throw new AssertionError("The incomplete turn must not be summarized");
		}, 20).keepRecentTurns(1).build();
		var call = new AssistantMessage.ToolCall("pending", "function", "produce", "{}");
		List<Message> history = List.of(new UserMessage(EARLY),
				AssistantMessage.builder().toolCalls(List.of(call)).build(), new UserMessage("next question"));
		assertThat(main.prepare(history, null).messages()).isEqualTo(history);

		var bounded = CheckpointContextCompaction.builder(prompt -> {
			throw new AssertionError("The summary prompt is above its input budget");
		}, 20).keepRecentTurns(1).maxSummaryInputTokens(10).build();
		assertThat(bounded.prepare(history(), null).checkpointUpdate()).isEmpty();
	}

	@Test
	void letsACompactTextConversationPassTheModelInputBudget() throws Exception {
		var compaction = CheckpointContextCompaction.builder(prompt -> answer("earlier work"), 140)
			.keepRecentTurns(1).build();
		var policy = ContextWindowPolicy.builder(160).build();
		var agent = ReactAgent.builder().name(AGENT).model(prompt -> answer("done"))
			.contextWindowPolicy(policy).checkpointContextCompaction(compaction).build();

		assertThat(agent.call(history()).getText()).isEqualTo("done");
	}

	@Test
	void doesNotCommitANewRecordWhenTheModelStreamFails() throws Exception {
		var config = RunnableConfig.builder().threadId("failure").build();
		var model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new AssertionError("Expected a stream");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.error(new IllegalStateException("model unavailable"));
			}
		};
		var policy = CheckpointContextCompaction.builder(prompt -> answer("new summary"), 140)
			.keepRecentTurns(1).build();
		var agent = agent(model, policy);
		assertThatThrownBy(() -> agent.stream(history(), config).blockLast(TIMEOUT)).hasMessageContaining("model unavailable");
		assertThat(agent.getAndCompileGraph().getState(config).state().value(STATE_KEY)).isEmpty();
	}

	private ReactAgent agent(ChatModel model, CheckpointContextCompaction compaction) {
		return ReactAgent.builder().name(AGENT).model(model).saver(saver()).checkpointContextCompaction(compaction).build();
	}

	private FileSystemSaver saver() {
		return FileSystemSaver.builder().targetFolder(this.directory).build();
	}

	private static List<Message> history() {
		return List.of(new UserMessage(EARLY), new AssistantMessage("previous answer"), new UserMessage("next question"));
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static List<String> texts(Prompt prompt) {
		return prompt.getInstructions().stream().map(Message::getText).toList();
	}

}
