/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

public class GraphAgentExecutor implements AgentExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

	private static final Set<String> IGNORE_NODE_TYPE = Set.of("preLlm", "postLlm", "preTool", "tool", "postTool");

	public static final String STREAMING_METADATA_KEY = "isStreaming";

	private final Agent executeAgent;

	public GraphAgentExecutor(Agent executeAgent) {
		this.executeAgent = executeAgent;
	}

	private Task newTask(Message request) {
		String contextId = request.getContextId();
		if (contextId == null || contextId.isEmpty()) {
			contextId = UUID.randomUUID().toString();
		}
		String id = UUID.randomUUID().toString();
		if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
			id = request.getTaskId();
		}
		return new Task(id, contextId, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
	}

	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		try {
			Message message = context.getParams().message();
			StringBuilder sb = new StringBuilder();
			for (Part<?> each : message.getParts()) {
				if (Part.Kind.TEXT.equals(each.getKind())) {
					sb.append(((TextPart) each).getText()).append("\n");
				}
			}
			// TODO adapter for all agent type, now only support react agent
			String input = sb.toString().trim();
			Map<String, Object> messages = Map.of();
			if (StringUtils.hasLength(input)) {
				messages = Map.of("messages", List.of(new UserMessage(input)));
			} else {
				LOGGER.info("Instruction in remote agent is empty, this agent will share messages with remote agent by using the same threadId.");
			}

			if (isStreamRequest(context)) {
				executeStreamTask(messages, context, eventQueue);
			}
			else {
				executeForNonStreamTask(messages, context, eventQueue);
			}
		}
		catch (Exception e) {
			LOGGER.error("Agent execution failed", e);
			eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
	}

	private boolean isStreamRequest(RequestContext context) {
		MessageSendParams params = context.getParams();
		if (null == params.metadata()) {
			return false;
		}
		if (!params.metadata().containsKey(STREAMING_METADATA_KEY)) {
			return false;
		}
		return (boolean) params.metadata().get(STREAMING_METADATA_KEY);
	}

	private RunnableConfig getRunnableConfig(RequestContext context) {
		RunnableConfig.Builder builder = RunnableConfig.builder();

		// Get metadata from context
		MessageSendParams params = context.getParams();
		if (params != null && params.metadata() != null) {
			Map<String, Object> metadata = params.metadata();

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

	private void executeStreamTask(Map<String, Object> input, RequestContext context, EventQueue eventQueue)
			throws GraphStateException, GraphRunnerException {
		RunnableConfig runnableConfig = getRunnableConfig(context);
		Flux<NodeOutput> generator = executeAgent.stream(input, runnableConfig);
		Task task = context.getTask();
		if (task == null) {
			task = newTask(context.getMessage());
			eventQueue.enqueueEvent(task);
		}
		TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
		taskUpdater.submit();
		generator.subscribe(new ReactAgentNodeOutputConsumer(taskUpdater), throwable -> {
			LOGGER.error("Agent execution failed", throwable);
			taskUpdater.fail(A2A.toAgentMessage(throwable.getMessage()));
		}, taskUpdater::complete);
		waitTaskCompleted(task);
	}

	private void executeForNonStreamTask(Map<String, Object> input, RequestContext context, EventQueue eventQueue)
			throws GraphStateException, GraphRunnerException {
		RunnableConfig runnableConfig = getRunnableConfig(context);
		var result = executeAgent.invoke(input, runnableConfig);
		// FIXME: currently only support ReactAgent and A2aRemoteAgent as the root agent
		String outputText = result.get().data().containsKey(((BaseAgent)executeAgent).getOutputKey())
				? String.valueOf(result.get().data().get(((BaseAgent)executeAgent).getOutputKey())) : "No output key in result.";

		Task task = context.getTask();
		if (task == null) {
			task = newTask(context.getMessage());
			eventQueue.enqueueEvent(task);
		}
		TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
		boolean taskComplete = true;
		boolean requireUserInput = false;
		if (!taskComplete && !requireUserInput) {
			taskUpdater.startWork(taskUpdater.newAgentMessage(List.of(new TextPart(outputText)), Map.of()));
		}
		else if (requireUserInput) {
			taskUpdater.startWork(taskUpdater.newAgentMessage(List.of(new TextPart(outputText)), Map.of()));
		}
		else {
			taskUpdater.addArtifact(List.of(new TextPart(outputText)), UUID.randomUUID().toString(),
					"conversation_result", Map.of("output", outputText));
			taskUpdater.complete();
		}
	}

	private void waitTaskCompleted(Task task) {
		while (!task.getStatus().state().equals(TaskState.COMPLETED)
				&& !task.getStatus().state().equals(TaskState.CANCELED)) {
			try {
				TimeUnit.SECONDS.sleep(1);
			}
			catch (InterruptedException ignored) {
			}
		}
	}

	private static class ReactAgentNodeOutputConsumer implements Consumer<NodeOutput> {

		private final TaskUpdater taskUpdater;

		private final AtomicInteger artifactNum;

		private ReactAgentNodeOutputConsumer(TaskUpdater taskUpdater) {
			this.taskUpdater = taskUpdater;
			this.artifactNum = new AtomicInteger();
		}

		@Override
		public void accept(NodeOutput nodeOutput) {
			if (nodeOutput.isSTART() || nodeOutput.isEND() || IGNORE_NODE_TYPE.contains(nodeOutput.node())) {
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

			taskUpdater.addArtifact(Collections.singletonList(new TextPart(content)), null,
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
