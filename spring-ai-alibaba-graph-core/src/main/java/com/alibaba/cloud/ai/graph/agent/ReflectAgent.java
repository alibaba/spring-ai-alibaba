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
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReflectAgent {

	private static final Logger logger = LoggerFactory.getLogger(ReflectAgent.class);

	public static final String MESSAGES = "messages";

	public static final String ITERATION_NUM = "iteration_num";

	private final String REFLECTION_NODE_ID = "reflection";

	private final String GRAPH_NODE_ID = "graph";

	private int maxIterations;

	private NodeAction graph;

	private NodeAction reflection;

	private StateGraph stateGraph;

	private CompiledGraph compiledGraph;

	private CompileConfig compileConfig;

	private String name;

	public ReflectAgent(NodeAction graph, NodeAction reflection, int maxIterations) {
		this.graph = graph;
		this.reflection = reflection;
		this.maxIterations = maxIterations;
	}

	public ReflectAgent(String name, NodeAction graph, NodeAction reflection, int maxIterations,
			CompileConfig compileConfig) {
		this.name = name;
		this.graph = graph;
		this.reflection = reflection;
		this.maxIterations = maxIterations;
		this.compileConfig = compileConfig;
	}

	public StateGraph createReflectionGraph() throws GraphStateException {
		return createReflectionGraph(this.graph, this.reflection, this.maxIterations);
	}

	public StateGraph createReflectionGraph(NodeAction graph, NodeAction reflection) throws GraphStateException {
		return createReflectionGraph(graph, reflection, 5);
	}

	public StateGraph createReflectionGraph(NodeAction graph, NodeAction reflection, int maxIterations)
			throws GraphStateException {
		this.maxIterations = maxIterations;
		logger.debug("Creating reflection graph with max iterations: {}", maxIterations);

		StateGraph stateGraph = new StateGraph(() -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put(MESSAGES, new ReplaceStrategy());
			keyStrategyHashMap.put(ITERATION_NUM, new ReplaceStrategy());
			return keyStrategyHashMap;
		}).addNode(GRAPH_NODE_ID, node_async(graph))
			.addNode(REFLECTION_NODE_ID, node_async(reflection))
			.addEdge(START, GRAPH_NODE_ID)
			.addConditionalEdges(GRAPH_NODE_ID, edge_async(this::graphCount),
					Map.of(REFLECTION_NODE_ID, REFLECTION_NODE_ID, END, END))
			.addConditionalEdges(REFLECTION_NODE_ID, edge_async(this::apply),
					Map.of(GRAPH_NODE_ID, GRAPH_NODE_ID, END, END));

		logger.info("Reflection graph created successfully with {} nodes", 2);
		return stateGraph;
	}

	public StateGraph getStateGraph() throws GraphStateException {
		if (this.stateGraph == null) {
			this.stateGraph = createReflectionGraph();
		}
		return this.stateGraph;
	}

	public CompiledGraph getCompiledGraph() throws GraphStateException {
		if (this.compiledGraph == null) {
			getAndCompileGraph();
		}
		return this.compiledGraph;
	}

	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		this.compiledGraph = getStateGraph().compile(compileConfig);
		return this.compiledGraph;
	}

	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = getStateGraph().compile();
		}
		else {
			this.compiledGraph = getStateGraph().compile(this.compileConfig);
		}
		return this.compiledGraph;
	}

	public void printMessage(OverAllState state) {
		List<Message> messages = (List<Message>) state.value("messages").get();
		for (Message message : messages) {
			logger.info(message.getMessageType().name());
			logger.info(message.getText());
			logger.info("===================================");
		}
	}

	public String graphCount(OverAllState state) throws Exception {
		Optional<Object> iterationNumOptional = state.value(ITERATION_NUM);

		if (!iterationNumOptional.isPresent()) {
			logger.debug("Initializing iteration counter to 1");
			state.updateState(Map.of(ITERATION_NUM, 1));
		}
		else {
			Integer iterationNum = (Integer) iterationNumOptional.get();
			logger.info("Current iteration: {} | Max iterations: {}", iterationNum, maxIterations);

			if (iterationNum >= maxIterations) {
				logger.info("Iteration limit reached, stopping reflection cycle");
				state.updateState(Map.of(ITERATION_NUM, 0));
				this.printMessage(state);
				return END;
			}

			int nextIterationNum = iterationNum + 1;
			logger.debug("Incrementing iteration counter from {} to {}", iterationNum, nextIterationNum);
			state.updateState(Map.of(ITERATION_NUM, nextIterationNum));
		}

		Integer updatedCount = (Integer) state.value(ITERATION_NUM).orElseThrow();
		logger.debug("Updated iteration count: {}", updatedCount);

		return REFLECTION_NODE_ID;
	}

	public String apply(OverAllState state) throws Exception {
		List<Message> messages = (List<Message>) state.value(MESSAGES).get();
		int messageSize = messages.size();

		logger.debug("Processing messages, found {} messages in state", messageSize);

		if (messageSize == 0) {
			logger.info("No messages to process, ending reflection cycle");
			return END;
		}

		if (messages.get(messages.size() - 1).getMessageType().equals(MessageType.ASSISTANT)) {
			logger.info("Last message is from assistant: {}", messages.get(messages.size() - 1).getText());
			return END;
		}

		logger.debug("Last message is from user, continuing to graph node");
		return "graph";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name;

		private NodeAction graph;

		private NodeAction reflection;

		private int maxIterations = 5;

		private CompileConfig compileConfig;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder graph(NodeAction graph) {
			this.graph = graph;
			return this;
		}

		public Builder reflection(NodeAction reflection) {
			this.reflection = reflection;
			return this;
		}

		public Builder maxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
			return this;
		}

		public Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public ReflectAgent build() throws GraphStateException {
			if (graph == null || reflection == null) {
				throw new IllegalArgumentException("Graph and reflection must be provided");
			}
			return new ReflectAgent(name, graph, reflection, maxIterations, compileConfig);
		}

	}

}
