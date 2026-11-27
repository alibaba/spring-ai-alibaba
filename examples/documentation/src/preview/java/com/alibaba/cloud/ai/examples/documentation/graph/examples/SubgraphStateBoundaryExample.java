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
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.RemoveByValue;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class SubgraphStateBoundaryExample {

	public static void resumeChildWithSelectedState() throws Exception {
		var saver = SaverConfig.builder().register(MemorySaver.builder().build()).build();
		var child = new StateGraph(() -> Map.of("notes", new AppendStrategy()))
				.addNode("prepare", node_async(state -> Map.of(
						"notes", List.of(RemoveByValue.of("Aa"), "prepared"),
						"internal", state.value("request").orElseThrow())))
				.addNode("finish", node_async(state -> Map.of("notes", List.of("finished"))))
				.addEdge(START, "prepare").addEdge("prepare", "finish").addEdge("finish", END)
				.compile(CompileConfig.builder().saverConfig(saver).interruptAfter("prepare").build());

		var parent = new StateGraph(() -> Map.of("notes", new AppendStrategy()))
				.addNode("child", child, Set.of("request", "notes"), Set.of("notes"))
				.addEdge(START, "child").addEdge("child", END)
				.compile(CompileConfig.builder().saverConfig(saver).build());

		var run = RunnableConfig.builder().threadId("subgraph-approval").build();
		parent.stream(Map.of("request", "review", "notes", List.of("BB", "Aa"), "internal", "parent"), run)
				.blockLast();
		var result = parent.invoke(Map.of(), run.withResume()).orElseThrow();
		System.out.println("Notes: " + result.value("notes").orElseThrow());
		System.out.println("Internal: " + result.value("internal").orElseThrow());
	}

	public static void main(String[] args) throws Exception {
		resumeChildWithSelectedState();
	}

}
