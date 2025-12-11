/*
 * Copyright 2024-2025 the original author or authors.
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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.exception.AgentException;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.factory.DefaultAgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static java.lang.String.format;

public class ReactAgent extends BaseAgent {
	Logger logger = LoggerFactory.getLogger(ReactAgent.class);

	final AgentLlmNode llmNode;

	final AgentToolNode toolNode;

	List<? extends Hook> hooks;

	private List<ModelInterceptor> modelInterceptors;

	private List<ToolInterceptor> toolInterceptors;

	private String instruction;

	StateSerializer stateSerializer;

	final Boolean hasTools;

	public ReactAgent(AgentLlmNode llmNode, AgentToolNode toolNode, CompileConfig compileConfig, Builder builder) {
		super(builder.name, builder.description, builder.includeContents, builder.returnReasoningContents,
				builder.outputKey, builder.outputKeyStrategy);
		this.instruction = builder.instruction;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.compileConfig = compileConfig;
		this.hooks = builder.hooks;
		this.modelInterceptors = builder.modelInterceptors;
		this.toolInterceptors = builder.toolInterceptors;
		this.includeContents = builder.includeContents;
		this.inputSchema = builder.inputSchema;
		this.inputType = builder.inputType;
		this.outputSchema = builder.outputSchema;
		this.outputType = builder.outputType;

		// Set state serializer from builder, or use default
		// Default to Jackson serializer for better compatibility and features
		this.stateSerializer = Objects.requireNonNullElseGet(builder.stateSerializer,
				() -> new SpringAIJacksonStateSerializer(OverAllState::new));

		// Set executor configuration from builder
		this.executor = builder.executor;

		// Set interceptors to nodes
		if (this.modelInterceptors != null && !this.modelInterceptors.isEmpty()) {
			this.llmNode.setModelInterceptors(this.modelInterceptors);
		}
		if (this.toolInterceptors != null && !this.toolInterceptors.isEmpty()) {
			this.toolNode.setToolInterceptors(this.toolInterceptors);
		}

		// Set tools flag if tool interceptors are present.
		hasTools = toolNode.getToolCallbacks() != null && !toolNode.getToolCallbacks().isEmpty();
	}

	public static Builder builder() {
		return new DefaultAgentBuilderFactory().builder();
	}

	public static Builder builder(AgentBuilderFactory agentBuilderFactory) {
		return agentBuilderFactory.builder();
	}

	public AssistantMessage call(String message) throws GraphRunnerException {
		return doMessageInvoke(message, null);
	}

	public AssistantMessage call(String message, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(message, config);
	}

	public AssistantMessage call(UserMessage message) throws GraphRunnerException {
		return doMessageInvoke(message, null);
	}

	public AssistantMessage call(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(message, config);
	}

	public AssistantMessage call(List<Message> messages) throws GraphRunnerException {
		return doMessageInvoke(messages, null);
	}

	public AssistantMessage call(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(messages, config);
	}

	private AssistantMessage doMessageInvoke(Object message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		Optional<OverAllState> state = doInvoke(inputs, config);

		if (StringUtils.hasLength(outputKey)) {
			return state.flatMap(s -> s.value(outputKey))
					.map(msg -> (AssistantMessage) msg)
					.orElseThrow(
							() -> new IllegalStateException("Output key " + outputKey + " not found in agent state"));
		}

		// Add a validation instance when performing message conversion to
		// avoid potential type conversion exceptions.
		return state.flatMap(s -> s.value("messages"))
				.stream()
				.flatMap(messageList -> ((List<?>) messageList).stream()
						.filter(msg -> msg instanceof AssistantMessage)
						.map(msg -> (AssistantMessage) msg))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AgentException("No AssistantMessage found in 'messages' state"));
	}

	public StateGraph getStateGraph() {
		return getGraph();
	}

	public CompiledGraph getCompiledGraph() {
		return compiledGraph;
	}

	@Override
	public Node asNode(boolean includeContents, boolean returnReasoningContents) {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new AgentSubGraphNode(this.name, includeContents, returnReasoningContents, this.compiledGraph,
				this.instruction);
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		return new ReactGraphBuilder(this).build();
	}

	KeyStrategyFactory buildMessagesKeyStrategyFactory(List<? extends Hook> hooks) {
		return () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			if (outputKey != null && !outputKey.isEmpty()) {
				keyStrategyHashMap.put(outputKey,
						outputKeyStrategy == null ? new ReplaceStrategy() : outputKeyStrategy);
			}
			keyStrategyHashMap.put("messages", new AppendStrategy());

			// Iterate through hooks and collect their key strategies
			if (hooks != null) {
				for (Hook hook : hooks) {
					Map<String, KeyStrategy> hookStrategies = hook.getKeyStrategys();
					if (hookStrategies != null && !hookStrategies.isEmpty()) {
						keyStrategyHashMap.putAll(hookStrategies);
					}
				}
			}

			return keyStrategyHashMap;
		};
	}

	EdgeAction makeModelToTools(String modelDestination, String endDestination) {
		return state -> {
			List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
			if (messages.isEmpty()) {
				logger.warn("No messages found in state when routing from model to tools");
				return endDestination;
			}
			Message lastMessage = messages.get(messages.size() - 1);

			// 1. Check the last message type
			if (lastMessage instanceof AssistantMessage assistantMessage) {
				// 2. If last message is AssistantMessage
				if (assistantMessage.hasToolCalls()) {
					return "tool";
				} else {
					return endDestination;
				}
			} else if (lastMessage instanceof ToolResponseMessage) {
				// 3. If last message is ToolResponseMessage
				if (messages.size() < 2) {
					// Should not happen in a valid ReAct loop, but as a safeguard.
					throw new RuntimeException(
							"Less than 2 messages in state when last message is ToolResponseMessage");
				}

				Message secondLastMessage = messages.get(messages.size() - 2);
				if (secondLastMessage instanceof AssistantMessage) {
					AssistantMessage assistantMessage = (AssistantMessage) secondLastMessage;
					ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;

					if (assistantMessage.hasToolCalls()) {
						Set<String> requestedToolNames = assistantMessage.getToolCalls().stream()
								.map(toolCall -> toolCall.name())
								.collect(java.util.stream.Collectors.toSet());

						Set<String> executedToolNames = toolResponseMessage.getResponses().stream()
								.map(response -> response.name())
								.collect(java.util.stream.Collectors.toSet());

						if (executedToolNames.containsAll(requestedToolNames)) {
							return modelDestination; // All requested tools were executed or responded
						} else {
							return "tool"; // Some tools are still pending
						}
					}
				}
			}

			return endDestination;
		};
	}

	EdgeAction makeToolsToModelEdge(String modelDestination, String endDestination) {
		return state -> {
			// 1. Extract last AI message and corresponding tool messages
			ToolResponseMessage toolResponseMessage = fetchLastToolResponseMessage(state);
			// 2. Exit condition: All executed tools have return_direct=True
			if (toolResponseMessage != null && !toolResponseMessage.getResponses().isEmpty()) {
				boolean allReturnDirect = toolResponseMessage.getResponses().stream().allMatch(toolResponse -> {
					String toolName = toolResponse.name();
					return false; // FIXME
				});
				if (allReturnDirect) {
					return endDestination;
				}
			}

			// 3. Default: Continue the loop
			// Tool execution completed successfully, route back to the model
			// so it can process the tool results and decide the next action.
			return modelDestination;
		};
	}

	private ToolResponseMessage fetchLastToolResponseMessage(OverAllState state) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

		ToolResponseMessage toolResponseMessage = null;

		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof ToolResponseMessage) {
				toolResponseMessage = (ToolResponseMessage) messages.get(i);
				break;
			}
		}

		return toolResponseMessage;
	}

	public String instruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
		llmNode.setInstruction(instruction);
	}

	public class AgentToSubCompiledGraphNodeAdapter implements NodeActionWithConfig, ResumableSubGraphAction {

		private String nodeId;

		private boolean includeContents;

		private boolean returnReasoningContents;

		private String instruction;

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public AgentToSubCompiledGraphNodeAdapter(String nodeId, boolean includeContents,
				boolean returnReasoningContents,
				CompiledGraph childGraph, String instruction, CompileConfig parentCompileConfig) {
			this.nodeId = nodeId;
			this.includeContents = includeContents;
			this.returnReasoningContents = returnReasoningContents;
			this.instruction = instruction;
			this.childGraph = childGraph;
			this.parentCompileConfig = parentCompileConfig;
		}

		@Override
		public String getResumeSubGraphId() {
			return resumeSubGraphId(nodeId);
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			final boolean resumeSubgraph = config.metadata(resumeSubGraphId(nodeId), new TypeRef<Boolean>() {
			}).orElse(false);

			RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);
			GraphFlux<GraphResponse<NodeOutput>> subGraphResult;
			Object parentMessages = null;

			AgentInstructionMessage instructionMessage = null;
			if (StringUtils.hasLength(instruction)) {
				instructionMessage = AgentInstructionMessage.builder().text(instruction).build();
			}
			if (includeContents) {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				List<Object> newMessages = new ArrayList<>((List<Object>) stateForChild.remove("messages"));
				// by default, includeContents is true, we pass down the messages from the
				// parent state
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					newMessages.add(instructionMessage);
				}
				stateForChild.put("messages", newMessages);
				subGraphResult = GraphFlux.of(subGraphId(nodeId),
						childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig));
			} else {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				parentMessages = stateForChild.remove("messages");
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					stateForChild.put("messages", instructionMessage);
				}
				subGraphResult = GraphFlux.of(subGraphId(nodeId),
						childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig));
			}

			Map<String, Object> result = new HashMap<>();

			String outputKeyToParent = StringUtils.hasLength(ReactAgent.this.outputKey) ? ReactAgent.this.outputKey
					: "messages";
			result.put(outputKeyToParent, getGraphResponseFlux(parentState, subGraphResult, instructionMessage));
			if (parentMessages != null) {
				result.put("messages", parentMessages);
			}
			return result;
		}

		private @NotNull GraphFlux<GraphResponse<NodeOutput>> getGraphResponseFlux(OverAllState parentState,
				GraphFlux<GraphResponse<NodeOutput>> subGraphResult, AgentInstructionMessage instructionMessage) {
			// Use buffer(2, 1) to create sliding windows: [elem0, elem1], [elem1, elem2],
			// ..., [elemN-1, elemN], [elemN]
			// For windows with 2 elements, emit the first (previous element)
			// For the last window with 1 element, process it specially
			Flux<GraphResponse<NodeOutput>> processedFlux = subGraphResult.getFlux()
					.buffer(2, 1)
					.flatMap(window -> {
						if (window.size() == 1) {
							// Last window: process the last element with message filtering
							return Flux.just(processLastResponse(window.get(0), parentState, instructionMessage));
						} else {
							// Regular window: emit the first element (previous, delayed by one)
							return Flux.just(window.get(0));
						}
					}, 1); // Concurrency of 1 to maintain order

			// Wrap the processed Flux with GraphFlux to preserve subgraph node ID
			String outputKey = StringUtils.hasLength(ReactAgent.this.outputKey) ? ReactAgent.this.outputKey
					: "messages";
			return GraphFlux.of(subGraphId(nodeId), outputKey, processedFlux);
		}

		/**
		 * Process the last response by filtering messages based on parent state and
		 * returnReasoningContents flag.
		 *
		 * @param lastResponse the last response from sub-graph
		 * @param parentState  the parent state containing messages to filter out
		 * @return processed GraphResponse with filtered messages
		 */
		private GraphResponse<NodeOutput> processLastResponse(GraphResponse<NodeOutput> lastResponse,
				OverAllState parentState, AgentInstructionMessage instructionMessage) {
			if (lastResponse == null) {
				return lastResponse;
			}

			if (lastResponse.resultValue().isPresent()) {
				Object resultValue = lastResponse.resultValue().get();
				if (resultValue instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> resultMap = (Map<String, Object>) resultValue;
					if (resultMap.get("messages") instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> messages = new ArrayList<>((List<Object>) resultMap.get("messages"));
						if (!messages.isEmpty()) {
							parentState.value("messages").ifPresent(parentMsgs -> {
								if (parentMsgs instanceof List) {
									messages.removeAll((List<?>) parentMsgs);
								}
							});

							List<Object> finalMessages;
							if (returnReasoningContents) {
								finalMessages = messages;
							} else {
								if (!messages.isEmpty()) {
									if (instructionMessage != null) {
										finalMessages = new ArrayList<>();
										finalMessages.add(instructionMessage);
										finalMessages.add(messages.get(messages.size() - 1));
									} else {
										finalMessages = List.of(messages.get(messages.size() - 1));
									}
								} else {
									finalMessages = List.of();
								}
							}

							Map<String, Object> newResultMap = new HashMap<>(resultMap);
							newResultMap.put("messages", finalMessages);
							return GraphResponse.done(newResultMap);
						}
					}
				}
			}
			return lastResponse;
		}

		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			RunnableConfig subGraphRunnableConfig = RunnableConfig.builder(config)
					.checkPointId(null)
					.clearContext()
					.nextNode(null)
					.addMetadata("_AGENT_", subGraphId(nodeId)) // subGraphId is the same as the name of the agent that
																// created it
					.build();
			var parentSaver = parentCompileConfig.checkpointSaver();
			var subGraphSaver = childGraph.compileConfig.checkpointSaver();

			if (subGraphSaver.isPresent()) {
				if (parentSaver.isEmpty()) {
					throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
				}

				// Check saver are the same instance
				if (parentSaver.get() == subGraphSaver.get()) {
					subGraphRunnableConfig = RunnableConfig.builder(config)
							.threadId(config.threadId()
									.map(threadId -> format("%s_%s", threadId, subGraphId(nodeId)))
									.orElseGet(() -> subGraphId(nodeId)))
							.nextNode(null)
							.checkPointId(null)
							.clearContext()
							.addMetadata("_AGENT_", subGraphId(nodeId)) // subGraphId is the same as the name of the
																		// agent that created it
							.build();
				}
			}
			return subGraphRunnableConfig;
		}

	}

	/**
	 * Internal class that adapts a ReactAgent to be used as a SubGraph Node.
	 */
	private class AgentSubGraphNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public AgentSubGraphNode(String id, boolean includeContents, boolean returnReasoningContents,
				CompiledGraph subGraph, String instruction) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> node_async(new AgentToSubCompiledGraphNodeAdapter(id, includeContents,
							returnReasoningContents, subGraph, instruction, config)));
			this.subGraph = subGraph;
		}

		@Override
		public StateGraph subGraph() {
			return subGraph.stateGraph;
		}

		@Override
		public Map<String, KeyStrategy> keyStrategies() {
			return subGraph.getKeyStrategyMap();
		}
	}
}
