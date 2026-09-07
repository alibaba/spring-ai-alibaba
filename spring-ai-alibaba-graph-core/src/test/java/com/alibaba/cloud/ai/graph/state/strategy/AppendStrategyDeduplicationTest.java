/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.state.strategy;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AppendStrategyDeduplicationTest {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void scalarAndSingletonListShouldApplyTheSameDuplicatePolicy(boolean allowDuplicate) {
		AppendStrategy strategy = new AppendStrategy(allowDuplicate);
		List<String> oldValues = List.of("first", "second");
		String newValue = new String("second");

		Object scalarResult = strategy.apply(oldValues, newValue);
		Object listResult = strategy.apply(oldValues, List.of(newValue));

		assertEquals(allowDuplicate ? List.of("first", "second", "second") : List.of("first", "second"),
				scalarResult);
		assertEquals(listResult, scalarResult);
		assertEquals(List.of("first", "second"), oldValues);
	}

	@Test
	void scalarAppendShouldDeduplicateExistingValuesLikeListAppend() {
		AppendStrategy strategy = new AppendStrategy(false);
		List<String> oldValues = new ArrayList<>(List.of("first", "first", "second"));

		Object result = strategy.apply(oldValues, "third");

		assertEquals(List.of("first", "second", "third"), result);
		assertEquals(strategy.apply(oldValues, List.of("third")), result);
		assertEquals(List.of("first", "first", "second"), oldValues);
		assertNotSame(oldValues, result);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void newScalarShouldBeAppendedWithoutMutatingOldValues(boolean allowDuplicate) {
		List<String> oldValues = new ArrayList<>(List.of("first"));

		Object result = new AppendStrategy(allowDuplicate).apply(oldValues, "second");

		assertEquals(List.of("first", "second"), result);
		assertEquals(List.of("first"), oldValues);
		assertNotSame(oldValues, result);
	}

	@Test
	void shouldDeduplicateScalarWhenOldValuesAreWrappedInOptional() {
		Object result = new AppendStrategy(false).apply(Optional.of(List.of("first")), "first");

		assertEquals(List.of("first"), result);
	}

	@Test
	void defaultStrategyShouldContinueAllowingDuplicates() {
		assertEquals(List.of("first", "first"), new AppendStrategy().apply(List.of("first"), "first"));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void graphShouldHonorDuplicatePolicyForScalarNodeUpdates(boolean allowDuplicate) throws Exception {
		var graph = new StateGraph(KeyStrategy.builder()
				.addStrategy("results", new AppendStrategy(allowDuplicate))
				.build())
				.addNode("first", node_async(state -> Map.of("results", "answer")))
				.addNode("second", node_async(state -> Map.of("results", "answer")))
				.addEdge(START, "first")
				.addEdge("first", "second")
				.addEdge("second", END)
				.compile();

		var result = graph.invoke(Map.of()).orElseThrow();

		assertEquals(allowDuplicate ? List.of("answer", "answer") : List.of("answer"),
				result.value("results").orElseThrow());
	}

}
