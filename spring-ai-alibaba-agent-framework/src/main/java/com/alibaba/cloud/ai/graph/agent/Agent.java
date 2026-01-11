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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;

import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import reactor.core.publisher.Flux;

import org.springframework.scheduling.Trigger;

import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;

/**
 * Abstract base class for all agents in the graph system. Contains common properties and
 * methods shared by different agent implementations.
 */
public abstract class Agent {

	/** The agent's name. Must be a unique identifier within the graph. */
	protected String name;

	/**
	 * One line description about the agent's capability. The system can use this for
	 * decision-making when delegating control to different agents.
	 */
	protected String description;

	protected CompileConfig compileConfig;

	protected volatile CompiledGraph compiledGraph;

	protected volatile StateGraph graph;

	protected Executor executor;

	/**
	 * Protected constructor for initializing all base agent properties.
	 * @param name the unique name of the agent
	 * @param description the description of the agent's capability
	 */
	protected Agent(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Default protected constructor for subclasses that need to initialize properties
	 * differently.
	 */
	protected Agent() {
		// Allow subclasses to initialize properties through other means
	}

	/**
	 * Gets the agent's unique name.
	 * @return the unique name of the agent.
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 * @return the description of the agent.
	 */
	public String description() {
		return description;
	}

	public StateGraph getGraph() {
		if (this.graph == null) {
			try {
				this.graph = initGraph();
			}
			catch (GraphStateException e) {
				throw new RuntimeException(e);
			}
		}
		return this.graph;
	}

	public synchronized CompiledGraph getAndCompileGraph() {
		if (compiledGraph != null) {
			return compiledGraph;
		}

		StateGraph graph = getGraph();
		try {
			if (this.compileConfig == null) {
				this.compiledGraph = graph.compile();
			}
			else {
				this.compiledGraph = graph.compile(this.compileConfig);
			}
		} catch (GraphStateException e) {
			throw new RuntimeException(e);
		}
		return this.compiledGraph;
	}

	/**
	 * Schedule the agent task with trigger.
	 * @param trigger the schedule configuration
	 * @param input the agent input
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(Trigger trigger, Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		ScheduleConfig scheduleConfig = ScheduleConfig.builder().trigger(trigger).inputs(input).build();
		return schedule(scheduleConfig);
	}

	/**
	 * Schedule the agent task with trigger.
	 * @param scheduleConfig the schedule configuration
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.schedule(scheduleConfig);
	}

	public StateSnapshot getCurrentState(RunnableConfig config) throws GraphRunnerException {
		return compiledGraph.getState(config);
	}

	// ------------------- Invoke with OverAllState as return value -------------------

	public Optional<OverAllState> invoke(String message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvoke(inputs, null);
	}

	public Optional<OverAllState> invoke(String message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvoke(inputs, config);
	}

	public Optional<OverAllState> invoke(UserMessage message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvoke(inputs, null);
	}

	public Optional<OverAllState> invoke(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvoke(inputs, config);
	}

	public Optional<OverAllState> invoke(List<Message> messages) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doInvoke(inputs, null);
	}

	public Optional<OverAllState> invoke(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doInvoke(inputs, config);
	}

	// ------------------- Invoke  methods with Output as return value -------------------

	public Optional<NodeOutput> invokeAndGetOutput(String message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvokeAndGetOutput(inputs, null);
	}

	public Optional<NodeOutput> invokeAndGetOutput(String message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvokeAndGetOutput(inputs, config);
	}

	public Optional<NodeOutput> invokeAndGetOutput(UserMessage message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvokeAndGetOutput(inputs, null);
	}

	public Optional<NodeOutput> invokeAndGetOutput(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doInvokeAndGetOutput(inputs, config);
	}

	public Optional<NodeOutput> invokeAndGetOutput(List<Message> messages) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doInvokeAndGetOutput(inputs, null);
	}

	public Optional<NodeOutput> invokeAndGetOutput(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doInvokeAndGetOutput(inputs, config);
	}

	// ------------------- Message Stream methods -------------------

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a plain text input.
	 * <p>
	 * This is a convenience API built on top of {@code stream(...)} that extracts
	 * and emits {@link Message} instances directly instead of low-level
	 * {@code NodeOutput} objects. It is intended for use cases that only care
	 * about the generated messages and do not require access to graph
	 * orchestration or node execution details.
	 *
	 * @param message the input message as plain text
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(String message) throws GraphRunnerException {
		return stream(message)
				.transform(this::extractMessages);
    }

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a plain text input and a custom {@link RunnableConfig}.
	 *
	 * @param message the input message as plain text
	 * @param config runtime configuration controlling execution behavior
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(String message, RunnableConfig config) throws GraphRunnerException {
		return stream(message, config)
				.transform(this::extractMessages);
    }

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a {@link UserMessage} as input.
	 *
	 * @param message the user message input
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(UserMessage message) throws GraphRunnerException {
		return stream(message)
				.transform(this::extractMessages);
    }

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a {@link UserMessage} as input and a custom {@link RunnableConfig}.
	 *
	 * @param message the user message input
	 * @param config runtime configuration controlling execution behavior
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		return stream(message, config)
				.transform(this::extractMessages);
    }

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a list of input {@link Message} instances.
	 *
	 * @param messages the input messages
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(List<Message> messages) throws GraphRunnerException {
		return stream(messages)
				.transform(this::extractMessages);
    }

	/**
	 * Streams the execution result as a {@link Flux} of {@link Message} objects
	 * using a list of input {@link Message} instances and a custom
	 * {@link RunnableConfig}.
	 *
	 * @param messages the input messages
	 * @param config runtime configuration controlling execution behavior
	 * @return a {@link Flux} emitting {@link Message} objects as they are produced
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public Flux<Message> streamMessages(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return stream(messages, config)
			.transform(this::extractMessages);
    }

	// ------------------- Stream methods -------------------

	public Flux<NodeOutput> stream(String message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doStream(inputs, buildStreamConfig(null));
	}

	public Flux<NodeOutput> stream(String message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doStream(inputs, config);
	}

	public Flux<NodeOutput> stream(UserMessage message) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doStream(inputs, buildStreamConfig(null));
	}

	public Flux<NodeOutput> stream(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(message);
		return doStream(inputs, config);
	}

	public Flux<NodeOutput> stream(List<Message> messages) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doStream(inputs, buildStreamConfig(null));
	}

	public Flux<NodeOutput> stream(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs = buildMessageInput(messages);
		return doStream(inputs, config);
	}

	protected Optional<OverAllState> doInvoke(Map<String, Object> input, RunnableConfig runnableConfig) {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.invoke(input, buildNonStreamConfig(runnableConfig));
	}

	protected Optional<NodeOutput> doInvokeAndGetOutput(Map<String, Object> input, RunnableConfig runnableConfig) {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.invokeAndGetOutput(input, buildNonStreamConfig(runnableConfig));
	}

	protected Flux<NodeOutput> doStream(Map<String, Object> input, RunnableConfig runnableConfig) {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.stream(input, buildStreamConfig(runnableConfig));
	}

	protected RunnableConfig buildNonStreamConfig(RunnableConfig config) {
		RunnableConfig.Builder builder = config == null 
			? RunnableConfig.builder() 
			: RunnableConfig.builder(config);
		
		builder.addMetadata("_stream_", false).addMetadata("_AGENT_", name);
		applyExecutorConfig(builder);
		
		return builder.build();
	}

	protected RunnableConfig buildStreamConfig(RunnableConfig config) {
		RunnableConfig.Builder builder = config == null 
			? RunnableConfig.builder() 
			: RunnableConfig.builder(config);
		
		builder.addMetadata("_AGENT_", name);
		applyExecutorConfig(builder);
		
		return builder.build();
	}

	/**
	 * Applies executor configuration to the RunnableConfig builder.
	 * This method sets the default executor for parallel nodes from the agent's configuration.
	 * @param builder the RunnableConfig builder to apply executor configuration to
	 */
	protected void applyExecutorConfig(RunnableConfig.Builder builder) {
		if (executor != null) {
			builder.defaultParallelExecutor(executor);
		}
	}

	protected Map<String, Object> buildMessageInput(Object message) {
		List<Message> messages;
		if (message instanceof List) {
			messages = (List<Message>) message;
		} else {
			messages = convertToMessages(message);
		}

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("messages", messages);

		UserMessage lastUserMessage = null;
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message msg = messages.get(i);
			if (msg instanceof UserMessage) {
				lastUserMessage = (UserMessage) msg;
				break;
			}
		}
		if (lastUserMessage != null) {
			inputs.put("input", lastUserMessage.getText());
		}
		return inputs;
	}

	protected abstract StateGraph initGraph() throws GraphStateException;

	/**
	 * Extracts {@link Message} objects from a stream of {@link NodeOutput}.
	 * <p>
	 * This helper method filters the incoming {@link NodeOutput} stream to retain only
	 * {@link StreamingOutput} instances whose {@link OutputType} is intended to expose
	 * messages at the Agent API level ({@code AGENT_MODEL_STREAMING} or
	 * {@code AGENT_TOOL_FINISHED}), and whose embedded {@link Message} is non-null.
	 * <p>
	 * All other {@link NodeOutput} types (such as tool or hook intermediate outputs)
	 * are intentionally filtered out to avoid leaking graph-level implementation
	 * details to Agent API consumers.
	 *
	 * @param stream the stream of {@link NodeOutput} produced during graph execution
	 * @return a {@link Flux} emitting only user-facing {@link Message} instances
	 */
	private Flux<Message> extractMessages(Flux<NodeOutput> stream) {
		return stream.filter(o -> o instanceof StreamingOutput<?> so
						&& isMessageOutputType(so.getOutputType())
						&& so.message() != null)
				.map(o -> ((StreamingOutput<?>) o).message());
	}

	/**
	 * Checks whether the given {@link OutputType} indicates a message-type output.
	 * <p>
	 * include {@link OutputType#AGENT_MODEL_STREAMING} and {@link OutputType#AGENT_TOOL_FINISHED}.
	 *
	 * @param type the {@link OutputType} to check
	 * @return true if the output type is a message-type output, false otherwise
	 */
	private boolean isMessageOutputType(OutputType type) {
		return type == OutputType.AGENT_MODEL_STREAMING
				|| type == OutputType.AGENT_TOOL_FINISHED;
	}

}
