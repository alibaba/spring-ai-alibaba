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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Iteration node that performs the same operation on all elements of a JSON array and saves the results in a JSON array. Both input and output JSON arrays are represented as JSON strings.
 * Node usage: IterationNode.Start -> SubStateGraphNode -> IterationNode.End
 *
 * @author vlsmb
 * @since 2025/7/19
 */
public class IterationNode {

	private static final Logger log = LoggerFactory.getLogger(IterationNode.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Starting node of iteration, reads one element from JSON array and passes it to the next node
	 *
	 * @param <ElementInput> type of input element
	 */
	public static class Start<ElementInput> implements NodeAction {

		/**
		 * Key of input JSON array, element type should be JSON string, strategy should be update
		 */
		private final String inputArrayJsonKey;

		/**
		 * Key to save the object to be iterated, element type should be List, strategy should be update
		 */
		private final String inputArrayKey;

		/**
		 * Key to save indexes still to be processed, type is List, strategy is update
		 */
		private final String taskIndexListKey;

		/**
		 * Key of element during iteration
		 */
		private final String outputItemKey;

		/**
		 * Key to output whether iteration is needed, type is Boolean
		 */
		private final String outputStartIterationKey;

		public Start(String inputArrayJsonKey, String inputArrayKey, String taskIndexListKey, String outputItemKey,
				String outputStartIterationKey) {
			this.inputArrayJsonKey = inputArrayJsonKey;
			this.inputArrayKey = inputArrayKey;
			this.taskIndexListKey = taskIndexListKey;
			this.outputItemKey = outputItemKey;
			this.outputStartIterationKey = outputStartIterationKey;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			try {
				List<ElementInput> list;
				List<Integer> indexes;
				// First iteration initialization, convert JSON string to List
				if (state.value(this.inputArrayKey, List.class).orElse(null) == null) {
					Object inputs = state.value(this.inputArrayJsonKey).orElse(null);
					if (inputs == null) {
						return Map.of(this.outputStartIterationKey, false);
					}
					// User input can be either List or JSON string

					if (inputs instanceof List) {
						list = List.copyOf((List<ElementInput>) inputs);
					}
					else {
						list = List
							.copyOf(OBJECT_MAPPER.readValue(inputs.toString(), new TypeReference<List<ElementInput>>() {
							}));
					}
					if (list.isEmpty()) {
						return Map.of(this.outputStartIterationKey, false);
					}
					indexes = new ArrayList<>();
					for (int i = 0; i < list.size(); i++) {
						indexes.add(i);
					}
				}
				else {
					// Read list and indexes from state
					list = (List<ElementInput>) state.value(this.inputArrayKey).orElseThrow();
					indexes = (List<Integer>) state.value(this.taskIndexListKey).orElseThrow();
				}

				if (indexes.isEmpty()) {
					return Map.of(this.outputStartIterationKey, false);
				}
				// Get the first element to process
				int index = indexes.get(0);
				indexes.remove(0);
				return Map.of(this.outputItemKey, list.get(index), this.outputStartIterationKey, true,
						this.taskIndexListKey, indexes, this.inputArrayKey, list);
			}
			catch (Exception e) {
				log.error("Iteration Start node error: {}", e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}

		public static class Builder<ElementInput> {

			private String inputArrayJsonKey;

			private String inputArrayKey;

			private String outputItemKey;

			private String outputStartIterationKey;

			private String taskIndexListKey;

			public Start<ElementInput> build() {
				return new Start<>(inputArrayJsonKey, inputArrayKey, taskIndexListKey, outputItemKey,
						outputStartIterationKey);
			}

			private Builder() {
				this.inputArrayJsonKey = null;
				this.inputArrayKey = null;
				this.outputItemKey = null;
				this.outputStartIterationKey = null;
				this.taskIndexListKey = null;
			}

			public Builder<ElementInput> inputArrayJsonKey(String inputArrayJsonKey) {
				this.inputArrayJsonKey = inputArrayJsonKey;
				return this;
			}

			public Builder<ElementInput> inputArrayKey(String inputArrayKey) {
				this.inputArrayKey = inputArrayKey;
				return this;
			}

			public Builder<ElementInput> outputItemKey(String outputItemKey) {
				this.outputItemKey = outputItemKey;
				return this;
			}

			public Builder<ElementInput> outputStartIterationKey(String outputStartIterationKey) {
				this.outputStartIterationKey = outputStartIterationKey;
				return this;
			}

			public Builder<ElementInput> taskIndexListKey(String taskIndexListKey) {
				this.taskIndexListKey = taskIndexListKey;
				return this;
			}

		}

	}

	/**
	 * Termination node of iteration, receives results processed during iteration and determines whether to jump back to start node
	 *
	 * @param <ElementInput> type of input element
	 * @param <ElementOutput> type of output element
	 */
	public static class End<ElementInput, ElementOutput> implements NodeAction {

		/**
		 * Key of the List of remaining element indexes
		 */
		private final String taskIndexSetKey;

		/**
		 * Key of result processed by subgraph node
		 */
		private final String inputResultKey;

		/**
		 * JSON result array output from entire iteration node processing, should be replace strategy
		 */
		private final String outputArrayJsonKey;

		/**
		 * Boolean value to output whether to continue iteration
		 */
		private final String outputContinueIterationKey;

		private final String outputStartIterationKey;

		public End(String taskIndexSetKey, String inputResultKey, String outputArrayJsonKey,
				String outputContinueIterationKey, String outputStartIterationKey) {
			this.taskIndexSetKey = taskIndexSetKey;
			this.inputResultKey = inputResultKey;
			this.outputArrayJsonKey = outputArrayJsonKey;
			this.outputContinueIterationKey = outputContinueIterationKey;
			this.outputStartIterationKey = outputStartIterationKey;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) throws Exception {
			try {
				List<ElementOutput> outputList = new ArrayList<>(
						OBJECT_MAPPER.readValue(state.value(this.outputArrayJsonKey, String.class).orElse("[]"),
								new TypeReference<List<ElementOutput>>() {
								}));
				// Check if it's an empty iteration node (i.e. outputStartIterationKey is false)
				if (!state.value(this.outputStartIterationKey, Boolean.class).orElse(false)) {
					return Map.of(this.outputContinueIterationKey, false, this.outputArrayJsonKey,
							OBJECT_MAPPER.writeValueAsString(outputList));
				}
				List<Integer> indexes = (List<Integer>) state.value(this.taskIndexSetKey, List.class).orElseThrow();
				ElementOutput result = (ElementOutput) state.value(this.inputResultKey).orElseThrow();
				// Add subgraph node processing result to final result array
				outputList.add(result);
				return Map.of(this.outputArrayJsonKey, OBJECT_MAPPER.writeValueAsString(outputList),
						this.outputContinueIterationKey, !indexes.isEmpty());
			}
			catch (Exception e) {
				log.error("Iteration End node error: {}", e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}

		public static class Builder<ElementInput, ElementOutput> {

			private String taskIndexListKey;

			private String inputResultKey;

			private String outputArrayKey;

			private String outputContinueIterationKey;

			private String outputStartIterationKey;

			private Builder() {
				this.taskIndexListKey = null;
				this.inputResultKey = null;
				this.outputArrayKey = null;
				this.outputContinueIterationKey = null;
				this.outputStartIterationKey = null;
			}

			public End<ElementInput, ElementOutput> build() {
				return new End<>(taskIndexListKey, inputResultKey, outputArrayKey, outputContinueIterationKey,
						outputStartIterationKey);
			}

			public Builder<ElementInput, ElementOutput> taskIndexListKey(String taskIndexListKey) {
				this.taskIndexListKey = taskIndexListKey;
				return this;
			}

			public Builder<ElementInput, ElementOutput> inputResultKey(String inputResultKey) {
				this.inputResultKey = inputResultKey;
				return this;
			}

			public Builder<ElementInput, ElementOutput> outputArrayKey(String outputArrayKey) {
				this.outputArrayKey = outputArrayKey;
				return this;
			}

			public Builder<ElementInput, ElementOutput> outputContinueIterationKey(String outputContinueIterationKey) {
				this.outputContinueIterationKey = outputContinueIterationKey;
				return this;
			}

			public Builder<ElementInput, ElementOutput> outputStartIterationKey(String outputStartIterationKey) {
				this.outputStartIterationKey = outputStartIterationKey;
				return this;
			}

		}

	}

	public static <ElementInput> Start.Builder<ElementInput> start() {
		return new Start.Builder<ElementInput>();
	}

	public static <ElementInput, ElementOutput> End.Builder<ElementInput, ElementInput> end() {
		return new End.Builder<ElementInput, ElementInput>();
	}

	/**
	 * Wrap iteration node as StateGraph, or add iteration node and conditional edges to existing StateGraph
	 *
	 * @param <ElementInput> iteration element input type
	 * @param <ElementOutput> iteration element output type
	 */
	public static class Converter<ElementInput, ElementOutput> {

		/**
		 * Key of input JSON array, element type should be JSON string, strategy should be update
		 */
		private String inputArrayJsonKey;

		/**
		 * JSON result array output from entire iteration node processing, should be replace strategy
		 */
		private String outputArrayJsonKey;

		/**
		 * Key of current iteration element in iteration subgraph
		 */
		private String iteratorItemKey;

		/**
		 * Key of processing result in iteration subgraph
		 */
		private String iteratorResultKey;

		/**
		 * Subgraph for single element operation
		 */
		private StateGraph subGraph = null;

		private String subGraphStartNodeName = null;

		private String subGraphEndNodeName = null;

		// Temporary variable names for iteration node

		private String tempArrayKey;

		private String tempIndexKey;

		private String tempStartFlagKey;

		private String tempEndFlagKey;

		public Converter<ElementInput, ElementOutput> inputArrayJsonKey(String inputArrayJsonKey) {
			this.inputArrayJsonKey = inputArrayJsonKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> outputArrayJsonKey(String outputArrayJsonKey) {
			this.outputArrayJsonKey = outputArrayJsonKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> iteratorItemKey(String iteratorItemKey) {
			this.iteratorItemKey = iteratorItemKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> iteratorResultKey(String iteratorResultKey) {
			this.iteratorResultKey = iteratorResultKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> subGraph(StateGraph subGraph) {
			this.subGraph = subGraph;
			return this;
		}

		public Converter<ElementInput, ElementOutput> subGraphStartNodeName(String subGraphStartNodeName) {
			this.subGraphStartNodeName = subGraphStartNodeName;
			return this;
		}

		public Converter<ElementInput, ElementOutput> subGraphEndNodeName(String subGraphEndNodeName) {
			this.subGraphEndNodeName = subGraphEndNodeName;
			return this;
		}

		public Converter<ElementInput, ElementOutput> tempArrayKey(String tempArrayKey) {
			this.tempArrayKey = tempArrayKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> tempIndexKey(String tempIndexKey) {
			this.tempIndexKey = tempIndexKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> tempStartFlagKey(String tempStartFlagKey) {
			this.tempStartFlagKey = tempStartFlagKey;
			return this;
		}

		public Converter<ElementInput, ElementOutput> tempEndFlagKey(String tempEndFlagKey) {
			this.tempEndFlagKey = tempEndFlagKey;
			return this;
		}

		/**
		 * Create a complete iteration graph (IterationNode.Start -> SubStateGraphNode -> IterationNode.End ->
		 * TempClear (clear temporary variable values during iteration) -> END) as a subgraph that can be nested by other graphs.
		 */
		public StateGraph convertToStateGraph() throws GraphStateException {
			if (!StringUtils.hasText(this.inputArrayJsonKey) || !StringUtils.hasText(this.outputArrayJsonKey)
					|| !StringUtils.hasText(this.iteratorItemKey) || !StringUtils.hasText(this.iteratorResultKey)
					|| this.subGraph == null) {
				throw new IllegalArgumentException("There are some empty fields");
			}
			if (!StringUtils.hasText(this.tempArrayKey)) {
				this.tempArrayKey = "input_array";
			}
			if (!StringUtils.hasText(this.tempStartFlagKey)) {
				this.tempStartFlagKey = "output_start";
			}
			if (!StringUtils.hasText(this.tempEndFlagKey)) {
				this.tempEndFlagKey = "output_continue";
			}
			if (!StringUtils.hasText(this.tempIndexKey)) {
				this.tempIndexKey = "iteration_index";
			}
			KeyStrategyFactory strategyFactory = () -> {
				Map<String, KeyStrategy> map = new HashMap<>();
				map.put(this.tempArrayKey, new ReplaceStrategy());
				map.put(this.inputArrayJsonKey, new ReplaceStrategy());
				map.put(this.iteratorItemKey, new ReplaceStrategy());
				map.put(this.tempStartFlagKey, new ReplaceStrategy());
				map.put(this.iteratorResultKey, new ReplaceStrategy());
				map.put(this.outputArrayJsonKey, new ReplaceStrategy());
				map.put(this.tempEndFlagKey, new ReplaceStrategy());
				map.put(this.tempIndexKey, new ReplaceStrategy());
				return map;
			};
			return new StateGraph("iteration_node", strategyFactory)
				.addNode("iteration_start",
						node_async(IterationNode.<ElementInput>start()
							.inputArrayJsonKey(this.inputArrayJsonKey)
							.inputArrayKey(this.tempArrayKey)
							.taskIndexListKey(this.tempIndexKey)
							.outputItemKey(this.iteratorItemKey)
							.outputStartIterationKey(this.tempStartFlagKey)
							.build()))
				.addNode("iteration", subGraph)
				.addNode("iteration_end",
						node_async(IterationNode.<ElementInput, ElementOutput>end()
							.taskIndexListKey(this.tempIndexKey)
							.inputResultKey(this.iteratorResultKey)
							.outputArrayKey(this.outputArrayJsonKey)
							.outputContinueIterationKey(this.tempEndFlagKey)
							.outputStartIterationKey(this.tempStartFlagKey)
							.build()))
				.addEdge(StateGraph.START, "iteration_start")
				.addConditionalEdges("iteration_start",
						edge_async(
								(OverAllState state) -> state.value(this.tempStartFlagKey, Boolean.class).orElse(false)
										? "true" : "false"),
						Map.of("true", "iteration", "false", "iteration_end"))
				.addEdge("iteration", "iteration_end")
				.addConditionalEdges("iteration_end",
						edge_async((OverAllState state) -> state.value(this.tempEndFlagKey, Boolean.class).orElse(false)
								? "true" : "false"),
						Map.of("true", "iteration_start", "false", StateGraph.END));
		}

		/**
		 * Add iteration Start and End nodes directly to existing StateGraph, only provide start and end node names of the subgraph for processing single element
		 * @param stateGraph the original stateGraph
		 * @param iterationName the name of iteration node
		 * @param iterationOutName the name of iteration node outgoing edge
		 */
		public void appendToStateGraph(StateGraph stateGraph, String iterationName, String iterationOutName)
				throws GraphStateException {
			if (!StringUtils.hasText(this.inputArrayJsonKey) || !StringUtils.hasText(this.outputArrayJsonKey)
					|| !StringUtils.hasText(this.iteratorItemKey) || !StringUtils.hasText(this.iteratorResultKey)
					|| !StringUtils.hasText(this.tempArrayKey) || !StringUtils.hasText(this.subGraphStartNodeName)
					|| !StringUtils.hasText(this.subGraphEndNodeName) || !StringUtils.hasText(this.tempStartFlagKey)
					|| !StringUtils.hasText(this.tempEndFlagKey) || stateGraph == null
					|| !StringUtils.hasText(this.tempIndexKey) || !StringUtils.hasText(iterationName)
					|| !StringUtils.hasText(iterationOutName)) {
				throw new IllegalArgumentException("There are some empty fields");
			}
			// Register replacement strategy for temporary variables
			stateGraph
				.addNode(iterationName,
						node_async(IterationNode.<ElementInput>start()
							.inputArrayJsonKey(this.inputArrayJsonKey)
							.taskIndexListKey(this.tempIndexKey)
							.inputArrayKey(this.tempArrayKey)
							.outputItemKey(this.iteratorItemKey)
							.outputStartIterationKey(this.tempStartFlagKey)
							.build()))
				.addNode(iterationName + "iteration_end",
						node_async(IterationNode.<ElementInput, ElementOutput>end()
							.taskIndexListKey(this.tempIndexKey)
							.inputResultKey(this.iteratorResultKey)
							.outputArrayKey(this.outputArrayJsonKey)
							.outputContinueIterationKey(this.tempEndFlagKey)
							.outputStartIterationKey(this.tempStartFlagKey)
							.build()))
				.addNode(iterationOutName, node_async((OverAllState state) -> Map.of()))
				.addConditionalEdges(iterationName,
						edge_async(
								(OverAllState state) -> state.value(this.tempStartFlagKey, Boolean.class).orElse(false)
										? "true" : "false"),
						Map.of("true", this.subGraphStartNodeName, "false", iterationName + "iteration_end"))
				.addEdge(this.subGraphEndNodeName, iterationName + "iteration_end")
				.addConditionalEdges(iterationName + "iteration_end",
						edge_async((OverAllState state) -> state.value(this.tempEndFlagKey, Boolean.class).orElse(false)
								? "true" : "false"),
						Map.of("true", iterationName, "false", iterationOutName));
		}

	}

	public static <ElementInput, ElementOutput> Converter<ElementInput, ElementOutput> converter() {
		return new Converter<ElementInput, ElementOutput>();
	}

}
