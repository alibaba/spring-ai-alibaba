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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.UserMessage;

public class GraphAgentExecutor implements AgentExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphAgentExecutor.class);

	private final BaseAgent executeAgent;

	public GraphAgentExecutor(BaseAgent executeAgent) {
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
			Map<String, Object> input = Map.of("messages", List.of(new UserMessage(sb.toString().trim())));
			var result = executeAgent.invoke(input);
			String outputText = result.get().data().containsKey(executeAgent.outputKey())
					? String.valueOf(result.get().data().get(executeAgent.outputKey())) : "No output key in result.";

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
		catch (Exception e) {
			LOGGER.error("Agent execution failed", e);
			eventQueue.enqueueEvent(A2A.toAgentMessage("Agent execution failed: " + e.getMessage()));
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
	}

}
