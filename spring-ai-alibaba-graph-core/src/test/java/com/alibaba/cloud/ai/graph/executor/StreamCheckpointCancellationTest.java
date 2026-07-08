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

package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for external stream cancellation while a checkpoint saver is
 * enabled. A cancelled SSE/client subscription must not leave the persisted message
 * history ending with a tool response.
 */
class StreamCheckpointCancellationTest {

	private static final String THREAD_ID = "stream-cancel-checkpoint-thread";

	@Test
	void streamCancellationShouldStillPersistFinalAssistantMessage() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StreamGraphFixture fixture = buildStreamGraphFixture(saver);
		CompiledGraph compiledGraph = fixture.compiledGraph();
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		List<NodeOutput> observed = compiledGraph.stream(initialMessages(), config)
			.takeUntil(output -> "streaming_model".equals(output.node()))
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(observed, "stream should emit before cancellation");
		assertTrue(observed.stream().anyMatch(output -> "streaming_model".equals(output.node())),
				"test should cancel after the first streaming chunk");

		Object lastMessage = awaitLastCheckpointMessage(compiledGraph, config);

		fixture.assertModelStreamCompletedWithoutCancellation("downstream cancellation");
		assertTrue(fixture.streamingNodeAfterCalled().get(),
				"streaming node after-listener should run after stream completion");
		assertInstanceOf(AssistantMessage.class, lastMessage,
				"checkpoint history must not end with ToolResponseMessage after stream cancellation");
		assertEquals("tool result received", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void streamTimeoutShouldStillPersistFinalAssistantMessage() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StreamGraphFixture fixture = buildStreamGraphFixture(saver);
		CompiledGraph compiledGraph = fixture.compiledGraph();
		RunnableConfig config = RunnableConfig.builder().threadId("stream-timeout-checkpoint-thread").build();
		List<NodeOutput> observed = new ArrayList<>();

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> timeoutAfterStreamingModelStarts(compiledGraph.stream(initialMessages(), config)
					.doOnNext(observed::add))
					.collectList()
					.block(Duration.ofSeconds(5)));

		assertTrue(hasCause(error, TimeoutException.class), "test should fail through Flux timeout");
		assertTrue(observed.stream().anyMatch(output -> "streaming_model".equals(output.node())),
				"test should time out after the streaming model starts");

		Object lastMessage = awaitLastCheckpointMessage(compiledGraph, config);

		fixture.assertModelStreamCompletedWithoutCancellation("timeout cancellation");
		assertTrue(fixture.streamingNodeAfterCalled().get(), "streaming node after-listener should run after timeout");
		assertInstanceOf(AssistantMessage.class, lastMessage,
				"checkpoint history must not end with ToolResponseMessage after stream timeout");
		assertEquals("tool result received", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void nextTurnAfterStreamTimeoutShouldInvokeModelWithOpenAiCompatibleMessages() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph timeoutGraph = buildStreamGraphFixture(saver).compiledGraph();
		RunnableConfig config = RunnableConfig.builder().threadId("timeout-next-turn-openai-payload-thread").build();

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> timeoutAfterStreamingModelStarts(timeoutGraph.stream(initialMessages(), config))
					.collectList()
					.block(Duration.ofSeconds(5)));
		assertTrue(hasCause(error, TimeoutException.class), "test should first reproduce timeout cancellation");
		assertTrue(awaitLastCheckpointMessage(timeoutGraph, config) instanceof AssistantMessage,
				"timeout cancellation should still persist the final assistant message");

		AtomicBoolean openAiPayloadVerified = new AtomicBoolean(false);
		CompiledGraph continuationGraph = buildContinuationGraph(saver, openAiPayloadVerified,
				List.of("user", "assistant", "tool", "assistant", "user"));

		List<NodeOutput> outputs = continuationGraph
			.stream(Map.of("messages", List.of(new UserMessage("\u8bf7\u7ee7\u7eed"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(outputs);
		assertTrue(openAiPayloadVerified.get(), "next turn after timeout should send OpenAI-compatible messages");
	}

	@Test
	void staleBackgroundCheckpointShouldNotOverrideNextTurnCheckpoint() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StreamGraphFixture timeoutFixture = buildStreamGraphFixture(saver, Duration.ofMillis(300));
		RunnableConfig firstTurnConfig = RunnableConfig.builder().threadId("stale-background-checkpoint-thread").build();

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> timeoutAfterStreamingModelStarts(timeoutFixture.compiledGraph().stream(initialMessages(),
						firstTurnConfig))
					.collectList()
					.block(Duration.ofSeconds(5)));
		assertTrue(hasCause(error, TimeoutException.class), "test should first reproduce timeout cancellation");

		AtomicBoolean openAiPayloadVerified = new AtomicBoolean(false);
		CompiledGraph continuationGraph = buildContinuationGraph(saver, openAiPayloadVerified, List.of("user"));
		RunnableConfig nextTurnConfig = RunnableConfig.builder().threadId("stale-background-checkpoint-thread").build();

		List<NodeOutput> outputs = continuationGraph
			.stream(Map.of("messages", List.of(new UserMessage("\u8bf7\u7ee7\u7eed"))), nextTurnConfig)
			.collectList()
			.block(Duration.ofSeconds(5));
		assertNotNull(outputs);
		assertTrue(openAiPayloadVerified.get(), "next turn should run before the old background stream completes");

		assertTrue(awaitUntil(timeoutFixture.modelStreamCompleted()::get),
				"old background stream should complete after the next turn");
		Object lastMessage = awaitLastCheckpointMessage(continuationGraph, nextTurnConfig);

		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText(),
				"stale background checkpoint must not become the latest checkpoint after a newer turn");
	}

	@Test
	void staleBackgroundCheckpointBeforeFirstWriteShouldNotOverrideNextTurnCheckpoint() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CountDownLatch oldRunAtStart = new CountDownLatch(1);
		CountDownLatch releaseOldRun = new CountDownLatch(1);
		RunnableConfig config = RunnableConfig.builder().threadId("stale-background-first-write-thread").build();
		CompiledGraph oldGraph = buildAnswerGraph(saver, "old background", new GraphLifecycleListener() {
			@Override
			public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
				oldRunAtStart.countDown();
				try {
					if (!releaseOldRun.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("old run was not released");
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(ex);
				}
			}
		});

		CompletableFuture<List<NodeOutput>> oldRun = CompletableFuture.supplyAsync(() -> oldGraph
			.stream(Map.of("messages", List.of(new UserMessage("old request"))), config)
			.collectList()
			.block(Duration.ofSeconds(5)));
		assertTrue(oldRunAtStart.await(2, TimeUnit.SECONDS), "old run should stop before its START checkpoint");

		CompiledGraph nextGraph = buildAnswerGraph(saver, "continued", null);
		List<NodeOutput> outputs = nextGraph
			.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));
		assertNotNull(outputs);

		releaseOldRun.countDown();
		oldRun.get(5, TimeUnit.SECONDS);
		Object lastMessage = awaitLastCheckpointMessage(nextGraph, config);

		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText(),
				"old background run must not write its first checkpoint after a newer turn has advanced the thread");
	}

	@Test
	void checkpointLineageCheckAndAppendShouldBeAtomic() throws Exception {
		BlockingSaver saver = new BlockingSaver(true, false);
		RunnableConfig config = RunnableConfig.builder().threadId("checkpoint-lineage-atomic-thread").build();
		CompiledGraph oldGraph = buildAnswerGraph(saver, "old background", false, null);

		CompletableFuture<List<NodeOutput>> oldRun = CompletableFuture.supplyAsync(() -> oldGraph
			.stream(Map.of("messages", List.of(new UserMessage("old request"))), config)
			.collectList()
			.block(Duration.ofSeconds(5)));
		assertTrue(saver.awaitFirstAppendReached(), "old run should pause after passing the lineage check");

		CompiledGraph nextGraph = buildAnswerGraph(saver, "continued", false, null);
		CountDownLatch nextRunCompleted = new CountDownLatch(1);
		CompletableFuture<List<NodeOutput>> nextRun = CompletableFuture.supplyAsync(() -> {
			try {
				return nextGraph.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
					.collectList()
					.block(Duration.ofSeconds(5));
			}
			finally {
				nextRunCompleted.countDown();
			}
		});
		assertFalse(nextRunCompleted.await(200, TimeUnit.MILLISECONDS),
				"another run on the same saver must not complete while the first append is inside its critical section");

		saver.releaseFirstAppend();
		assertNotNull(oldRun.get(5, TimeUnit.SECONDS));
		assertNotNull(nextRun.get(5, TimeUnit.SECONDS));
	}

	@Test
	void checkpointLineageCheckAndReleaseShouldBeAtomic() throws Exception {
		BlockingSaver saver = new BlockingSaver(false, true);
		RunnableConfig config = RunnableConfig.builder().threadId("checkpoint-release-atomic-thread").build();
		CompiledGraph oldGraph = buildAnswerGraph(saver, "old background", true, null);

		CompletableFuture<List<NodeOutput>> oldRun = CompletableFuture.supplyAsync(() -> oldGraph
			.stream(Map.of("messages", List.of(new UserMessage("old request"))), config)
			.collectList()
			.block(Duration.ofSeconds(5)));
		assertTrue(saver.awaitFirstReleaseReached(), "old run should pause inside release after passing the lineage check");

		CompiledGraph nextGraph = buildAnswerGraph(saver, "continued", false, null);
		CountDownLatch nextRunCompleted = new CountDownLatch(1);
		CompletableFuture<List<NodeOutput>> nextRun = CompletableFuture.supplyAsync(() -> {
			try {
				return nextGraph.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
					.collectList()
					.block(Duration.ofSeconds(5));
			}
			finally {
				nextRunCompleted.countDown();
			}
		});
		assertFalse(nextRunCompleted.await(200, TimeUnit.MILLISECONDS),
				"another run on the same saver must not complete while release is inside its critical section");

		saver.releaseFirstRelease();
		assertNotNull(oldRun.get(5, TimeUnit.SECONDS));
		assertNotNull(nextRun.get(5, TimeUnit.SECONDS));

		Object lastMessage = awaitLastCheckpointMessage(nextGraph, config);
		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void staleResumedCheckpointBeforeFirstWriteShouldNotOverrideNextTurnCheckpoint() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		RunnableConfig config = RunnableConfig.builder().threadId("stale-resume-first-write-thread").build();
		Checkpoint resumeCheckpoint = Checkpoint.builder()
			.nodeId(START)
			.nextNodeId("streaming_model")
			.state(stableState())
			.build();
		saver.put(config, resumeCheckpoint);

		CountDownLatch oldRunBeforeNode = new CountDownLatch(1);
		CountDownLatch releaseOldRun = new CountDownLatch(1);
		CompiledGraph oldGraph = buildAnswerGraph(saver, "old resumed", false, new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
				oldRunBeforeNode.countDown();
				try {
					if (!releaseOldRun.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("old resumed run was not released");
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(ex);
				}
			}
		});
		RunnableConfig resumeConfig = RunnableConfig.builder(config)
			.checkPointId(resumeCheckpoint.getId())
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "continue")
			.build();

		CompletableFuture<List<NodeOutput>> oldRun = CompletableFuture.supplyAsync(() -> oldGraph
			.stream(Map.of("messages", List.of(new UserMessage("old resume"))), resumeConfig)
			.collectList()
			.block(Duration.ofSeconds(5)));
		assertTrue(oldRunBeforeNode.await(2, TimeUnit.SECONDS), "old resumed run should stop before its first checkpoint");

		CompiledGraph nextGraph = buildAnswerGraph(saver, "continued", false, null);
		List<NodeOutput> outputs = nextGraph
			.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));
		assertNotNull(outputs);

		releaseOldRun.countDown();
		oldRun.get(5, TimeUnit.SECONDS);
		Object lastMessage = awaitLastCheckpointMessage(nextGraph, config);

		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText(),
				"old resumed run must not write its first checkpoint after a newer turn has advanced the thread");
	}

	@Test
	void staleBackgroundCompletionShouldNotReleaseNewerTurnCheckpoints() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CountDownLatch oldRunAtStart = new CountDownLatch(1);
		CountDownLatch releaseOldRun = new CountDownLatch(1);
		RunnableConfig config = RunnableConfig.builder().threadId("stale-background-release-thread").build();
		CompiledGraph oldGraph = buildAnswerGraph(saver, "old background", true, new GraphLifecycleListener() {
			@Override
			public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
				oldRunAtStart.countDown();
				try {
					if (!releaseOldRun.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("old run was not released");
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(ex);
				}
			}
		});

		CompletableFuture<List<NodeOutput>> oldRun = CompletableFuture.supplyAsync(() -> oldGraph
			.stream(Map.of("messages", List.of(new UserMessage("old request"))), config)
			.collectList()
			.block(Duration.ofSeconds(5)));
		assertTrue(oldRunAtStart.await(2, TimeUnit.SECONDS), "old run should stop before its START checkpoint");

		CompiledGraph nextGraph = buildAnswerGraph(saver, "continued", false, null);
		List<NodeOutput> outputs = nextGraph
			.stream(Map.of("messages", List.of(new UserMessage("please continue"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));
		assertNotNull(outputs);

		releaseOldRun.countDown();
		oldRun.get(5, TimeUnit.SECONDS);
		List<?> checkpointMessages = checkpointMessages(nextGraph, config);

		assertFalse(checkpointMessages.isEmpty(), "newer turn checkpoints must not be released by an older run");
		Object lastMessage = checkpointMessages.get(checkpointMessages.size() - 1);
		assertInstanceOf(AssistantMessage.class, lastMessage);
		assertEquals("continued", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void streamCancellationAfterToolBeforeModelShouldStillPersistFinalAssistantMessage() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		StreamGraphFixture fixture = buildStreamGraphFixture(saver);
		CompiledGraph compiledGraph = fixture.compiledGraph();
		RunnableConfig config = RunnableConfig.builder().threadId("stream-cancel-after-tool-thread").build();
		saver.put(config, Checkpoint.builder().nodeId("stable").nextNodeId(END).state(stableState()).build());

		List<NodeOutput> observed = compiledGraph.stream(initialMessages(), config)
			.takeUntil(output -> "tool_node".equals(output.node()))
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(observed, "stream should emit the tool node output before cancellation");
		assertTrue(observed.stream().anyMatch(output -> "tool_node".equals(output.node())),
				"test should cancel before the streaming model node is subscribed");

		Object lastMessage = awaitLastCheckpointMessage(compiledGraph, config);

		fixture.assertModelStreamCompletedWithoutCancellation("downstream cancellation");
		assertInstanceOf(AssistantMessage.class, lastMessage,
				"background execution should keep tool response and final assistant paired");
		assertEquals("tool result received", ((AssistantMessage) lastMessage).getText());
	}

	@Test
	void streamFailureBeforeAssistantChunkShouldNotBeLoadedAsNextTurnHistory() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		CompiledGraph compiledGraph = buildFailingGraph(saver);
		RunnableConfig config = RunnableConfig.builder().threadId("stream-failure-checkpoint-thread").build();

		assertThrows(IllegalStateException.class,
				() -> compiledGraph.stream(initialMessages(), config).collectList().block(Duration.ofSeconds(5)));

		List<?> latestCheckpointMessages = checkpointMessages(compiledGraph, config);
		assertInstanceOf(ToolResponseMessage.class, latestCheckpointMessages.get(latestCheckpointMessages.size() - 1),
				"the latest raw checkpoint reproduces the incomplete tool response state");

		@SuppressWarnings("unchecked")
		List<Message> nextTurnMessages = (List<Message>) compiledGraph
			.getInitialState(Map.of("messages", List.of(new UserMessage("continue"))), config)
			.get("messages");

		assertFalse(nextTurnMessages.stream().anyMatch(ToolResponseMessage.class::isInstance),
				"next user input must not be appended after an incomplete tool response checkpoint");
		assertInstanceOf(UserMessage.class, nextTurnMessages.get(nextTurnMessages.size() - 1));
	}

	@Test
	void nextTurnShouldInvokeModelWithOpenAiCompatibleMessagesAfterIncompleteCheckpoint() throws Exception {
		MemorySaver saver = MemorySaver.builder().build();
		AtomicBoolean openAiPayloadVerified = new AtomicBoolean(false);
		CompiledGraph compiledGraph = buildContinuationGraph(saver, openAiPayloadVerified,
				List.of("user", "assistant", "user"));
		RunnableConfig config = RunnableConfig.builder().threadId("next-turn-openai-payload-thread").build();

		saver.put(config, Checkpoint.builder().nodeId("stable").nextNodeId(END).state(stableState()).build());
		saver.put(config, Checkpoint.builder()
			.nodeId("tool_node")
			.nextNodeId("streaming_model")
			.state(incompleteToolResponseState())
			.build());

		List<NodeOutput> outputs = compiledGraph
			.stream(Map.of("messages", List.of(new UserMessage("\u8bf7\u7ee7\u7eed"))), config)
			.collectList()
			.block(Duration.ofSeconds(5));

		assertNotNull(outputs);
		assertTrue(openAiPayloadVerified.get(), "the next model call should receive an OpenAI-compatible payload");
	}

	private static StreamGraphFixture buildStreamGraphFixture(MemorySaver saver) throws Exception {
		return buildStreamGraphFixture(saver, Duration.ofMillis(50));
	}

	private static StreamGraphFixture buildStreamGraphFixture(MemorySaver saver, Duration finalChunkDelay)
			throws Exception {
		AtomicBoolean modelStreamSubscribed = new AtomicBoolean(false);
		AtomicBoolean modelStreamCancelled = new AtomicBoolean(false);
		AtomicBoolean modelStreamCompleted = new AtomicBoolean(false);
		AtomicBoolean streamingNodeAfterCalled = new AtomicBoolean(false);

		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("tool_node", node_async(state -> Map.of("messages", toolResponse())))
			.addNode("streaming_model",
					node_async(state -> Map.of("messages",
							streamingAssistantResponse(modelStreamSubscribed, modelStreamCancelled,
									modelStreamCompleted, finalChunkDelay))))
			.addEdge(START, "tool_node")
			.addEdge("tool_node", "streaming_model")
			.addEdge("streaming_model", END);

		CompiledGraph compiledGraph = stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.withLifecycleListener(new GraphLifecycleListener() {
				@Override
				public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
					if ("streaming_model".equals(nodeId)) {
						streamingNodeAfterCalled.set(true);
					}
				}
			})
			.build());
		return new StreamGraphFixture(compiledGraph, modelStreamSubscribed, modelStreamCancelled, modelStreamCompleted,
				streamingNodeAfterCalled);
	}

	private static CompiledGraph buildFailingGraph(MemorySaver saver) throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("tool_node", node_async(state -> Map.of("messages", toolResponse())))
			.addNode("streaming_model",
					node_async(state -> Map.of("messages",
							Flux.<ChatResponse>error(new IllegalStateException("model stream failed")))))
			.addEdge(START, "tool_node")
			.addEdge("tool_node", "streaming_model")
			.addEdge("streaming_model", END);

		return stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.build());
	}

	private static CompiledGraph buildAnswerGraph(BaseCheckpointSaver saver, String answer, GraphLifecycleListener listener)
			throws Exception {
		return buildAnswerGraph(saver, answer, false, listener);
	}

	private static CompiledGraph buildAnswerGraph(BaseCheckpointSaver saver, String answer, boolean releaseThread,
			GraphLifecycleListener listener) throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("streaming_model",
				node_async(state -> Map.of("messages", Flux.just(chatResponse(answer)))))
			.addEdge(START, "streaming_model")
			.addEdge("streaming_model", END);

		CompileConfig.Builder builder = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.releaseThread(releaseThread);
		if (listener != null) {
			builder.withLifecycleListener(listener);
		}
		return stateGraph.compile(builder.build());
	}

	private static final class BlockingSaver implements BaseCheckpointSaver {

		private final LinkedList<Checkpoint> checkpoints = new LinkedList<>();

		private final boolean blockFirstAppend;

		private final boolean blockFirstRelease;

		private final CountDownLatch firstAppendReached = new CountDownLatch(1);

		private final CountDownLatch releaseFirstAppend = new CountDownLatch(1);

		private final CountDownLatch firstReleaseReached = new CountDownLatch(1);

		private final CountDownLatch releaseFirstRelease = new CountDownLatch(1);

		private final AtomicBoolean shouldBlockFirstAppend = new AtomicBoolean(true);

		private final AtomicBoolean shouldBlockFirstRelease = new AtomicBoolean(true);

		BlockingSaver(boolean blockFirstAppend, boolean blockFirstRelease) {
			this.blockFirstAppend = blockFirstAppend;
			this.blockFirstRelease = blockFirstRelease;
		}

		@Override
		public synchronized Collection<Checkpoint> list(RunnableConfig config) {
			return List.copyOf(checkpoints);
		}

		@Override
		public synchronized Optional<Checkpoint> get(RunnableConfig config) {
			if (config.checkPointId().isPresent()) {
				return checkpoints.stream()
					.filter(checkpoint -> checkpoint.getId().equals(config.checkPointId().get()))
					.findFirst();
			}
			return checkpoints.isEmpty() ? Optional.empty() : Optional.of(checkpoints.peek());
		}

		@Override
		public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
			if (config.checkPointId().isPresent()) {
				return replace(config, checkpoint);
			}
			if (blockFirstAppend && shouldBlockFirstAppend.compareAndSet(true, false)) {
				firstAppendReached.countDown();
				if (!releaseFirstAppend.await(3, TimeUnit.SECONDS)) {
					throw new IllegalStateException("first append was not released");
				}
			}
			synchronized (this) {
				checkpoints.push(checkpoint);
			}
			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
		}

		private synchronized RunnableConfig replace(RunnableConfig config, Checkpoint checkpoint) {
			String checkpointId = config.checkPointId().orElseThrow();
			for (int i = 0; i < checkpoints.size(); i++) {
				if (checkpointId.equals(checkpoints.get(i).getId())) {
					checkpoints.set(i, checkpoint);
					return config;
				}
			}
			throw new IllegalArgumentException("Checkpoint with id " + checkpointId + " not found");
		}

		@Override
		public Tag release(RunnableConfig config) throws Exception {
			if (blockFirstRelease && shouldBlockFirstRelease.compareAndSet(true, false)) {
				firstReleaseReached.countDown();
				if (!releaseFirstRelease.await(3, TimeUnit.SECONDS)) {
					throw new IllegalStateException("first release was not released");
				}
			}
			return doRelease(config);
		}

		private synchronized Tag doRelease(RunnableConfig config) {
			String threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
			List<Checkpoint> released = List.copyOf(checkpoints);
			checkpoints.clear();
			return new Tag(threadId, released);
		}

		boolean awaitFirstAppendReached() throws InterruptedException {
			return firstAppendReached.await(2, TimeUnit.SECONDS);
		}

		void releaseFirstAppend() {
			releaseFirstAppend.countDown();
		}

		boolean awaitFirstReleaseReached() throws InterruptedException {
			return firstReleaseReached.await(2, TimeUnit.SECONDS);
		}

		void releaseFirstRelease() {
			releaseFirstRelease.countDown();
		}

	}

	private static CompiledGraph buildContinuationGraph(MemorySaver saver, AtomicBoolean openAiPayloadVerified,
			List<String> expectedRoles) throws Exception {
		StateGraph stateGraph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy());
			return strategies;
		}).addNode("streaming_model", node_async(state -> {
			@SuppressWarnings("unchecked")
			List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
			ChatModel chatModel = verifyingOpenAiPayloadChatModel(openAiPayloadVerified, expectedRoles);
			return Map.of("messages", chatModel.stream(new Prompt(messages)));
		})).addEdge(START, "streaming_model").addEdge("streaming_model", END);

		return stateGraph.compile(CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(saver).build())
			.build());
	}

	private static ChatModel verifyingOpenAiPayloadChatModel(AtomicBoolean verified, List<String> expectedRoles) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage("continued"))));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				List<Map<String, Object>> openAiMessages = toOpenAiMessages(prompt.getInstructions());
				assertOpenAiToolMessageOrder(openAiMessages);
				assertEquals(expectedRoles, openAiMessages.stream()
					.map(message -> (String) message.get("role"))
					.toList());
				assertEquals("\u8bf7\u7ee7\u7eed", openAiMessages.get(openAiMessages.size() - 1).get("content"));
				verified.set(true);
				return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("continued")))));
			}
		};
	}

	private static Map<String, Object> initialMessages() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		List<Message> messages = List.of(
				new UserMessage("search first"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build());
		return Map.of(OverAllState.DEFAULT_INPUT_KEY, "search first", "messages", messages);
	}

	private static Map<String, Object> stableState() {
		return Map.of("messages", List.of(new UserMessage("hello"), new AssistantMessage("previous answer")));
	}

	private static Map<String, Object> incompleteToolResponseState() {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call_1", "function", "search", "{}");
		return Map.of("messages", List.of(
				new UserMessage("hello"),
				new AssistantMessage("previous answer"),
				new UserMessage("search"),
				AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build(),
				toolResponse()));
	}

	private static ToolResponseMessage toolResponse() {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "search", "ok")))
			.build();
	}

	private static Flux<ChatResponse> streamingAssistantResponse(AtomicBoolean subscribed, AtomicBoolean cancelled,
			AtomicBoolean completed, Duration finalChunkDelay) {
		return Flux.concat(
				Flux.just(chatResponse("tool result")),
				Flux.just(chatResponse(" received")).delayElements(finalChunkDelay))
			.doOnSubscribe(subscription -> subscribed.set(true))
			.doOnCancel(() -> cancelled.set(true))
			.doOnComplete(() -> completed.set(true));
	}

	private static ChatResponse chatResponse(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static List<?> awaitCheckpointMessages(CompiledGraph compiledGraph, RunnableConfig config)
			throws InterruptedException {
		List<?> lastMessages = List.of();
		for (int attempt = 0; attempt < 20; attempt++) {
			lastMessages = checkpointMessages(compiledGraph, config);
			if (!lastMessages.isEmpty()
					&& lastMessages.get(lastMessages.size() - 1) instanceof AssistantMessage) {
				return lastMessages;
			}
			Thread.sleep(50);
		}
		return lastMessages;
	}

	private static List<?> checkpointMessages(CompiledGraph compiledGraph, RunnableConfig config) {
		return compiledGraph.stateOf(config)
			.flatMap(state -> state.state().value("messages"))
			.filter(List.class::isInstance)
			.map(List.class::cast)
			.orElse(List.of());
	}

	private static Object awaitLastCheckpointMessage(CompiledGraph compiledGraph, RunnableConfig config)
			throws InterruptedException {
		List<?> checkpointMessages = awaitCheckpointMessages(compiledGraph, config);
		return checkpointMessages.get(checkpointMessages.size() - 1);
	}

	private static boolean awaitUntil(BooleanSupplier condition) throws InterruptedException {
		for (int attempt = 0; attempt < 20; attempt++) {
			if (condition.getAsBoolean()) {
				return true;
			}
			Thread.sleep(50);
		}
		return false;
	}

	private static Flux<NodeOutput> timeoutAfterStreamingModelStarts(Flux<NodeOutput> stream) {
		return stream.timeout(Mono.delay(Duration.ofSeconds(1)), output -> "streaming_model".equals(output.node())
				? Mono.delay(Duration.ofMillis(25)) : Mono.delay(Duration.ofSeconds(1)));
	}

	private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
		Throwable current = throwable;
		while (current != null) {
			if (causeType.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private static List<Map<String, Object>> toOpenAiMessages(List<Message> messages) {
		List<Map<String, Object>> openAiMessages = new ArrayList<>();
		for (Message message : messages) {
			if (message instanceof UserMessage userMessage) {
				openAiMessages.add(Map.of("role", "user", "content", userMessage.getText()));
			}
			else if (message instanceof AssistantMessage assistantMessage) {
				Map<String, Object> openAiMessage = new HashMap<>();
				openAiMessage.put("role", "assistant");
				openAiMessage.put("content", assistantMessage.getText());
				if (assistantMessage.hasToolCalls()) {
					openAiMessage.put("tool_calls", assistantMessage.getToolCalls()
						.stream()
						.map(toolCall -> Map.of("id", toolCall.id(), "type", toolCall.type(), "function",
								Map.of("name", toolCall.name(), "arguments", toolCall.arguments())))
						.toList());
				}
				openAiMessages.add(openAiMessage);
			}
			else if (message instanceof ToolResponseMessage toolResponseMessage) {
				for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
					openAiMessages.add(Map.of("role", "tool", "tool_call_id", response.id(), "content",
							response.responseData()));
				}
			}
		}
		return openAiMessages;
	}

	@SuppressWarnings("unchecked")
	private static void assertOpenAiToolMessageOrder(List<Map<String, Object>> messages) {
		Set<String> pendingToolCallIds = new HashSet<>();
		boolean waitingForAssistantAfterTool = false;
		for (Map<String, Object> message : messages) {
			String role = (String) message.get("role");
			if ("assistant".equals(role)) {
				assertTrue(pendingToolCallIds.isEmpty(), "assistant cannot appear before all tool calls are answered");
				waitingForAssistantAfterTool = false;
				Object toolCalls = message.get("tool_calls");
				if (toolCalls instanceof List<?> calls) {
					for (Object call : calls) {
						pendingToolCallIds.add((String) ((Map<String, Object>) call).get("id"));
					}
				}
			}
			else if ("tool".equals(role)) {
				assertFalse(pendingToolCallIds.isEmpty(), "tool message must follow assistant tool calls");
				assertTrue(pendingToolCallIds.remove(message.get("tool_call_id")),
						"tool message must reference a pending assistant tool call");
				waitingForAssistantAfterTool = pendingToolCallIds.isEmpty();
			}
			else {
				assertTrue(pendingToolCallIds.isEmpty() && !waitingForAssistantAfterTool,
						role + " message cannot follow an unfinished tool response sequence");
			}
		}
		assertTrue(pendingToolCallIds.isEmpty(), "all assistant tool calls must have tool responses");
		assertFalse(waitingForAssistantAfterTool, "tool response must be followed by assistant before new user input");
	}

	private record StreamGraphFixture(
			CompiledGraph compiledGraph,
			AtomicBoolean modelStreamSubscribed,
			AtomicBoolean modelStreamCancelled,
			AtomicBoolean modelStreamCompleted,
			AtomicBoolean streamingNodeAfterCalled) {

		void assertModelStreamCompletedWithoutCancellation(String cancellationType) {
			assertTrue(modelStreamSubscribed.get(), "model stream should be subscribed");
			assertFalse(modelStreamCancelled.get(), "model stream should not be cancelled by " + cancellationType);
			assertTrue(modelStreamCompleted.get(), "model stream should keep running after " + cancellationType);
		}
	}

}
