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

	public static CompiledGraph createGraphWithCheckpointer() throws GraphStateException {
		var saverConfig = SaverConfig.builder().register(MemorySaver.builder().build()).build();
		return new StateGraph(() -> Map.of("messages", new AppendStrategy()))
				.addNode("answer", node_async(state -> {
					var messages = state.value("messages", List.class).orElseThrow();
					return Map.of("messages", List.of("Echo: " + messages.get(messages.size() - 1)));
				}))
				.addEdge(START, "answer").addEdge("answer", END)
				.compile(CompileConfig.builder().saverConfig(saverConfig).build());
	}

	public static void multiSessionIsolation(CompiledGraph graph) {
		var alice = RunnableConfig.builder().threadId("alice").build();
		var bob = RunnableConfig.builder().threadId("bob").build();
		graph.invoke(Map.of("messages", List.of("Hello")), alice).orElseThrow();
		graph.invoke(Map.of("messages", List.of("Good morning")), bob).orElseThrow();

		var secondTurn = graph.invoke(Map.of("messages", List.of("One more question")), alice).orElseThrow();
		System.out.println("Alice: " + secondTurn.value("messages").orElseThrow());
		System.out.println("Bob: " + graph.getState(bob).state().value("messages").orElseThrow());
	}

	public static void main(String[] args) throws GraphStateException {
		multiSessionIsolation(createGraphWithCheckpointer());
	}

}

