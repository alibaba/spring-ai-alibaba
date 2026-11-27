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
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class TimeTravelExample {

	public static CompiledGraph createGraph(BaseCheckpointSaver saver) throws GraphStateException {
		return new StateGraph(() -> Map.of("steps", new AppendStrategy()))
				.addNode("draft", node_async(state -> Map.of("steps", List.of("drafted"))))
				.addNode("render", node_async(state -> {
					var format = state.value("format", String.class).orElseThrow();
					return Map.of("steps", List.of("rendered:" + format));
				}))
				.addEdge(START, "draft")
				.addEdge("draft", "render")
				.addEdge("render", END)
				.compile(CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build()).build());
	}

	public static StateSnapshot findDraft(CompiledGraph graph, RunnableConfig thread) {
		return graph.getStateHistory(thread).stream()
				.filter(snapshot -> "draft".equals(snapshot.node()))
				.findFirst()
				.orElseThrow();
	}

	public static void replayAndEdit(CompiledGraph graph) throws Exception {
		var thread = RunnableConfig.builder().threadId("report-" + UUID.randomUUID()).build();
		graph.invoke(Map.of("format", "html"), thread).orElseThrow();
		var draft = findDraft(graph, thread);
		System.out.println("Saved draft: " + draft.config().checkPointId().orElseThrow());

		var replay = graph.invoke(Map.of(), draft.config()).orElseThrow();
		System.out.println("Replay: " + replay.value("steps").orElseThrow());

		var update = graph.updateState(draft.config(), Map.of("format", "text"));
		var edited = graph.invoke(Map.of(), update).orElseThrow();
		System.out.println("Edited replay: " + edited.value("steps").orElseThrow());
	}

	public static void main(String[] args) throws Exception {
		replayAndEdit(createGraph(MemorySaver.builder().build()));
	}

}

