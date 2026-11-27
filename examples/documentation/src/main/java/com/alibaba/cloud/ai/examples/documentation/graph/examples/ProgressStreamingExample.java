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

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ProgressStreamingExample {

	public record Progress(String item, int completed, int total) {
	}

	public static int streamProgress() throws GraphStateException {
		List<String> files = List.of("report.csv", "summary.csv");
		var graph = new StateGraph()
				.addNode("process", node_async(state -> {
					var progress = Flux.range(0, files.size())
							.map(index -> new Progress(files.get(index), index + 1, files.size()));
					var commit = GraphResponse.<NodeOutput>done(Map.of("processedCount", files.size()));
					return Map.of("progress", Flux.<Object>concat(progress, Flux.just(commit)));
				}))
				.addNode("summarize", node_async(state -> {
					int count = state.value("processedCount", Integer.class).orElseThrow();
					return Map.of("summaryCount", count);
				}))
				.addEdge(START, "process").addEdge("process", "summarize").addEdge("summarize", END)
				.compile();

		var end = graph.stream(Map.of())
				.doOnNext(output -> {
					if (output instanceof StreamingOutput<?> stream && stream.getOriginData() instanceof Progress event) {
						System.out.printf("%s %d/%d %s%n", stream.node(), event.completed(), event.total(), event.item());
					}
				})
				.blockLast(Duration.ofSeconds(5));
		return end.state().value("summaryCount", Integer.class).orElseThrow();
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println("Processed: " + streamProgress());
	}

}
