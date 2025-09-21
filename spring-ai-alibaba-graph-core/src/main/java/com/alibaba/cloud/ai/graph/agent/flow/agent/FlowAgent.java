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
package com.alibaba.cloud.ai.graph.agent.flow.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;

import org.springframework.ai.chat.messages.UserMessage;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;

public abstract class FlowAgent extends Agent {

	protected KeyStrategyFactory keyStrategyFactory;

	protected List<Agent> subAgents;

	protected FlowAgent(String name, String description,
			KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig, List<Agent> subAgents)
			throws GraphStateException {
		super(name, description);
		this.compileConfig = compileConfig;
		this.keyStrategyFactory = keyStrategyFactory;
		this.subAgents = subAgents;
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		// Use FlowGraphBuilder to construct the graph
		FlowGraphBuilder.FlowGraphConfig config = FlowGraphBuilder.FlowGraphConfig.builder()
			.name(this.name())
			.keyStrategyFactory(keyStrategyFactory)
			.rootAgent(this)
			.subAgents(this.subAgents());

		// Delegate to specific graph builder based on agent type
		return buildSpecificGraph(config);
	}

	public Optional<OverAllState> invoke(String message) throws GraphRunnerException {
		return invoke(createInputMap(message));
	}

	public Optional<OverAllState> invoke(UserMessage message) throws GraphRunnerException {
		return invoke(createInputMap(message));
	}

	public Optional<OverAllState> invoke(String message, RunnableConfig runnableConfig) throws GraphRunnerException {
		return invoke(createInputMap(message), runnableConfig);
	}

	public Optional<OverAllState> invoke(UserMessage message, RunnableConfig runnableConfig) throws GraphRunnerException {
		return invoke(createInputMap(message), runnableConfig);
	}

	public Flux<NodeOutput> stream(String message) throws GraphRunnerException {
		return stream(createInputMap(message));
	}

	public Flux<NodeOutput> stream(UserMessage message) throws GraphRunnerException {
		return stream(createInputMap(message));
	}

	public Flux<NodeOutput> stream(String message, RunnableConfig runnableConfig) throws GraphRunnerException {
		return stream(createInputMap(message), runnableConfig);
	}

	public Flux<NodeOutput> stream(UserMessage message, RunnableConfig runnableConfig) throws GraphRunnerException {
		return stream(createInputMap(message), runnableConfig);
	}

	@Override
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.schedule(scheduleConfig);
	}

	public StateGraph asStateGraph(){
		return getGraph();
	}

	/**
	 * Abstract method for subclasses to specify their graph building strategy. This
	 * method should be implemented by concrete FlowAgent subclasses to define how their
	 * specific graph structure should be built.
	 * @param config the graph configuration
	 * @return the constructed StateGraph
	 * @throws GraphStateException if graph construction fails
	 */
	protected abstract StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config)
			throws GraphStateException;

	public CompileConfig compileConfig() {
		return compileConfig;
	}

	/**
	 * Gets the input keys with strategy factory for the agent.
	 * @return the input keys with strategy factory.
	 */

	public KeyStrategyFactory keyStrategyFactory() {
		return keyStrategyFactory;
	}

	public List<Agent> subAgents() {
		return this.subAgents;
	}

	/**
	 * Creates a map with messages and input for String message
	 */
	private Map<String, Object> createInputMap(String message) {
		return Map.of("messages", convertToMessages(message), "input", message);
	}

	/**
	 * Creates a map with messages and input for UserMessage
	 */
	private Map<String, Object> createInputMap(UserMessage message) {
		return Map.of("messages", convertToMessages(message), "input", message.getText());
	}


}
