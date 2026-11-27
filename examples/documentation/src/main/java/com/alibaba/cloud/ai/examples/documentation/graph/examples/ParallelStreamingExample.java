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

import com.alibaba.cloud.ai.examples.documentation.graph.core.StreamingExample;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.GraphFlux;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ParallelStreamingExample {

	public static String parallelStreamingWithNodeIdPreservation() throws GraphStateException {
		var graph = new StateGraph(() -> Map.of("messages", new AppendStrategy()))
				.addNode("left", node_async(state -> Map.of("leftAnswer",
						GraphFlux.of("left", "leftAnswer",
								Flux.just("left", " ready").map(StreamingExample::responseChunk)))))
				.addNode("right", node_async(state -> Map.of("rightAnswer",
						GraphFlux.of("right", "rightAnswer",
								Flux.just("right", " ready").map(StreamingExample::responseChunk)))))
				.addNode("join", node_async(state -> {
					var left = state.value("leftAnswer", AssistantMessage.class).orElseThrow();
					var right = state.value("rightAnswer", AssistantMessage.class).orElseThrow();
					return Map.of("summary", left.getText() + " | " + right.getText());
				}))
				.addEdge(START, "left").addEdge(START, "right")
				.addEdge("left", "join").addEdge("right", "join").addEdge("join", END)
				.compile();

		var executor = Executors.newFixedThreadPool(2);
		try {
			var run = RunnableConfig.builder().addParallelNodeExecutor(START, executor).build();
			var result = graph.stream(Map.of(), run)
					.doOnNext(output -> {
						if (output instanceof StreamingOutput<?> chunk && chunk.chunk() != null) {
							System.out.printf("%s: %s%n", chunk.node(), chunk.chunk());
						}
					})
					.blockLast(Duration.ofSeconds(5));
			return result.state().value("summary", String.class).orElseThrow();
		}
		finally {
			executor.shutdown();
		}
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println(parallelStreamingWithNodeIdPreservation());
	}

}

