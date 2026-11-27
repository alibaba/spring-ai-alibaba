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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class HumanInTheLoopExample {

	public static CompiledGraph createApprovalGraph(boolean dynamic) throws GraphStateException {
		var saver = SaverConfig.builder().register(MemorySaver.builder().build()).build();
		var compile = CompileConfig.builder().saverConfig(saver);
		var graph = new StateGraph()
				.addNode("prepare", node_async(state -> Map.of("draft", "Refund request")));
		if (dynamic) {
			graph.addNode("publish", new ApprovalAction());
		}
		else {
			graph.addNode("publish", node_async(HumanInTheLoopExample::publish));
			compile.interruptBefore("publish");
		}
		return graph.addEdge(START, "prepare").addEdge("prepare", "publish").addEdge("publish", END)
				.compile(compile.build());
	}

	public static String approve(CompiledGraph graph, RunnableConfig thread) throws Exception {
		var last = graph.stream(Map.of(), thread).blockLast(Duration.ofSeconds(5));
		if (!(last instanceof InterruptionMetadata interruption)) {
			throw new IllegalStateException("Expected approval to be pending");
		}
		System.out.println("Review: " + interruption.state().value("draft").orElseThrow());

		var updated = graph.updateState(thread, Map.of("decision", "approved"));
		return graph.invoke(Map.of(), updated.withResume()).orElseThrow()
				.value("status", String.class).orElseThrow();
	}

	private static Map<String, Object> publish(OverAllState state) {
		String decision = state.value("decision", String.class).orElseThrow();
		return Map.of("status", "approved".equals(decision) ? "published" : "rejected");
	}

	public static class ApprovalAction implements AsyncNodeActionWithConfig, InterruptableAction {

		@Override
		public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
			if (state.value("decision").isPresent()) {
				return Optional.empty();
			}
			return Optional.of(InterruptionMetadata.builder(nodeId, state).build());
		}

		@Override
		public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(publish(state));
		}

	}

	public static void main(String[] args) throws Exception {
		for (boolean dynamic : new boolean[] { false, true }) {
			var thread = RunnableConfig.builder().threadId("approval-" + dynamic).build();
			System.out.println("Status: " + approve(createApprovalGraph(dynamic), thread));
		}
	}

}
