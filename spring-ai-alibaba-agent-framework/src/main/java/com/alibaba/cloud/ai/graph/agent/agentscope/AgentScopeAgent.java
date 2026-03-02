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
package com.alibaba.cloud.ai.graph.agent.agentscope;

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
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

/**
 * Spring AI Alibaba agent that proxies an AgentScope {@link ReActAgent}.
 * <p>
 * AgentScopeAgent wraps an {@code io.agentscope.core.ReActAgent} so it can be used
 * as a {@link BaseAgent} in graph workflows. Call, invoke, and stream are delegated
 * to the underlying graph (START → self → END), whose single node delegates to the
 * ReActAgent.
 */
public class AgentScopeAgent extends BaseAgent {

	private static final String AGENT_NODE_ID = "agent";

	private final ReActAgent reactAgent;

	private final StateSerializer stateSerializer;

	private final String instruction;

	public AgentScopeAgent(ReActAgent reactAgent, Builder builder) {
		super(
				builder.name != null ? builder.name : reactAgent.getName(),
				builder.description != null ? builder.description : reactAgent.getDescription(),
				builder.includeContents,
				builder.returnReasoningContents,
				builder.outputKey != null ? builder.outputKey : "messages",
				builder.outputKeyStrategy != null ? builder.outputKeyStrategy : KeyStrategy.REPLACE);
		this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
		this.stateSerializer = builder.stateSerializer != null
				? builder.stateSerializer
				: new SpringAIJacksonStateSerializer(OverAllState::new);
		this.instruction = builder.instruction;
	}

	/**
	 * Create a builder that wraps the given AgentScope ReActAgent.
	 * @param agent the AgentScope ReActAgent to proxy
	 * @return a new builder
	 */
	public static Builder fromAgent(ReActAgent agent) {
		return new Builder(agent);
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
				Map.of("messages", List.of(org.springframework.ai.chat.messages.UserMessage.builder().text(message).build()), "input", message),
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
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new SubCompiledGraphNode(this.name, this.compiledGraph);
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		StateGraph graph = new StateGraph(name, buildKeyStrategyFactory(), stateSerializer);
		graph.addNode(AGENT_NODE_ID, node_async(new ReActAgentNodeAction(reactAgent, outputKey, returnReasoningContents, includeContents, instruction, AGENT_NODE_ID, this.name)));
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
	 * GraphResponse. Uses returnReasoningContents to decide whether to emit all isLast
	 * events as messages or only the final AGENT_RESULT.
	 */
	private static final class ReActAgentNodeAction implements NodeActionWithConfig {

		private final ReActAgent reactAgent;
		private final String outputKey;
		private final boolean returnReasoningContents;
		private final boolean includeContents;
		private final String instruction;
		private final String nodeId;
		private final String agentName;

		ReActAgentNodeAction(ReActAgent reactAgent, String outputKey, boolean returnReasoningContents,
				boolean includeContents, String instruction, String nodeId, String agentName) {
			this.reactAgent = reactAgent;
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
			List<Message> out = new ArrayList<>(stateMessages);

			Message instructionMessage = null;
			if (StringUtils.hasLength(instruction)) {
				String renderedText = renderInstructionTemplate(instruction, state);
				instructionMessage = new UserMessage(renderedText);
			}

			if (!includeContents) {
				return instructionMessage != null ? List.of(instructionMessage) : List.of();
			}
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
			@SuppressWarnings("unchecked")
			List<Message> stateMessages = (List<Message>) state.value("messages").orElse(List.of());
			List<Message> messages = buildMessagesForAgent(state, stateMessages);
			List<Msg> agentScopeMsgs = AgentScopeMessageUtils.toAgentScopeMessages(messages);
			Flux<Event> eventFlux = reactAgent.stream(agentScopeMsgs, StreamOptions.defaults());

			// Collect isLast events for building final done state
			List<Event> isLastEvents = new ArrayList<>();

			Flux<GraphResponse<NodeOutput>> responseFlux = eventFlux
					.concatMap(event -> {
						if (!event.isLast()) {
							// Not isLast: convert to Message and output directly in stream
							Message msg = AgentScopeMessageUtils.toMessage(event.getMessage());
							if (msg == null) {
								return Flux.empty();
							}
							StreamingOutput<?> so = new StreamingOutput<>(msg, nodeId, agentName, state,
									OutputType.AGENT_MODEL_STREAMING);
							return Flux.just(GraphResponse.of((NodeOutput) so));
						}
						// isLast: collect and handle by returnReasoningContents
						isLastEvents.add(event);
						if (returnReasoningContents) {
							// returnReasoningContents==true: emit this message and include in final list
							Message msg = AgentScopeMessageUtils.toMessage(event.getMessage());
							if (msg == null) {
								return Flux.empty();
							}
							StreamingOutput<?> so = new StreamingOutput<>(msg, nodeId, agentName, state,
									OutputType.AGENT_MODEL_STREAMING);
							return Flux.just(GraphResponse.of((NodeOutput) so));
						}
						// returnReasoningContents==false: only emit when AGENT_RESULT
						if (event.getType() == EventType.AGENT_RESULT) {
							AssistantMessage am = AgentScopeMessageUtils.toAssistantMessage(event.getMessage());
							StreamingOutput<?> so = new StreamingOutput<>(am != null ? am : new AssistantMessage(""), nodeId, agentName, state,
									OutputType.AGENT_MODEL_FINISHED);
							return Flux.just(GraphResponse.of((NodeOutput) so));
						}
						return Flux.empty();
					})
					.concatWith(Flux.defer(() -> {
						// Build final done state from collected isLast events
						if (returnReasoningContents) {
							List<Message> allMessages = new ArrayList<>();
							for (Event e : isLastEvents) {
								Message m = AgentScopeMessageUtils.toMessage(e.getMessage());
								if (m != null) {
									allMessages.add(m);
								}
							}
							Map<String, Object> doneState = new HashMap<>();
							doneState.put(outputKey, allMessages);
							return Flux.just(GraphResponse.<NodeOutput>done(doneState));
						}
						// returnReasoningContents==false: only last AGENT_RESULT
						Event lastAgentResult = null;
						for (int i = isLastEvents.size() - 1; i >= 0; i--) {
							if (isLastEvents.get(i).getType() == EventType.AGENT_RESULT) {
								lastAgentResult = isLastEvents.get(i);
								break;
							}
						}
						if (lastAgentResult == null) {
							return Flux.just(GraphResponse.done(Map.of(outputKey, List.<Message>of())));
						}
						AssistantMessage am = AgentScopeMessageUtils.toAssistantMessage(lastAgentResult.getMessage());
						Map<String, Object> doneState = new HashMap<>();
						doneState.put(outputKey, am != null ? am : new AssistantMessage(""));
						return Flux.just(GraphResponse.done(doneState));
					}));

			Map<String, Object> result = new HashMap<>();
			result.put(outputKey, responseFlux);
			return result;
		}
	}


	public static final class Builder {

		private final ReActAgent reactAgent;
		private String name;
		private String description;
		private String instruction;
		private boolean includeContents = true;
		private boolean returnReasoningContents = false;
		private String outputKey = "messages";
		private KeyStrategy outputKeyStrategy = KeyStrategy.REPLACE;
		private StateSerializer stateSerializer;

		private Builder(ReActAgent reactAgent) {
			this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
		}

		/**
		 * Set the instruction (optionally a template with placeholders like {@code {input}}).
		 * Rendered with state before being passed to the ReActAgent.
		 */
		public Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder includeContents(boolean includeContents) {
			this.includeContents = includeContents;
			return this;
		}

		public Builder returnReasoningContents(boolean returnReasoningContents) {
			this.returnReasoningContents = returnReasoningContents;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputKeyStrategy(KeyStrategy outputKeyStrategy) {
			this.outputKeyStrategy = outputKeyStrategy;
			return this;
		}

		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public AgentScopeAgent build() {
			return new AgentScopeAgent(reactAgent, this);
		}
	}
}
