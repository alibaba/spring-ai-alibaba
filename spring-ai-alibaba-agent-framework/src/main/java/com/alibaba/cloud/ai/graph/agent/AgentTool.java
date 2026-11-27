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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.streaming.RunEvent;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.context.AgentToolContext;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.util.JsonHelper;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.taskThreadId;

/**
 * Factory class for creating ToolCallback instances for ReactAgent.
 * This class provides a programmatic way to expose a ReactAgent as a tool without requiring @Tool annotation.
 */
public class AgentTool {
	private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();
	private static final JsonHelper jsonHelper = new JsonHelper();
	private static final AgentToolContext ISOLATED_CONTEXT = AgentToolContext.builder().build();

	public static final String CONTINUATION_CONTEXT_KEY = "_AGENT_TOOL_CONTINUATION_";

	public record Continuation(String threadId, InterruptionMetadata feedback) {
	}

	public static final class Interrupted extends RuntimeException {

		private final String threadId;

		private final InterruptionMetadata interruption;

		Interrupted(String threadId, InterruptionMetadata interruption) {
			super("Agent tool paused for feedback");
			this.threadId = threadId;
			this.interruption = interruption;
		}

		public String threadId() {
			return threadId;
		}

		public InterruptionMetadata interruption() {
			return interruption;
		}

	}

	/**
	 * Create a ToolCallback for invoking a ReactAgent as a tool.
	 *
	 * @param agent the ReactAgent instance
	 * @return ToolCallback for the ReactAgent
	 * @see AgentTool#create(ReactAgent)
	 */
	public static ToolCallback getFunctionToolCallback(ReactAgent agent) {
		return AgentTool.create(agent);
	}

	public static boolean isAgentTool(ToolCallback callback) {
		return callback instanceof AgentToolCallback;
	}

	/**
	 * Create a ToolCallback that forwards raw tool-call JSON to the agent executor.
	 * 
	 * @param agent the ReactAgent instance
	 * @return ToolCallback for the ReactAgent
	 */
	public static ToolCallback create(ReactAgent agent) {
		return create(agent, ISOLATED_CONTEXT);
	}

	/** Creates a delegated tool with explicit parent inputs and child outputs. */
	public static ToolCallback create(ReactAgent agent, AgentToolContext context) {
		// Get the original schema from inputSchema or inputType
		String originalSchema = StringUtils.hasLength(agent.getInputSchema())
				? agent.getInputSchema()
				: (agent.getInputType() != null)
				? JsonSchemaGenerator.generateForType(agent.getInputType())
				: null;

		DefaultToolDefinition.Builder builder = ToolDefinition.builder()
				.name(agent.name())
				.description(agent.description())
				.inputSchema(wrapSchemaInInputParameter(originalSchema));

		return new AgentToolCallback(builder.build(), new AgentToolExecutor(agent, context));
	}

	/**
	 * Wrap the original schema in an "input" parameter.
	 * This method handles both JSON Schema strings (including DRAFT_2020_12 format) and null cases.
	 * 
	 * @param originalSchema the original schema string (may be null, or DRAFT_2020_12 format)
	 * @return a wrapped schema with "input" parameter containing the original schema
	 */
	private static String wrapSchemaInInputParameter(String originalSchema) {
		try {
			// Parse the original schema if provided
			Map<String, Object> originalSchemaMap = null;
			if (StringUtils.hasLength(originalSchema)) {
				try {
					originalSchemaMap = jsonHelper.fromJsonToMap(originalSchema);
				}
				catch (Exception e) {
					// If parsing fails, treat as a simple string type
					originalSchemaMap = Map.of("type", "string");
				}
			}
			else {
				// Default to string type if no schema provided
				originalSchemaMap = Map.of("type", "string");
			}

			// Create the wrapped schema structure
			Map<String, Object> wrappedSchema = new HashMap<>();
			wrappedSchema.put("type", "object");
			
			Map<String, Object> properties = new HashMap<>();
			properties.put("input", originalSchemaMap);
			wrappedSchema.put("properties", properties);
			
			wrappedSchema.put("required", List.of("input"));

			// Convert to JSON string
			return jsonHelper.toJson(wrappedSchema);
		}
		catch (Exception e) {
			// Fallback: create a simple wrapper schema
			return String.format("""
					{
						"type": "object",
						"properties": {
							"input": {
								"type": "string"
							}
						},
						"required": ["input"]
					}
					""");
		}
	}

	private static class AgentToolCallback implements CancellableAsyncToolCallback {

		private final ToolDefinition toolDefinition;

		private final AgentToolExecutor executor;

		AgentToolCallback(ToolDefinition toolDefinition, AgentToolExecutor executor) {
			this.toolDefinition = toolDefinition;
			this.executor = executor;
		}

		@Override
		public ToolDefinition getToolDefinition() {
			return toolDefinition;
		}

		@Override
		public String call(String toolInput) {
			return call(toolInput, new ToolContext(Map.of()));
		}

		@Override
		public String call(String toolInput, ToolContext toolContext) {
			try {
				return executor.executeTool(toolInput, toolContext);
			}
			catch (Interrupted | ToolExecutionException e) {
				throw e;
			}
			catch (RuntimeException e) {
				throw new ToolExecutionException(toolDefinition, e);
			}
		}

		@Override
		public CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken cancellation) {
			var result = executor.executeAgentResult(arguments, context)
					.map(executor::formatResult)
					.onErrorMap(RuntimeException.class,
							error -> error instanceof Interrupted || error instanceof ToolExecutionException
									? error : new ToolExecutionException(toolDefinition, error))
					.subscribeOn(Schedulers.boundedElastic()).toFuture();
			cancellation.onCancel(() -> result.cancel(true));
			return result;
		}

	}

	/**
	 * Executor class for AgentTool. This class does not require @Tool annotation.
	 */
	public static class AgentToolExecutor {

		private final ReactAgent agent;

		private final AgentToolContext scope;

		public AgentToolExecutor(ReactAgent agent) {
			this(agent, ISOLATED_CONTEXT);
		}

		public AgentToolExecutor(ReactAgent agent, AgentToolContext scope) {
			this.agent = Objects.requireNonNull(agent, "agent must not be null");
			this.scope = Objects.requireNonNull(scope, "context must not be null");
		}

		/**
		 * Execute the agent tool with the given input.
		 * This method is called by AgentToolCallback with the raw tool-call JSON and the injected ToolContext.
		 * 
		 * @param input the input JSON string containing the "input" parameter
		 * @param toolContext the tool context containing state, config, etc.
		 * @return AssistantMessage the response from the agent
		 */
		public AssistantMessage executeAgent(String input, ToolContext toolContext) {
			return executeAgentResult(input, toolContext).map(ExecutionResult::answer).block();
		}

		private String executeTool(String input, ToolContext toolContext) {
			if (this.scope.outputKeys().isEmpty()) {
				return CONVERTER.convert(executeAgent(input, toolContext), AssistantMessage.class);
			}
			return executeAgentResult(input, toolContext).map(this::formatResult).block();
		}

		private Mono<ExecutionResult> executeAgentResult(String input, ToolContext toolContext) {
			return Mono.defer(() -> executeChild(input, toolContext));
		}

		private Mono<ExecutionResult> executeChild(String input, ToolContext toolContext) {
			String actualInput = extractInputValue(input);
			var parent = ToolContextHelper.getConfig(toolContext).orElse(null);
			var continuation = ToolContextHelper.getMetadata(toolContext, CONTINUATION_CONTEXT_KEY, Continuation.class);
			var parentState = continuation.isPresent() ? Map.<String, Object>of()
					: ToolContextHelper.getState(toolContext).map(state -> select(state.data(), this.scope.stateKeys()))
						.orElseGet(Map::of);
			var parentMetadata = parent == null ? Map.<String, Object>of()
					: parent.metadata().map(metadata -> select(metadata, this.scope.metadataKeys())).orElseGet(Map::of);
			String stateJson = parentState.isEmpty() ? "" : jsonHelper.toJson(parentState);
			String metadataJson = parentMetadata.isEmpty() ? "" : jsonHelper.toJson(parentMetadata);
			if ((long) stateJson.length() + metadataJson.length() > this.scope.maxContextCharacters()) {
				throw new IllegalArgumentException("Delegated context exceeds the configured character limit");
			}

			var run = childConfig(toolContext, parent, continuation);
			if (!metadataJson.isEmpty()) {
				jsonHelper.fromJsonToMap(metadataJson).forEach(run::addMetadata);
			}
			var inputs = continuation.isPresent() ? Map.<String, Object>of() : childInputs(actualInput, stateJson);
			var childConfig = run.build();
			boolean streamMessages = childConfig.eventContext().filter(events -> events.options().includeNested())
					.filter(events -> events.options().channels().contains(RunEvent.Channel.MESSAGES)).isPresent();
			var childRun = streamMessages ? agent.buildStreamConfig(childConfig) : agent.buildNonStreamConfig(childConfig);
			return Mono.defer(() -> agent.getAndCompileGraph().stream(inputs, childRun).last())
					.onErrorMap(error -> buildExecutionException(actualInput, parent,
							"sub-agent invocation failed", error))
					.map(output -> new ExecutionResult(requireAssistantMessage(output, childRun, actualInput, parent),
							select(output.state().data(), this.scope.outputKeys())));
		}

		private Map<String, Object> childInputs(String actualInput, String stateJson) {
			List<Message> messages = new ArrayList<>();
			if (StringUtils.hasLength(agent.instruction())) {
				messages.add(AgentInstructionMessage.builder().text(agent.instruction()).build());
			}
			if (!stateJson.isEmpty()) {
				messages.add(new UserMessage("Delegated reference data (JSON):\n" + stateJson));
			}
			messages.add(new UserMessage(actualInput));
			return stateJson.isEmpty() ? Map.of("messages", messages)
					: Map.of("messages", messages, AgentToolContext.STATE_KEY, jsonHelper.fromJsonToMap(stateJson));
		}

		private RunnableConfig.Builder childConfig(ToolContext context, RunnableConfig parent,
				Optional<Continuation> continuation) {
			String threadId = continuation.map(Continuation::threadId).orElseGet(() -> childThreadId(context, parent));
			var child = RunnableConfig.isolatedChild(parent).threadId(threadId)
					.addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, threadId);
			continuation.ifPresent(resume -> child.addHumanFeedback(resume.feedback()));
			return child;
		}

		private String childThreadId(ToolContext context, RunnableConfig parent) {
			if (parent != null) {
				var callId = ToolContextHelper.getMetadata(context, AGENT_TOOL_CALL_ID_CONTEXT_KEY, String.class)
						.filter(StringUtils::hasText);
				var dispatch = parent.checkPointId().or(parent::taskId)
						.or(() -> ToolContextHelper.getState(context)
								.flatMap(state -> state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class)));
				if (callId.isPresent() && dispatch.isPresent()) {
					return taskThreadId(parent, agent.name(), dispatch.get() + ":" + callId.get());
				}
			}
			return "task-" + UUID.randomUUID();
		}

		private static Map<String, Object> select(Map<String, Object> source, List<String> keys) {
			Map<String, Object> selected = new LinkedHashMap<>();
			for (String key : keys) {
				Object value = source.get(key);
				if (value != null) {
					selected.put(key, value instanceof Message message ? message.getText() : value);
				}
			}
			return selected;
		}

		private String formatResult(ExecutionResult result) {
			String answer = CONVERTER.convert(result.answer(), AssistantMessage.class);
			return this.scope.outputKeys().isEmpty() ? answer
					: jsonHelper.toJson(Map.of("answer", answer, "outputs", result.outputs()));
		}

		private record ExecutionResult(AssistantMessage answer, Map<String, Object> outputs) {
		}

		private AssistantMessage requireAssistantMessage(NodeOutput output, RunnableConfig run, String actualInput,
				RunnableConfig parent) {
			if (output instanceof InterruptionMetadata pause) {
				throw new Interrupted(run.threadId().orElse(THREAD_ID_DEFAULT), pause);
			}
			Optional<List<Message>> messages = output.state().value("messages");
			if (messages.isPresent()) {
				List<Message> messageList = messages.get();
				if (!messageList.isEmpty() && messageList.get(messageList.size() - 1) instanceof AssistantMessage assistantMessage) {
					return assistantMessage;
				}
				throw buildExecutionException(actualInput, parent,
						"sub-agent returned no assistant message. Last message type: "
								+ (messageList.isEmpty() ? "<empty>" : messageList.get(messageList.size() - 1).getMessageType()),
						null);
			}
			
			throw buildExecutionException(actualInput, parent,
					"sub-agent returned no messages", null);
		}

		private RuntimeException buildExecutionException(String actualInput, RunnableConfig parentConfig, String detail,
				Throwable cause) {
			String threadId = parentConfig != null ? parentConfig.threadId().orElse("<no-thread-id>") : "<standalone>";
			String message = String.format("Failed to execute agent tool '%s' (parentThreadId=%s, input=%s): %s",
					agent.name(), threadId, actualInput, detail);
			return cause == null ? new RuntimeException(message) : new RuntimeException(message, cause);
		}

		/**
		 * Extract the actual input value from the wrapped JSON structure.
		 * The input is expected to be in the format: {"input": "actual_value"}
		 * If the input is not a valid JSON object or doesn't contain "input" field,
		 * the original input string is returned as-is.
		 * 
		 * @param input the wrapped input JSON string
		 * @return the extracted input value, or the original input if extraction fails
		 */
		private String extractInputValue(String input) {
			if (!StringUtils.hasText(input)) {
				return input;
			}

			// Try to parse as JSON object and extract the "input" field
			try {
				Map<String, Object> jsonMap = jsonHelper.fromJsonToMap(input);

				if (jsonMap != null && jsonMap.containsKey("input")) {
					Object inputValue = jsonMap.get("input");
					// Convert the input value to string
					if (inputValue != null) {
						// If it's already a string, return it
						if (inputValue instanceof String) {
							return (String) inputValue;
						}
						// Otherwise, serialize it to JSON string
						return jsonHelper.toJson(inputValue);
					}
				}
			}
			catch (Exception e) {
				// Not a JSON object or parsing failed, use input as-is
			}

			// If extraction fails, return the original input
			return input;
		}
	}
}
