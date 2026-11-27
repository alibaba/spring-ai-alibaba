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

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.THREAD_ID_DEFAULT;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.childInvocation;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.taskThreadId;

/**
 * Factory class for creating ToolCallback instances for ReactAgent.
 * This class provides a programmatic way to expose a ReactAgent as a tool without requiring @Tool annotation.
 */
public class AgentTool {
	private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();
	private static final JsonHelper jsonHelper = new JsonHelper();

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

		return new AgentToolCallback(builder.build(), new AgentToolExecutor(agent));
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
				AssistantMessage result = executor.executeAgent(toolInput, toolContext);
				return CONVERTER.convert(result, AssistantMessage.class);
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
					.map(message -> CONVERTER.convert(message, AssistantMessage.class))
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

		public AgentToolExecutor(ReactAgent agent) {
			this.agent = agent;
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
			return executeAgentResult(input, toolContext).block();
		}

		private Mono<AssistantMessage> executeAgentResult(String input, ToolContext toolContext) {
			// Extract the actual input value from the wrapped JSON structure
			// The input parameter is wrapped as {"input": "actual_value"}
			String actualInput = extractInputValue(input);

			// Build the messages list to add
			// Add instruction first if present, then the user input
			// Note: We must add all messages at once because cloneState doesn't copy keyStrategies,
			// so multiple updateState calls would overwrite instead of append
			List<Message> messagesToAdd = new ArrayList<>();
			if (StringUtils.hasLength(agent.instruction())) {
				messagesToAdd.add(AgentInstructionMessage.builder().text(agent.instruction()).build());
			}
			messagesToAdd.add(new UserMessage(actualInput));

			var parent = ToolContextHelper.getConfig(toolContext).orElse(null);
			var continuation = ToolContextHelper.getMetadata(toolContext, CONTINUATION_CONTEXT_KEY, Continuation.class);
			var run = childConfig(toolContext, parent, continuation);
			var inputs = continuation.isPresent() ? Map.<String, Object>of() : Map.<String, Object>of("messages", messagesToAdd);
			return Mono.defer(() -> agent.getAndCompileGraph().stream(inputs, agent.buildNonStreamConfig(run)).last())
					.onErrorMap(error -> buildExecutionException(actualInput, parent,
							"sub-agent invocation failed: " + error.getMessage(), error))
					.map(output -> requireAssistantMessage(output, run, actualInput, parent));
		}

		private RunnableConfig childConfig(ToolContext context, RunnableConfig parent, Optional<Continuation> continuation) {
			if (parent == null && continuation.isEmpty()) {
				return RunnableConfig.builder().build();
			}
			var caller = parent == null ? RunnableConfig.builder().build() : parent;
			var child = childInvocation(caller, continuation.isPresent())
					.threadId(caller.threadId().map(id -> id + "_" + agent.name()).orElseGet(agent::name))
					.clearContext();
			ToolContextHelper.getMetadata(context, AGENT_TOOL_CALL_ID_CONTEXT_KEY, String.class)
					.filter(StringUtils::hasText).ifPresent(callId -> {
						String parentExecution = caller.checkPointId().or(caller::taskId)
								.or(() -> ToolContextHelper.getState(context)
										.flatMap(state -> state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class)))
								.orElse("");
						String invocationId = taskThreadId(caller, agent.name(), parentExecution + ":" + callId);
						child.threadId(invocationId).addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, invocationId);
					});
			continuation.ifPresent(resume -> child.threadId(resume.threadId())
					.addMetadata(RunnableConfig.TASK_ID_METADATA_KEY, resume.threadId()).addHumanFeedback(resume.feedback()));
			return child.build();
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
