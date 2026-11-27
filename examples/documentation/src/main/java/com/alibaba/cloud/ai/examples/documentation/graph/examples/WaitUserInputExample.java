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
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class WaitUserInputExample {

	public static CompiledGraph createGraphWithInterrupt() throws GraphStateException {
		var saver = SaverConfig.builder().register(MemorySaver.builder().build()).build();
		return new StateGraph(() -> Map.of("steps", new AppendStrategy()))
				.addNode("prepare", node_async(state -> Map.of("steps", List.of("prepared"))))
				.addNode("feedback", node_async(state -> Map.of()))
				.addNode("finish", node_async(state -> Map.of("steps", List.of("finished"))))
				.addEdge(START, "prepare").addEdge("prepare", "feedback")
				.addConditionalEdges("feedback", edge_async(state -> state.value("feedback", String.class).orElseThrow()),
						Map.of("back", "prepare", "next", "finish"))
				.addEdge("finish", END)
				.compile(CompileConfig.builder().saverConfig(saver).interruptBefore("feedback").build());
	}

	public static List<?> runTwoRounds() throws Exception {
		var graph = createGraphWithInterrupt();
		var thread = RunnableConfig.builder().threadId("feedback-demo").build();
		var firstPause = graph.stream(Map.of(), thread).blockLast(Duration.ofSeconds(5));
		expectPause(firstPause);

		var back = graph.updateState(thread, Map.of("feedback", "back"));
		var secondPause = graph.stream(Map.of(), back.withResume()).blockLast(Duration.ofSeconds(5));
		expectPause(secondPause);

		var next = graph.updateState(thread, Map.of("feedback", "next"));
		var result = graph.invoke(Map.of(), next.withResume()).orElseThrow();
		return result.value("steps", List.class).orElseThrow();
	}

	private static void expectPause(NodeOutput output) {
		if (!(output instanceof InterruptionMetadata interruption)) {
			throw new IllegalStateException("Expected the graph to wait for feedback");
		}
		System.out.println("Waiting: " + interruption.state().value("steps").orElseThrow());
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Steps: " + runTwoRounds());
	}

}

