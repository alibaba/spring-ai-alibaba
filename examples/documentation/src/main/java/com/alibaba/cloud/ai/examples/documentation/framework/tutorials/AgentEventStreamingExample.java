/*
 * Copyright 2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StreamingToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStreamPart;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.StreamOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/** Streams an agent used as a graph node and as a delegated tool. */
public class AgentEventStreamingExample {

	private static final Logger log = LoggerFactory.getLogger(AgentEventStreamingExample.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final String QUESTION = "How many items are available?";

	private static final StreamOptions OPTIONS = new StreamOptions(
			Set.of(RunEvent.Channel.LIFECYCLE, RunEvent.Channel.MESSAGES, RunEvent.Channel.TOOLS,
					RunEvent.Channel.CUSTOM),
			true, false, false, 256, 256 * 1024);

	public static ReactAgent inventoryAgent() {
		return ReactAgent.builder().name("inventoryAgent").description("Checks stock")
				.model(new LocalToolModel("checkStock", "{}", "Inventory report: "))
				.tools(new StockTool())
				.build();
	}

	public static ReactAgent supervisor() {
		return ReactAgent.builder().name("supervisor")
				.model(new LocalToolModel("inventoryAgent", "{\"input\":\"Check stock\"}", "Supervisor answer: "))
				.tools(AgentTool.create(inventoryAgent()))
				.build();
	}

	public static Flux<RunEvent> streamAgentAsNode() throws GraphStateException {
		var worker = inventoryAgent();
		var graph = new StateGraph("stockWorkflow", () -> Map.of("messages", new AppendStrategy()))
				.addNode(worker.name(), worker.asNode(true, false))
				.addEdge(START, worker.name()).addEdge(worker.name(), END).compile();
		return graph.streamEvents(Map.of("messages", List.of(new UserMessage(QUESTION))),
				RunnableConfig.builder().build(), OPTIONS);
	}

	public static RunEvent streamAgentAsTool() throws Exception {
		var supervisor = supervisor();
		var config = RunnableConfig.builder().threadId("stock-42").build();
		try (var run = supervisor.eventRun(QUESTION, config, OPTIONS)) {
			var messages = run.messages().doOnNext(AgentEventStreamingExample::logEvent).then().toFuture();
			var tools = run.toolCalls().doOnNext(AgentEventStreamingExample::logEvent).then().toFuture();
			var workers = run.subagents().flatMap(worker -> worker.output().doOnNext(outcome ->
					log.info("Worker scope {} finished with {}", worker.scope().scopeId(), outcome.type())))
					.then().toFuture();
			run.start();
			var outcome = run.output().block(TIMEOUT);
			CompletableFuture.allOf(messages, tools, workers).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			return outcome;
		}
	}

	public static class StockTool implements StreamingToolCallback {

		@Override
		public ToolDefinition getToolDefinition() {
			return ToolDefinition.builder().name("checkStock")
					.description("Reads current stock")
					.inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
		}

		@Override
		public Flux<ToolStreamPart> stream(String arguments, ToolContext context, CancellationToken cancellation) {
			return Flux.defer(() -> {
				cancellation.throwIfCancelled();
				ToolContextHelper.streamWriter(context).tryEmit("inventory.progress", "Checking the warehouse");
				return Flux.just(new ToolStreamPart.Delta("Warehouse checked"),
						new ToolStreamPart.Completed("7 items available"));
			});
		}
	}

	private record LocalToolModel(String toolName, String arguments, String prefix) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			var toolResult = toolResult(prompt);
			return toolResult == null ? toolCall() : response(new AssistantMessage(prefix + toolResult));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			var toolResult = toolResult(prompt);
			return toolResult == null ? Flux.just(toolCall())
					: Flux.just(prefix, toolResult).map(AssistantMessage::new).map(LocalToolModel::response);
		}

		private ChatResponse toolCall() {
			var call = new AssistantMessage.ToolCall(toolName + "-1", "function", toolName, arguments);
			return response(AssistantMessage.builder().toolCalls(List.of(call)).build());
		}

		private static String toolResult(Prompt prompt) {
			return prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast).reduce((earlier, later) -> later)
					.map(message -> message.getResponses().get(0).responseData()).orElse(null);
		}

		private static ChatResponse response(AssistantMessage message) {
			return new ChatResponse(List.of(new Generation(message)));
		}
	}

	private static void logEvent(RunEvent event) {
		if (event.data() instanceof RunEvent.ModelDelta delta && delta.text() != null) {
			log.info("{} {}", event.scope().nodeId(), delta.text());
		}
		else if (event.data() instanceof RunEvent.Tool tool && event.type() == RunEvent.Type.TOOL_DELTA) {
			log.info("{} {} {}", tool.toolCallId(), event.scope().scopeId(), tool.value());
		}
		else if (event.data() instanceof RunEvent.Custom progress) {
			log.info("{} {}", progress.name(), progress.value());
		}
	}

	public static void main(String[] args) throws Exception {
		var node = streamAgentAsNode().doOnNext(AgentEventStreamingExample::logEvent).blockLast(TIMEOUT);
		log.info("Agent as node answer = {}", finalAnswer(node));
		log.info("Agent as tool answer = {}", finalAnswer(streamAgentAsTool()));
	}

	private static String finalAnswer(RunEvent event) {
		if (event != null && event.type() == RunEvent.Type.RUN_COMPLETED && event.data() instanceof RunEvent.State state
				&& state.values().get("messages") instanceof List<?> messages && !messages.isEmpty()
				&& messages.get(messages.size() - 1) instanceof AssistantMessage answer) {
			return answer.getText();
		}
		throw new IllegalStateException("The run did not return an answer. Outcome " + event);
	}
}
