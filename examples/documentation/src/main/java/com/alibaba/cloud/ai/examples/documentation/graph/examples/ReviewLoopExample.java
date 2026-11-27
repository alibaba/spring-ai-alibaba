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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReviewLoopExample {

	public static CompiledGraph createGraph() throws GraphStateException {
		return new StateGraph(() -> Map.of("reviews", new AppendStrategy()))
				.addNode("draft", node_async(state -> {
					int version = state.value("version", Integer.class).orElse(0) + 1;
					return Map.of("version", version, "draft", "Release note v" + version);
				}))
				.addNode("review", node_async(state -> {
					// A deterministic stand-in for a reviewer.
					int version = state.value("version", Integer.class).orElseThrow();
					return Map.of("accepted", version >= 2);
				}))
				.addEdge(START, "draft")
				.addEdge("draft", "review")
				.addConditionalEdges("review", AsyncCommandAction.node_async((state, config) -> {
					int version = state.value("version", Integer.class).orElseThrow();
					int budget = state.value("reviewBudget", Integer.class).orElseThrow();
					boolean accepted = state.value("accepted", Boolean.class).orElseThrow();
					if (accepted) {
						return new Command("finish", Map.of("status", "approved", "reviews", List.of("approved")));
					}
					if (version >= budget) {
						return new Command("finish",
								Map.of("status", "manual_review", "reviews", List.of("budget_exhausted")));
					}
					return new Command("revise", Map.of("reviews", List.of("revision_requested")));
				}), Map.of("revise", "draft", "finish", END))
				.compile(CompileConfig.builder().recursionLimit(12).build());
	}

	public static void main(String[] args) throws GraphStateException {
		var graph = createGraph();
		for (int budget : List.of(2, 1)) {
			var run = RunnableConfig.builder().threadId("review-budget-" + budget).build();
			var result = graph.invoke(Map.of("reviewBudget", budget), run).orElseThrow();
			System.out.printf("Budget %d: %s %s%n", budget, result.value("status").orElseThrow(),
					result.value("reviews").orElseThrow());
		}
	}

}
