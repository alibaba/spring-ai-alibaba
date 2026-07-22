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

package com.alibaba.cloud.ai.a2a.core.server;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.reactivestreams.Subscription;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

public class GraphAgentExecutor implements AgentExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

	private static final Set<String> IGNORE_NODE_TYPE = Set.of("preLlm", "postLlm", "preTool", "tool", "postTool");

	public static final String STREAMING_METADATA_KEY = "isStreaming";

	private static final String AGENT_EXECUTION_FAILED = "Agent execution failed";

	private final Agent executeAgent;

	private final ConcurrentMap<String, StreamExecution> activeStreams = new ConcurrentHashMap<>();

	public GraphAgentExecutor(Agent executeAgent) {
		this.executeAgent = executeAgent;
	}

	@Override
	public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
		try {
			String input = context.getUserInput().trim();
			if (!StringUtils.hasLength(input)) {
				LOGGER.info("Instruction in remote agent is empty, this agent will share messages with remote agent by using the same threadId.");
			}

			if (isStreamRequest(context)) {
				executeStreamTask(input, context, emitter);
			}
			else {
				executeForNonStreamTask(input, context, emitter);
			}
		}
		catch (Exception e) {
			LOGGER.error("Agent execution failed", e);
			emitter.fail(emitter.newAgentMessage(List.of(new TextPart(AGENT_EXECUTION_FAILED)), Map.of()));
		}
	}

	@Override
	public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
		String streamKey = streamKey(context, emitter);
		StreamExecution execution = this.activeStreams.get(streamKey);
		if (execution == null) {
			emitter.cancel();
			return;
		}
		boolean cancelled = execution.cancel();
		if (cancelled) {
			try {
				emitter.cancel();
			}
			finally {
				this.activeStreams.remove(streamKey, execution);
				execution.signalCompletion();
			}
		}
	}

	private boolean isStreamRequest(RequestContext context) {
		Map<String, Object> metadata = context.getMetadata();
		if (metadata == null) {
			return false;
		}
		return Boolean.TRUE.equals(metadata.get(STREAMING_METADATA_KEY));
	}

	private RunnableConfig getRunnableConfig(RequestContext context) {
		RunnableConfig.Builder builder = RunnableConfig.builder();

		// Get metadata from context
		Map<String, Object> metadata = context.getMetadata();
		if (metadata != null) {

			// Check if threadId exists in metadata and add it to RunnableConfig
			if (metadata.containsKey("threadId")) {
				Object threadIdObj = metadata.get("threadId");
				if (threadIdObj instanceof String) {
					builder.threadId((String) threadIdObj);
				}
			}

			// Add all metadata to RunnableConfig
			for (Map.Entry<String, Object> entry : metadata.entrySet()) {
				builder.addMetadata(entry.getKey(), entry.getValue());
			}
		}

		return builder.build();
	}

	private void executeStreamTask(String inputMessage, RequestContext context, AgentEmitter emitter)
			throws GraphStateException, GraphRunnerException {
		String streamKey = streamKey(context, emitter);
		StreamExecution execution = new StreamExecution();
		if (this.activeStreams.putIfAbsent(streamKey, execution) != null) {
			throw new IllegalStateException("An agent stream is already active for task " + context.getTaskId());
		}

		try {
			RunnableConfig runnableConfig = getRunnableConfig(context);
			Flux<NodeOutput> generator = executeAgent.stream(inputMessage, runnableConfig);
			ReactAgentNodeOutputConsumer outputConsumer = new ReactAgentNodeOutputConsumer(emitter);
			AtomicReference<Error> fatalError = new AtomicReference<>();
			BaseSubscriber<NodeOutput> subscriber = new BaseSubscriber<>() {
				@Override
				protected void hookOnSubscribe(Subscription subscription) {
					requestUnbounded();
				}

				@Override
				protected void hookOnNext(NodeOutput value) {
					outputConsumer.accept(value);
				}

				@Override
				protected void hookOnError(Throwable throwable) {
					if (throwable instanceof Error error) {
						fatalError.set(error);
						terminateStream(streamKey, execution, () -> {
						});
						return;
					}
					LOGGER.error("Agent execution failed", throwable);
					terminateStream(streamKey, execution,
							() -> emitter.fail(emitter.newAgentMessage(List.of(new TextPart(AGENT_EXECUTION_FAILED)), Map.of())));
				}

				@Override
				protected void hookOnComplete() {
					terminateStream(streamKey, execution, emitter::complete);
				}
			};
			boolean started = execution.start(subscriber, () -> startTask(context, emitter));
			if (started) {
				generator.subscribe(subscriber);
				awaitCompletion(execution);
				if (fatalError.get() != null) {
					throw fatalError.get();
				}
			}
		}
		catch (Exception ex) {
			LOGGER.error("Agent execution failed", ex);
			if (execution.cancel()) {
				finishClaimedStream(streamKey, execution,
						() -> emitter.fail(emitter.newAgentMessage(List.of(new TextPart(AGENT_EXECUTION_FAILED)), Map.of())));
			}
		}
		catch (Error ex) {
			if (execution.cancel()) {
				this.activeStreams.remove(streamKey, execution);
				execution.signalCompletion();
			}
			throw ex;
		}
	}

	private void terminateStream(String streamKey, StreamExecution execution, Runnable terminalSignal) {
		if (execution.claimTermination()) {
			finishClaimedStream(streamKey, execution, terminalSignal);
		}
	}

	private void finishClaimedStream(String streamKey, StreamExecution execution, Runnable terminalSignal) {
		try {
			terminalSignal.run();
		}
		finally {
			this.activeStreams.remove(streamKey, execution);
			execution.signalCompletion();
		}
	}

	private void executeForNonStreamTask(String inputMessage, RequestContext context, AgentEmitter emitter)
			throws GraphStateException, GraphRunnerException {
		RunnableConfig runnableConfig = getRunnableConfig(context);
		startTask(context, emitter);
		var result = executeAgent.invoke(inputMessage, runnableConfig);
		// FIXME: currently only support ReactAgent and A2aRemoteAgent as the root agent
		String outputText = result.get().data().containsKey(((BaseAgent)executeAgent).getOutputKey())
				? String.valueOf(result.get().data().get(((BaseAgent)executeAgent).getOutputKey())) : "No output key in result.";

		emitter.addArtifact(List.of(new TextPart(outputText)), null, "conversation_result",
				Map.of("output", outputText));
		emitter.complete();
	}

	private void startTask(RequestContext context, AgentEmitter emitter) {
		if (context.getTask() == null) {
			emitter.submit();
		}
		emitter.startWork();
	}

	private String streamKey(RequestContext context, AgentEmitter emitter) {
		String taskId = context.getTaskId();
		if (!StringUtils.hasText(taskId)) {
			taskId = emitter.getTaskId();
		}
		if (!StringUtils.hasText(taskId)) {
			throw new IllegalStateException("Cannot track an agent stream without a task id");
		}
		return taskId;
	}

	private void awaitCompletion(StreamExecution execution) {
		try {
			execution.awaitCompletion();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for agent stream completion", e);
		}
	}

	private static final class StreamExecution {

		private final CountDownLatch completion = new CountDownLatch(1);

		private final AtomicBoolean terminationClaimed = new AtomicBoolean();

		private final AtomicReference<Disposable> subscription = new AtomicReference<>();

		synchronized boolean start(Disposable disposable, Runnable taskStarter) {
			if (this.terminationClaimed.get()) {
				disposable.dispose();
				return false;
			}
			if (!this.subscription.compareAndSet(null, disposable)) {
				disposable.dispose();
				throw new IllegalStateException("Agent stream subscription was already registered");
			}
			taskStarter.run();
			return true;
		}

		synchronized boolean claimTermination() {
			return this.terminationClaimed.compareAndSet(false, true);
		}

		synchronized boolean cancel() {
			if (!claimTermination()) {
				return false;
			}
			Disposable disposable = this.subscription.get();
			if (disposable != null) {
				disposable.dispose();
			}
			return true;
		}

		void signalCompletion() {
			this.completion.countDown();
		}

		void awaitCompletion() throws InterruptedException {
			this.completion.await();
		}

	}

	private static class ReactAgentNodeOutputConsumer implements Consumer<NodeOutput> {

		private final AgentEmitter emitter;

		private final AtomicInteger artifactNum;

		private ReactAgentNodeOutputConsumer(AgentEmitter emitter) {
			this.emitter = emitter;
			this.artifactNum = new AtomicInteger();
		}

		@Override
		public void accept(NodeOutput nodeOutput) {
			if (nodeOutput.isSTART() || nodeOutput.isEND()
					|| (nodeOutput.node() != null && IGNORE_NODE_TYPE.contains(nodeOutput.node()))) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Agent parts output: {}", buildDebugDetailInfo(nodeOutput));
				}
				return;
			}

			String content = "";
			if (nodeOutput instanceof StreamingOutput) {
				content = ((StreamingOutput) nodeOutput).chunk();
			}

			if (!StringUtils.hasLength(content)) {
				return;
			}

			emitter.addArtifact(Collections.singletonList(new TextPart(content)), null,
					String.valueOf(artifactNum.incrementAndGet()), Map.of());
		}

		private String buildDebugDetailInfo(NodeOutput nodeOutput) {
			JSONObject outputJson = new JSONObject();
			outputJson.put("data", nodeOutput.state().data());
			outputJson.put("node", nodeOutput.node());
			return JSON.toJSONString(outputJson);
		}

	}

}
