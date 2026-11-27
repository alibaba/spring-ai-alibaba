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
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class GraphLifecycleExample {

	public static List<String> trace(boolean fail) throws GraphStateException {
		var events = new ArrayList<String>();
		var listener = new GraphLifecycleListener() {
			@Override
			public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long time) {
				events.add("before:" + nodeId);
			}

			@Override
			public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long time) {
				events.add("after:" + nodeId);
			}

			@Override
			public void onError(String nodeId, Map<String, Object> state, Throwable error, RunnableConfig config) {
				events.add("error:" + nodeId);
			}
		};

		var graph = new StateGraph()
				.addNode("work", node_async(state -> {
					if (fail) {
						throw new WorkFailedException();
					}
					return Map.of("processed", true);
				}))
				.addEdge(START, "work").addEdge("work", END)
				.compile(CompileConfig.builder().withLifecycleListener(listener).build());

		try {
			graph.stream(Map.of()).blockLast(Duration.ofSeconds(5));
		}
		catch (WorkFailedException expected) {
			// The same error is visible to the stream subscriber.
		}
		return List.copyOf(events);
	}

	public static void main(String[] args) throws GraphStateException {
		System.out.println("Success: " + trace(false));
		System.out.println("Failure: " + trace(true));
	}

	private static class WorkFailedException extends RuntimeException {

		WorkFailedException() {
			super("Work failed");
		}

	}

}
