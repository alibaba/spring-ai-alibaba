/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.agent.agentscope;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.tool.ToolExecutionContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

/**
 * Spring AI Alibaba agent that proxies an AgentScope {@link ReActAgent}.
 * <p>
 * AgentScopeAgent uses an {@link ReActAgent.Builder} to create a new ReActAgent instance
 * for each invocation, so that stateful agent state is correctly isolated per call.
 * When a {@link Session} is configured, state is loaded from the session before each run
 * and saved in {@link Flux#doFinally} after the run completes.
 * <p>
 * This agent can be used as a {@link BaseAgent} in graph workflows. Call, invoke, and
 * stream are delegated to the underlying graph (START → self → END), whose single node
 * delegates to a freshly built ReActAgent per apply.
 */
public class AgentScopeAgent extends BaseAgent {

	private static final Logger logger = LoggerFactory.getLogger(AgentScopeAgent.class);

	private static final String AGENT_NODE_ID = "agent";

	private static final String DEFAULT_SESSION_ID = "default";

	private final ReActAgent.Builder reactAgentBuilder;

	private final Session session;

	private final String defaultSessionId;

	private final StateSerializer stateSerializer;

	private final String instruction;

	AgentScopeAgent(ReActAgent.Builder reactAgentBuilder, AgentScopeAgentBuilder builder) {
		super(
				builder.name != null ? builder.name : "AgentScopeAgent",
				builder.description != null ? builder.description : "",
				builder.includeContents,
				builder.returnReasoningContents,
				builder.outputKey,
				builder.outputKeyStrategy != null ? builder.outputKeyStrategy : KeyStrategy.REPLACE);
		this.reactAgentBuilder = Objects.requireNonNull(reactAgentBuilder, "reactAgentBuilder must not be null");
		this.session = builder.session;
		this.defaultSessionId = StringUtils.hasText(builder.defaultSessionId) ? builder.defaultSessionId : DEFAULT_SESSION_ID;
		this.stateSerializer = builder.stateSerializer != null
				? builder.stateSerializer
				: new SpringAIJacksonStateSerializer(OverAllState::new);
		this.instruction = builder.instruction;
	}

	/**
	 * Create a builder that uses the given AgentScope ReActAgent.Builder to instantiate
	 * a new ReActAgent for each invocation. When {@link AgentScopeAgentBuilder#session(Session)}
	 * is set, state is loaded before and saved after each run.
	 * @param reactAgentBuilder the AgentScope ReActAgent.Builder
	 * @return a new builder
	 */
	public static AgentScopeAgentBuilder fromBuilder(ReActAgent.Builder reactAgentBuilder) {
		return new AgentScopeAgentBuilder(reactAgentBuilder);
	}

	// ------------------- Call / Invoke / Stream (delegate to Graph) -------------------
	// BaseAgent extends Agent; invoke(), stream() use doInvoke/doStream which use
	// getAndCompileGraph() and run the graph. So call/invoke/stream are already
	// provided by Agent and work via our initGraph(). We only need call() that
	// returns AssistantMessage: Agent has invoke() returning OverAllState but
	// BaseAgent/ReactAgent also expose call() returning AssistantMessage. So we
	// add call overloads that extract the assistant message from the graph result.

	/**
	 * Call the agent with the given message and return the assistant response.
	 */
	public AssistantMessage call(String message) throws GraphRunnerException {
		return call(message, null);
	}

	/**
	 * Call the agent with the given message and config.
	 */
	public AssistantMessage call(String message, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(
				Map.of("messages", List.of(UserMessage.builder().text(message).build()), "input", message),
				config);
	}

	/**
	 * Call the agent with the given messages and return the assistant response.
	 */
	public AssistantMessage call(List<Message> messages) throws GraphRunnerException {
		return doMessageInvoke(messages, null);
	}

	/**
	 * Call the agent with the given messages and config.
	 */
	public AssistantMessage call(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(messages, config);
	}

	/**
	 * Call the agent with the given inputs map.
	 */
	public AssistantMessage call(Map<String, Object> inputs) throws GraphRunnerException {
		return doMessageInvoke(inputs, null);
	}

	/**
	 * Call the agent with the given inputs map and config.
	 */
	public AssistantMessage call(Map<String, Object> inputs, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(inputs, config);
	}

	@SuppressWarnings("unchecked")
	private AssistantMessage doMessageInvoke(Object input, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = input instanceof Map
				? (Map<String, Object>) input
				: buildMessageInput(input);
		RunnableConfig runnableConfig = config != null ? config : RunnableConfig.builder().build();
		var stateOpt = invoke(inputs, runnableConfig);
		return stateOpt
				.flatMap(s -> s.value("messages"))
				.filter(List.class::isInstance)
				.map(list -> (List<?>) list)
				.filter(list -> !list.isEmpty())
				.map(list -> list.get(list.size() - 1))
				.filter(AssistantMessage.class::isInstance)
				.map(AssistantMessage.class::cast)
				.orElseThrow(() -> new GraphRunnerException("No AssistantMessage in graph result"));
	}

	@Override
	public Node asNode(boolean includeContents, boolean returnReasoningContents) {
		return new Node(this.name, (config) -> node_async(new ReActAgentNodeAction(
				reactAgentBuilder, session, defaultSessionId,
				outputKey, returnReasoningContents, includeContents, instruction, this.name, this.name)));
	}

	/**
	 * Build the internal graph (START → ReActAgentNodeAction → END) for standalone
	 * call/invoke/stream. Note: this graph is not used when the agent is embedded as a
	 * node via {@link #asNode(boolean, boolean)} — asNode returns a Node that directly
	 * wraps ReActAgentNodeAction without going through SubCompiledGraphNode.
	 */
	@Override
	protected StateGraph initGraph() throws GraphStateException {
		StateGraph graph = new StateGraph(name, buildKeyStrategyFactory(), stateSerializer);
		graph.addNode(AGENT_NODE_ID, node_async(new ReActAgentNodeAction(
				reactAgentBuilder, session, defaultSessionId,
				outputKey, returnReasoningContents, includeContents, instruction, AGENT_NODE_ID, this.name)));
		graph.addEdge(START, AGENT_NODE_ID);
		graph.addEdge(AGENT_NODE_ID, END);
		return graph;
	}

	private KeyStrategyFactory buildKeyStrategyFactory() {
		return () -> {
			Map<String, KeyStrategy> map = new HashMap<>();
			map.put("messages", new AppendStrategy());
			if (outputKey != null && !outputKey.isEmpty()) {
				map.put(outputKey, outputKeyStrategy != null ? outputKeyStrategy : KeyStrategy.REPLACE);
			}
			return map;
		};
	}

	/**
	 * Node action that runs the AgentScope ReActAgent via stream and returns a Flux of
	 * GraphResponse. For each apply(), a new ReActAgent is obtained from the supplier,
	 * state is loaded from the session (if present), the agent runs, and state is saved
	 * in Flux doFinally. Uses returnReasoningContents to decide whether to emit all isLast
	 * events as messages or only the final AGENT_RESULT.
	 */
	private static final class ReActAgentNodeAction implements NodeActionWithConfig {

		private final ReActAgent.Builder reactAgentBuilder;
		private final Session session;
		private final String defaultSessionId;
		private final String outputKey;
		private final boolean returnReasoningContents;
		private final boolean includeContents;
		private final String instruction;
		private final String nodeId;
		private final String agentName;

		ReActAgentNodeAction(ReActAgent.Builder reactAgentBuilder, Session session, String defaultSessionId,
				String outputKey, boolean returnReasoningContents, boolean includeContents, String instruction,
				String nodeId, String agentName) {
			this.reactAgentBuilder = Objects.requireNonNull(reactAgentBuilder, "reactAgentBuilder must not be null");
			this.session = session;
			this.defaultSessionId = defaultSessionId != null ? defaultSessionId : DEFAULT_SESSION_ID;
			this.outputKey = StringUtils.hasLength(outputKey) ? outputKey : "messages";
			this.returnReasoningContents = returnReasoningContents;
			this.includeContents = includeContents;
			this.instruction = instruction;
			this.nodeId = nodeId;
			this.agentName = agentName != null ? agentName : "";
		}

		/**
		 * Build the message list to send to ReActAgent: render instruction template
		 * via AgentInstructionMessage; if includeContents is false only the instruction
		 * message is used, otherwise instruction (if set) is prepended to state messages.
		 */
		private List<Message> buildMessagesForAgent(OverAllState state, List<Message> stateMessages) {
			Message instructionMessage = null;
			if (StringUtils.hasLength(instruction)) {
				String renderedText = renderInstructionTemplate(instruction, state);
				instructionMessage = new UserMessage(renderedText);
			}

			if (!includeContents) {
				return instructionMessage != null ? List.of(instructionMessage) : List.of();
			}

			List<Message> out = new ArrayList<>(stateMessages);
			if (instructionMessage != null) {
				out.add(instructionMessage);
			}
			return out;
		}

		private static String renderInstructionTemplate(String template, OverAllState state) {
			Map<String, Object> params = buildTemplateParamsFromState(state);
			return PromptTemplate.builder().template(template).build().render(params);
		}

		private static Map<String, Object> buildTemplateParamsFromState(OverAllState state) {
			Map<String, Object> data = state.data();
			Map<String, Object> processed = new HashMap<>();
			if (data == null) {
				return processed;
			}
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				if ("messages".equals(entry.getKey())) {
					continue;
				}
				Object value = entry.getValue();
				if (value instanceof List) {
					continue;
				}
				if (value instanceof Message message) {
					processed.put(entry.getKey(), message.getText() != null ? message.getText() : "");
				}
				else {
					processed.put(entry.getKey(), value);
				}
			}
			return processed;
		}

		@Override
		public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
			// Build ToolContext with OverAllState, RunnableConfig, and stateForUpdate map (per ToolContextHelper pattern)
			Map<String, Object> stateForUpdate = new HashMap<>();
			Map<String, Object> contextMap = new HashMap<>();
			contextMap.put(AGENT_STATE_CONTEXT_KEY, state);
			contextMap.put(AGENT_CONFIG_CONTEXT_KEY, config);
			contextMap.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, stateForUpdate);
			ToolContext toolContext = new ToolContext(contextMap);

			ToolExecutionContext agentScopeCtx = ToolExecutionContext.builder()
					.register(toolContext)
					.build();
			ReActAgent reactAgent = reactAgentBuilder.toolExecutionContext(agentScopeCtx).build();

			// Derive sessionId from threadId + agentName to isolate each AgentScopeAgent in a graph
			// (same pattern as ReactAgent.MainAgentNodeAction / getSubGraphRunnableConfig)
			String sessionId = config.threadId()
					.map(threadId -> format("%s_%s", threadId, agentName))
					.orElse(defaultSessionId);
			SessionKey sessionKey = SimpleSessionKey.of(sessionId);
			if (session != null && session.exists(sessionKey)) {
				reactAgent.loadFrom(session, sessionKey);
			}

			@SuppressWarnings("unchecked")
			List<Message> stateMessages = (List<Message>) state.value("messages").orElse(List.of());
			List<Message> messages = buildMessagesForAgent(state, stateMessages);
			List<Msg> agentScopeMsgs = AgentScopeMessageUtils.toAgentScopeMessages(messages);
			Flux<Event> eventFlux = reactAgent.stream(agentScopeMsgs, StreamOptions.defaults());

			// Collect isLast events for building final done state
			List<Event> isLastEvents = new ArrayList<>();

			Flux<GraphResponse<NodeOutput>> responseFlux = eventFlux
					.doFinally(signalType -> {
						// Save on error/cancel only; normal completion saves in Flux.defer before emitting done.
						// Root cause: (1) Reactor FluxDoFinally forwards onComplete BEFORE running callback, so
						// block() can return before this runs; (2) graph executor may consider node done when it
						// receives GraphResponse.done() and proceed to END, completing the stream early.
						// Must not rely on doFinally for normal completion.
						if (session != null && signalType != SignalType.ON_COMPLETE) {
							reactAgent.saveTo(session, sessionKey);
						}
					})
					.concatMap(event -> {
						if (!event.isLast()) {
							// Not isLast: skip messages containing toolUseBlocks; convert others to stream output
							Msg agentScopeMsg = event.getMessage();
							if (agentScopeMsg != null && !agentScopeMsg.getContentBlocks(ToolUseBlock.class).isEmpty()) {
								return Flux.empty();
							}
							Message msg = AgentScopeMessageUtils.toMessage(agentScopeMsg);
							if (msg == null) {
								return Flux.empty();
							}
							StreamingOutput<?> so = new StreamingOutput<>(msg, nodeId, agentName, state,
									OutputType.AGENT_MODEL_STREAMING);
							return Flux.just(GraphResponse.of((NodeOutput) so));
						}
						// isLast: skip if not tool call, not tool response, and not from AGENT_RESULT
						isLastEvents.add(event);
						Msg agentScopeMsg = event.getMessage();
						Message msg = agentScopeMsg != null ? AgentScopeMessageUtils.toMessage(event) : null;
						if (msg == null) {
							return Flux.empty();
						}

						OutputType outputType = msg instanceof ToolResponseMessage
								? OutputType.AGENT_TOOL_FINISHED
								: OutputType.AGENT_MODEL_FINISHED;
						StreamingOutput<?> so = new StreamingOutput<>(msg, nodeId, agentName, state, outputType);
						return Flux.just(GraphResponse.of((NodeOutput) so));
					})
					.concatWith(Flux.defer(() -> {
						// Save ReActAgent state before emitting done. Must run here (not in doFinally) because
						// doFinally forwards onComplete before its callback runs, and the graph executor may
						// proceed to END when it receives GraphResponse.done(), so block() can return before
						// doFinally's callback finishes.
						if (session != null) {
							reactAgent.saveTo(session, sessionKey);
						}
						// Build final done state from collected isLast events
						if (isLastEvents.isEmpty()) {
							logger.debug("isLastEvents is empty, skipping done state build from events");
							return Flux.just(GraphResponse.done(stateForUpdate));
						}

						Map<String, Object> doneState = new HashMap<>();

						List<Message> allMessages = new ArrayList<>(messages);
						for (Event e : isLastEvents) {
							Message m = AgentScopeMessageUtils.toMessage(e);
							if (m == null) {
								continue;
							}
							// Skip duplicate AGENT_RESULT: same type and content as previous message
							if (e.getType() == EventType.AGENT_RESULT && !allMessages.isEmpty()) {
								Message last = allMessages.get(allMessages.size() - 1);
								if (Objects.equals(m.getMessageType(), last.getMessageType())
										&& Objects.equals(m.getText(), last.getText())) {
									continue;
								}
							}
							allMessages.add(m);
						}

						if (allMessages.isEmpty()) {
							logger.debug("No valid messages from isLastEvents, skipping done state build");
							return Flux.just(GraphResponse.done(stateForUpdate));
						}

						Message lastResultMessage = allMessages.get(allMessages.size() - 1);
						if (!"messages".equals(outputKey)) {
							doneState.put(outputKey, lastResultMessage);
							// If outputKey is not "messages" and messages is not already in doneState,
							// make sure messages updated for consistent context for next agent node.
						}
						if (!returnReasoningContents) {
							Message firstUserOrInstruction = null;
							for (Message m : allMessages) {
								if (m instanceof UserMessage || m instanceof AgentInstructionMessage) {
									firstUserOrInstruction = m;
									break;
								}
							}
							List<Message> resultMessages = new ArrayList<>();
							if (firstUserOrInstruction != null && firstUserOrInstruction != lastResultMessage) {
								resultMessages.add(firstUserOrInstruction);
							}
							resultMessages.add(lastResultMessage);
							doneState.put("messages", resultMessages);
						} else {
							doneState.put("messages", allMessages);
						}

						// Flatten stateForUpdate (updated by tools via ToolContextHelper) into doneState
						doneState.putAll(stateForUpdate);

						return Flux.just(GraphResponse.done(doneState));
					}));

			Map<String, Object> result = new HashMap<>();
			result.put(outputKey, responseFlux);
			return result;
		}
	}


	public static final class AgentScopeAgentBuilder {

		private final ReActAgent.Builder reactAgentBuilder;
		private Session session;
		private String defaultSessionId;
		private String name;
		private String description;
		private String instruction;
		private boolean includeContents = true;
		private boolean returnReasoningContents = false;
		private String outputKey;
		private KeyStrategy outputKeyStrategy = KeyStrategy.REPLACE;
		private StateSerializer stateSerializer;

		private AgentScopeAgentBuilder(ReActAgent.Builder reactAgentBuilder) {
			this.reactAgentBuilder = Objects.requireNonNull(reactAgentBuilder, "reactAgentBuilder must not be null");
		}

		/**
		 * Set the session for loading and saving ReActAgent state per run. When set,
		 * state is loaded before each invocation and saved in doFinally after the run.
		 */
		public AgentScopeAgentBuilder session(Session session) {
			this.session = session;
			return this;
		}

		/**
		 * Set the default session id used when {@link RunnableConfig#threadId()} is empty.
		 */
		public AgentScopeAgentBuilder defaultSessionId(String defaultSessionId) {
			this.defaultSessionId = defaultSessionId;
			return this;
		}

		/**
		 * Set the instruction (optionally a template with placeholders like {@code {input}}).
		 * Rendered with state before being passed to the ReActAgent.
		 */
		public AgentScopeAgentBuilder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public AgentScopeAgentBuilder name(String name) {
			this.name = name;
			return this;
		}

		public AgentScopeAgentBuilder description(String description) {
			this.description = description;
			return this;
		}

		public AgentScopeAgentBuilder includeContents(boolean includeContents) {
			this.includeContents = includeContents;
			return this;
		}

		public AgentScopeAgentBuilder returnReasoningContents(boolean returnReasoningContents) {
			this.returnReasoningContents = returnReasoningContents;
			return this;
		}

		public AgentScopeAgentBuilder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public AgentScopeAgentBuilder outputKeyStrategy(KeyStrategy outputKeyStrategy) {
			this.outputKeyStrategy = outputKeyStrategy;
			return this;
		}

		public AgentScopeAgentBuilder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public AgentScopeAgent build() {
			return new AgentScopeAgent(reactAgentBuilder, this);
		}
	}
}
