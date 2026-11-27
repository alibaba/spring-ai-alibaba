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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.SignalType;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class CancellationExample {

	public static SignalType cancelSubscription() throws GraphStateException, InterruptedException {
		var started = new CountDownLatch(1);
		var unfinished = new CompletableFuture<Map<String, Object>>();
		var graph = new StateGraph()
				.addNode("work", (state, config) -> {
					started.countDown();
					return unfinished;
				})
				.addEdge(START, "work")
				.addEdge("work", END)
				.compile();

		var termination = new CompletableFuture<SignalType>();
		var subscription = graph.stream(Map.of())
				.doFinally(termination::complete)
				.subscribe(output -> System.out.println(output.node()), termination::completeExceptionally);
		try {
			if (!started.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("The work node did not start");
			}
		}
		finally {
			subscription.dispose();
		}
		return termination.join();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Termination: " + cancelSubscription());
	}

}

