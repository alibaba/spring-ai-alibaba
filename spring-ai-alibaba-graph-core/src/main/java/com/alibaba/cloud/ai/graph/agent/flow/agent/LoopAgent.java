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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 循环Agent，支持三种模式：限定次数、限定条件、迭代可叠代对象。输出元素为一个List，是每次循环之后最后一个子Agent的输出，对应的Key应为AppendStrategy
 */
public class LoopAgent extends FlowAgent {

	private final LoopConfig loopConfig;

	public static final String LOOP_CONFIG_KEY = "loopConfig";

	/**
	 * LoopAgent的子Agent为循环体，可以为一个SequentialAgent。若有多个Agent，则会组装成一个SequentialAgent进行处理
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
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = this.getAndCompileGraph();
		return compiledGraph.invoke(input);
	}

    public static Builder builder() {
        return new Builder();
    }

	public enum LoopMode {

		/**
		 * 限定次数
		 */
		COUNT((agentName, loopConfig) -> (state -> {
			String countKey = agentName + "__loop_count";
			int loopCount = state.value(countKey, 0);
			int maxCount = Optional.ofNullable(loopConfig.loopCount()).orElse(0);

			// 循环次数大于等于预设值，直接退出循环
			if (loopCount >= maxCount) {
				return Map.of(LoopMode.loopStartFlagKey(agentName), false);
			}

			// 将当前迭代次数进行更新，同时将当前迭代次数作为本次的iterator_item
			return Map.of(countKey, loopCount + 1, LoopMode.loopStartFlagKey(agentName), true, LoopMode.iteratorItemKey(agentName), String.valueOf(loopCount + 1));
		}), (agentName, loopConfig) -> (state -> {
            // 将结果放入outputKey中
            Optional<Object> value = state.value(iteratorResultKey(agentName));
            return value.map(o -> Map.of(loopConfig.outputKey(), o)).orElseGet(Map::of);
        }),
				(agentName) -> combineKeyStrategy(agentName,
						Map.of(agentName + "__loop_count", new ReplaceStrategy()))),

		/**
		 * 限定条件
		 */
		CONDITION((agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName) -> {
			return combineKeyStrategy(agentName, Map.of());
		}),

		/**
		 * 迭代可叠代对象
		 */
		ITERABLE((agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName) -> {
			return combineKeyStrategy(agentName, Map.of());
		}),

		/**
		 * 迭代数组对象
		 */
		ARRAY((agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName) -> {
			return combineKeyStrategy(agentName, Map.of());
		}),

		/**
		 * 迭代JSON数组
		 */
		JSON_ARRAY((agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName, loopConfig) -> (state -> {
			return Map.of();
		}), (agentName) -> {
			return combineKeyStrategy(agentName, Map.of());
		});

		private final BiFunction<String, LoopConfig, NodeAction> startActionFunc;

		private final BiFunction<String, LoopConfig, NodeAction> endActionFunc;

		private final Function<String, KeyStrategyFactory> loopTempKeyStrategyFactoryFunc;

		LoopMode(BiFunction<String, LoopConfig, NodeAction> startActionFunc,
				BiFunction<String, LoopConfig, NodeAction> endActionFunc,
				Function<String, KeyStrategyFactory> loopTempKeyStrategyFactoryFunc) {
			this.startActionFunc = startActionFunc;
			this.endActionFunc = endActionFunc;
			this.loopTempKeyStrategyFactoryFunc = loopTempKeyStrategyFactoryFunc;
		}

		public NodeAction getStartAction(String agentName, LoopConfig loopConfig) {
			return startActionFunc.apply(agentName, loopConfig);
		}

		public NodeAction getEndAction(String agentName, LoopConfig loopConfig) {
			return endActionFunc.apply(agentName, loopConfig);
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
	 * 循环配置类，用于封装循环相关的配置信息
	 *
     * @param inputKey 循环的输入Key，应符合loopMode的要求，部分Mode可以无输入
     * @param outputKey 循环的输出结果，为List对象
	 * @param loopMode 循环模式，决定循环的执行方式
	 * @param loopCount 循环次数，仅在COUNT模式下有效
	 * @param loopCondition 循环条件，仅在CONDITION模式下有效，每次循环都会根据该条件判断是否继续
	 */
	public record LoopConfig(String inputKey, String outputKey, LoopMode loopMode, Integer loopCount, Predicate<Object> loopCondition) {
		/**
		 * 验证循环配置的有效性
		 * @throws IllegalArgumentException 当配置不合法时抛出异常
		 */
		public void validate() {
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

		public Builder loopMode(LoopMode loopMode) {
			this.loopMode = loopMode;
			return self();
		}

		public Builder loopCount(Integer loopCount) {
			this.loopCount = loopCount;
			return self();
		}

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
			loopConfig = new LoopConfig(inputKey, outputKey, loopMode, loopCount, loopCondition);
			validate();
			return new LoopAgent(self());
		}

	}

}
