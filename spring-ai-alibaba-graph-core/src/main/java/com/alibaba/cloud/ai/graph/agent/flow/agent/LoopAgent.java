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

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Loop Agent that supports multiple loop modes:
 * <ul>
 * <li><b>COUNT</b>: Execute a fixed number of loops</li>
 * <li><b>CONDITION</b>: Continue looping based on a condition, similar to a do-while
 * structure, but when the condition is true, terminate the loop</li>
 * <li><b>ITERABLE</b>: Iterate over each element in an Iterable object</li>
 * <li><b>ARRAY</b>: Iterate over each element in an array</li>
 * <li><b>JSON_ARRAY</b>: Parse a JSON array and iterate over its elements</li>
 * </ul>
 *
 * <p>
 * The output result is a List containing the output of the last sub-agent after each loop
 * iteration. Note: The strategy corresponding to outputKey should be set to
 * AppendStrategy to correctly collect loop results.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * LoopAgent loopAgent = LoopAgent.builder()
 *     .name("example-loop-agent")
 *     .description("Example loop agent")
 *     .inputKey("input")
 *     .outputKey("results")
 *     .loopMode(LoopAgent.LoopMode.COUNT)
 *     .loopCount(3)
 *     .subAgents(subAgents)
 *     .build();
 * }</pre>
 *
 * @author vlsmb
 * @since 2025/8/25
 */
public class LoopAgent extends FlowAgent {

	private final LoopConfig loopConfig;

	public static final String LOOP_CONFIG_KEY = "loopConfig";

	public static final int ITERABLE_ELEMENT_COUNT = 10000;

	/**
	 * The sub-agents of LoopAgent constitute the loop body, which can be one or more
	 * agents. When building the graph, the agents will be connected head-to-tail.
	 */
	private LoopAgent(Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.loopConfig = builder.loopConfig;
		this.graph = this.initGraph();
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		config.customProperty(LOOP_CONFIG_KEY, this.loopConfig);
		return FlowGraphBuilder.buildGraph(FlowAgentEnum.LOOP.getType(), config);
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input, RunnableConfig runnableConfig)
			throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = this.getAndCompileGraph();
		// Initialize outputKey as an empty list to collect loop results
		return compiledGraph.invoke(Stream.of(input, Map.of(this.outputKey(), new ArrayList<>()))
			.map(Map::entrySet)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue)),
				runnableConfig);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Loop mode enumeration that defines different loop execution methods
	 */
	public enum LoopMode {

		/**
		 * <b>Count mode</b>: Execute a fixed number of loops
		 * <p>
		 * Execution steps:
		 * <ol>
		 * <li>Get input data from inputKey (optional)</li>
		 * <li>Track the current loop count</li>
		 * <li>Increment the count by 1 for each loop until reaching the preset loopCount
		 * value</li>
		 * <li>Set loopStartFlag to false when the loop ends</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Applicable scenario: Scenarios requiring a fixed number of operations
		 * </p>
		 */
		COUNT(loopConfig -> (state -> {
			String agentName = loopConfig.agentName();
			// Get input for passing to the iterator body
			Optional<?> input = state.value(loopConfig.inputKey());

			// Get current iteration count
			String countKey = agentName + "__loop_count";
			int loopCount = state.value(countKey, 0);
			int maxCount = Optional.ofNullable(loopConfig.loopCount()).orElse(0);

			// If loop count is greater than or equal to the preset value, exit the loop
			// directly
			if (loopCount >= maxCount) {
				return Map.of(LoopMode.loopStartFlagKey(agentName), false);
			}

			// Update the current iteration count and return
			return input
				.map(o -> Map.of(countKey, loopCount + 1, LoopMode.loopStartFlagKey(agentName), true,
						LoopMode.iteratorItemKey(agentName), o))
				.orElseGet(() -> Map.of(countKey, loopCount + 1, LoopMode.loopStartFlagKey(agentName), true));
		}), loopConfig -> (state -> {
			// Put the result into outputKey
			Optional<Object> value = state.value(iteratorResultKey(loopConfig.agentName()));
			return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
		}), (agentName) -> combineKeyStrategy(agentName, Map.of(agentName + "__loop_count", new ReplaceStrategy()))),

		/**
		 * <b>Condition mode</b>: Continue looping based on a condition, similar to a
		 * do-while structure, but when the condition is true, terminate the loop
		 * <p>
		 * Working principle:
		 * <ol>
		 * <li>Execute the first loop directly (unconditionally)</li>
		 * <li>Subsequent loops check if the previous output meets the loopCondition</li>
		 * <li>Exit the loop if the condition is met, otherwise continue executing</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Applicable scenario: Scenarios where the decision to continue looping needs to
		 * be made dynamically based on execution results
		 * </p>
		 */
		CONDITION(loopConfig -> (state -> {
			String agentName = loopConfig.agentName();
			// Get input for passing to the iterator body
			Optional<?> input = state.value(loopConfig.inputKey());
			// Check if it's the first loop, if so, allow it to proceed directly
			if (state.value(loopStartFlagKey(agentName)).isEmpty()) {
				return input.map(o -> Map.of(loopStartFlagKey(agentName), true, iteratorItemKey(agentName), o))
					.orElseGet(() -> Map.of(loopStartFlagKey(agentName), true));
			}

			// Get current iteration result
			Object result = state.value(iteratorResultKey(agentName)).orElse(null);
			if (loopConfig.loopCondition.test(result)) {
				return Map.of(LoopMode.loopStartFlagKey(agentName), false);
			}
			// Continue retrying
			return input.map(o -> Map.of(loopStartFlagKey(agentName), true, iteratorItemKey(agentName), o))
				.orElseGet(() -> Map.of(loopStartFlagKey(agentName), true));
		}), loopConfig -> (state -> {
			// Put the result into outputKey
			Optional<Object> value = state.value(iteratorResultKey(loopConfig.agentName()));
			return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
		}), (agentName) -> combineKeyStrategy(agentName, Map.of())),

		/**
		 * <b>Iterable mode</b>: Iterate over each element in an Iterable object
		 * <p>
		 * Working principle:
		 * <ol>
		 * <li>Get the Iterable object from inputKey</li>
		 * <li>Convert to List and limit the maximum number of elements
		 * (ITERABLE_ELEMENT_COUNT)</li>
		 * <li>Track the current index</li>
		 * <li>Extract each element sequentially as input to the loop body</li>
		 * <li>Exit the loop after iterating through all elements</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Applicable scenario: Scenarios requiring iteration over each element in a
		 * collection or list
		 * </p>
		 */
		ITERABLE(loopConfig -> (state -> {
			String agentName = loopConfig.agentName();
			// Get the elements to be iterated over. If they don't exist, it means it's
			// the first loop execution, so get the input first
			String iteratorKey = agentName + "__iterableElement";
			Optional<Object> iteratorObj = state.value(iteratorKey);
			List<?> iteratorElement;

			if (iteratorObj.isEmpty()) {
				// Get output
				Optional<?> inputIterable = state.value(loopConfig.inputKey());
				if (inputIterable.isEmpty()) {
					return Map.of(loopStartFlagKey(agentName), false);
				}
				Object iterableObj = inputIterable.get();
				if (!(iterableObj instanceof Iterable<?> iterable)) {
					throw new IllegalStateException("Input iterable is not iterable");
				}
				// Convert Iterable to List and limit the maximum number of elements
				iteratorElement = StreamSupport.stream(iterable.spliterator(), false)
					.limit(ITERABLE_ELEMENT_COUNT)
					.toList();
			}
			else {
				iteratorElement = (List<?>) iteratorObj.get();
			}

			// Get current iteration index
			String indexKey = agentName + "__iterableIndex";
			int index = state.value(indexKey, 0);

			// Check if there is a next element, and if so, get the next element
			if (index < iteratorElement.size()) {
				Object next = iteratorElement.get(index);
				return Map.of(iteratorItemKey(agentName), next, loopStartFlagKey(agentName), true, iteratorKey,
						iteratorElement, indexKey, index + 1);
			}
			else {
				return Map.of(loopStartFlagKey(agentName), false);
			}
		}), loopConfig -> (state -> {
			// Put the result into outputKey
			Optional<Object> value = state.value(iteratorResultKey(loopConfig.agentName()));
			return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
		}), (agentName) -> combineKeyStrategy(agentName,
				Map.of(agentName + "__iterableElement", new ReplaceStrategy(), agentName + "__iterableIndex",
						new ReplaceStrategy()))),

		/**
		 * <b>Array mode</b>: Iterate over each element in an array
		 * <p>
		 * Working principle:
		 * <ol>
		 * <li>Get the array object from inputKey</li>
		 * <li>Track the current index</li>
		 * <li>Use reflection to get each element in the array sequentially</li>
		 * <li>Exit the loop after iterating through all elements</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Applicable scenario: Scenarios requiring iteration over each element in a Java
		 * array
		 * </p>
		 */
		ARRAY(loopConfig -> (state -> {
			String agentName = loopConfig.agentName();
			// Get the input array
			Object arrayObj = state.value(loopConfig.inputKey()).orElse(null);
			if (arrayObj == null) {
				return Map.of(loopStartFlagKey(agentName), false);
			}
			if (!arrayObj.getClass().isArray()) {
				throw new IllegalStateException("Input array is not an array");
			}

			// Get current iteration index
			String indexKey = agentName + "__arrayIndex";
			int index = state.value(indexKey, 0);
			int length = Array.getLength(arrayObj);
			if (index >= length) {
				return Map.of(loopStartFlagKey(agentName), false);
			}
			Object obj = Array.get(arrayObj, index);
			return Map.of(loopStartFlagKey(agentName), true, iteratorItemKey(agentName), obj, indexKey, index + 1);
		}), loopConfig -> (state -> {
			// Put the result into outputKey
			Optional<Object> value = state.value(iteratorResultKey(loopConfig.agentName()));
			return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
		}), (agentName) -> combineKeyStrategy(agentName, Map.of(agentName + "__arrayIndex", new ReplaceStrategy()))),

		/**
		 * <b>JSON array mode</b>: Parse a JSON array and iterate over its elements
		 * <p>
		 * Working principle:
		 * <ol>
		 * <li>Get the JSON string from inputKey</li>
		 * <li>Parse it into a List object</li>
		 * <li>Track the current index</li>
		 * <li>Extract each element sequentially as input to the loop body</li>
		 * <li>Exit the loop after iterating through all elements</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Applicable scenario: Scenarios requiring processing of JSON format array data
		 * </p>
		 */
		JSON_ARRAY(loopConfig -> (state -> {
			String agentName = loopConfig.agentName();
			String listKey = agentName + "__list";
			// Try to get the list of iteration elements. If it's the first execution,
			// initialize from the JSON string
			List<?> list;
			Optional<Object> listObj = state.value(listKey);
			if (listObj.isEmpty()) {
				// Get the input array
				String jsonStr = state.value(loopConfig.inputKey()).orElse("[]").toString();
				try {
					list = JsonParser.fromJson(jsonStr, List.class);
				}
				catch (Exception e) {
					throw new IllegalStateException("Input json array is not a json array");
				}
			}
			else {
				list = (List<?>) listObj.get();
			}

			// Get current iteration index
			String indexKey = agentName + "__jsonIndex";
			int index = state.value(indexKey, 0);
			if (index >= list.size()) {
				return Map.of(loopStartFlagKey(agentName), false);
			}
			Object obj = list.get(index);
			return Map.of(loopStartFlagKey(agentName), true, iteratorItemKey(agentName), obj, indexKey, index + 1,
					listKey, list);
		}), loopConfig -> (state -> {
			// Put the result into outputKey
			Optional<Object> value = state.value(iteratorResultKey(loopConfig.agentName()));
			return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
		}), (agentName) -> combineKeyStrategy(agentName,
				Map.of(agentName + "__jsonIndex", new ReplaceStrategy(), agentName + "__list", new ReplaceStrategy())));

		/**
		 * Get the corresponding Start node based on LoopConfig and LoopMode
		 */
		private final Function<LoopConfig, NodeAction> startActionFunc;

		/**
		 * Get the corresponding End node based on LoopConfig and LoopMode
		 */
		private final Function<LoopConfig, NodeAction> endActionFunc;

		/**
		 * Get the KeyStrategy required for LoopStart and LoopEnd nodes based on LoopMode
		 */
		private final Function<String, KeyStrategyFactory> loopTempKeyStrategyFactoryFunc;

		LoopMode(Function<LoopConfig, NodeAction> startActionFunc, Function<LoopConfig, NodeAction> endActionFunc,
				Function<String, KeyStrategyFactory> loopTempKeyStrategyFactoryFunc) {
			this.startActionFunc = startActionFunc;
			this.endActionFunc = endActionFunc;
			this.loopTempKeyStrategyFactoryFunc = loopTempKeyStrategyFactoryFunc;
		}

		public NodeAction getStartAction(LoopConfig loopConfig) {
			return startActionFunc.apply(loopConfig);
		}

		public NodeAction getEndAction(LoopConfig loopConfig) {
			return endActionFunc.apply(loopConfig);
		}

		public KeyStrategyFactory getLoopTempKeyStrategyFactory(String agentName) {
			return loopTempKeyStrategyFactoryFunc.apply(agentName);
		}

		private static KeyStrategyFactory combineKeyStrategy(String agentName, Map<String, KeyStrategy> strategyMap) {
			return new KeyStrategyFactoryBuilder().addStrategies(strategyMap)
				.addStrategy(loopStartFlagKey(agentName), new ReplaceStrategy())
				.addStrategy(iteratorItemKey(agentName), new ReplaceStrategy())
				.addStrategy(iteratorResultKey(agentName), new ReplaceStrategy())
				.build();
		}

		public static String loopStartFlagKey(String agentName) {
			return agentName + "__loop_start_flag";
		}

		public static String iteratorItemKey(String agentName) {
			return agentName + "__iterator_item";
		}

		public static String iteratorResultKey(String agentName) {
			return agentName + "__iterator_result";
		}

	}

	/**
	 * Loop configuration class for encapsulating loop-related configuration information
	 *
	 * @param agentName The name of the agent
	 * @param inputKey The input key for the loop, should conform to the requirements of
	 * loopMode. Some modes can have no input.
	 * @param outputKey The loop output result, which is a List object
	 * @param loopMode The loop mode that determines how the loop is executed
	 * @param loopCount The number of loops, only valid in COUNT mode
	 * @param loopCondition The loop condition, only valid in CONDITION mode. The
	 * condition is checked for continuation on each loop iteration.
	 */
	public record LoopConfig(String agentName, String inputKey, String outputKey, LoopMode loopMode, Integer loopCount,
			Predicate<Object> loopCondition) {
		/**
		 * Validate the validity of the loop configuration
		 * @throws IllegalArgumentException Thrown when the configuration is invalid
		 */
		public void validate() {
			if (!StringUtils.hasText(agentName)) {
				throw new IllegalArgumentException("AgentName must be provided");
			}
			if (loopMode == null) {
				throw new IllegalArgumentException("Must choose a LoopMode");
			}
			if (loopMode == LoopMode.COUNT && loopCount == null) {
				throw new IllegalArgumentException("LoopCount must be provided for COUNT loop mode");
			}
			if (loopMode == LoopMode.CONDITION && loopCondition == null) {
				throw new IllegalArgumentException("LoopCondition must be provided for CONDITION loop mode");
			}
		}
	}

	public static class Builder extends FlowAgentBuilder<LoopAgent, Builder> {

		private LoopMode loopMode;

		private Integer loopCount;

		private Predicate<Object> loopCondition;

		private LoopConfig loopConfig;

		/**
		 * Set the loop mode
		 * @param loopMode The loop mode enumeration value
		 * @return The builder instance
		 */
		public Builder loopMode(LoopMode loopMode) {
			this.loopMode = loopMode;
			return self();
		}

		/**
		 * Set the loop count (only valid in COUNT mode)
		 * @param loopCount The number of loops
		 * @return The builder instance
		 */
		public Builder loopCount(Integer loopCount) {
			this.loopCount = loopCount;
			return self();
		}

		/**
		 * Set the loop condition (only valid in CONDITION mode)
		 * @param loopCondition The condition checking function
		 * @return The builder instance
		 */
		public Builder loopCondition(Predicate<Object> loopCondition) {
			this.loopCondition = loopCondition;
			return self();
		}

		@Override
		protected void validate() {
			super.validate();
			loopConfig.validate();
		}

		@Override
		protected Builder self() {
			return this;
		}

		@Override
		public LoopAgent build() throws GraphStateException {
			loopConfig = new LoopConfig(name, inputKey, outputKey, loopMode, loopCount, loopCondition);
			validate();
			return new LoopAgent(self());
		}

	}

}
