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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class PersistenceExample {

	public static CompiledGraph createGraph() throws GraphStateException {
		var saver = SaverConfig.builder().register(MemorySaver.builder().build()).build();
		return new StateGraph(() -> Map.of("events", new AppendStrategy()))
				.addNode("review", node_async(state -> {
					var decision = state.value("decision", String.class).orElseThrow();
					return Map.of("events", List.of(decision));
				}))
				.addEdge(START, "review")
				.addEdge("review", END)
				.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("review").build());
	}

	public static void inspectAndResume() throws Exception {
		var graph = createGraph();
		var thread = RunnableConfig.builder().threadId("approval-1").checkpointsNumRetained(20).build();

		graph.stream(Map.of("events", List.of("submitted")), thread).blockLast();
		var saved = graph.getState(thread);
		System.out.println("Checkpoint: " + saved.config().checkPointId().orElseThrow());

		var updated = graph.updateState(saved.config(), Map.of("decision", "approved"));
		var result = graph.invoke(Map.of(), updated.withResume()).orElseThrow();
		System.out.println(result.value("events").orElseThrow());

		graph.getStateHistory(thread).forEach(snapshot ->
				System.out.printf("%s %s%n", snapshot.node(), snapshot.config().checkPointId().orElseThrow()));
	}

	public static void main(String[] args) throws Exception {
		inspectAndResume();
	}

}

