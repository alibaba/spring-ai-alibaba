/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Regression test for
 * <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4999">#4999</a>:
 * {@link CompiledGraph#cloneState(Map)} handed the compiled graph's live
 * {@code keyStrategyMap} to {@link OverAllState}, whose two-argument
 * constructor registers {@code input -> ReplaceStrategy} in place — silently
 * overwriting a user-registered strategy (e.g. {@link AppendStrategy}) for
 * every subsequent run and state-resume of that compiled graph.
 */
class CompiledGraphCloneStateStrategyTest {

	private static KeyStrategyFactory strategyFactory() {
		return KeyStrategy.builder()
			.addStrategy(OverAllState.DEFAULT_INPUT_KEY, new AppendStrategy())
			.addStrategy("messages", new ReplaceStrategy())
			.build();
	}

	@Test
	void cloneStateMustNotCorruptTheCompiledKeyStrategyMap() throws Exception {
		CompiledGraph compiled = new StateGraph(strategyFactory())
			.addEdge(StateGraph.START, StateGraph.END)
			.compile();

		assertEquals(AppendStrategy.class, compiled.getKeyStrategyMap().get(OverAllState.DEFAULT_INPUT_KEY).getClass(),
				"precondition: the user-registered strategy is active before any clone");

		compiled.cloneState(Map.of(OverAllState.DEFAULT_INPUT_KEY, List.of("v1")));

		assertInstanceOf(AppendStrategy.class, compiled.getKeyStrategyMap().get(OverAllState.DEFAULT_INPUT_KEY),
				"cloneState must not overwrite the user-registered strategy held by the compiled graph");
	}

	@Test
	void cloneAndSnapShotPreserveTheUserInputStrategy() throws Exception {
		CompiledGraph compiled = new StateGraph(strategyFactory())
			.addEdge(StateGraph.START, StateGraph.END)
			.compile();

		OverAllState cloned = compiled.cloneState(Map.of(OverAllState.DEFAULT_INPUT_KEY, List.of("v1")));

		assertInstanceOf(AppendStrategy.class, cloned.keyStrategies().get(OverAllState.DEFAULT_INPUT_KEY),
				"the cloned state must keep the user-registered strategy for 'input'");

		cloned.updateState(Map.of(OverAllState.DEFAULT_INPUT_KEY, "v2"));
		assertEquals(List.of("v1", "v2"), cloned.value(OverAllState.DEFAULT_INPUT_KEY).orElseThrow(),
				"updates on the cloned state must follow the user-registered append semantics");

		// Parallel-branch snapshots go through the three-argument constructor.
		OverAllState snapshot = cloned.snapShot().orElseThrow();
		assertInstanceOf(AppendStrategy.class, snapshot.keyStrategies().get(OverAllState.DEFAULT_INPUT_KEY),
				"snapshots must keep the user-registered strategy for 'input'");
	}

}
