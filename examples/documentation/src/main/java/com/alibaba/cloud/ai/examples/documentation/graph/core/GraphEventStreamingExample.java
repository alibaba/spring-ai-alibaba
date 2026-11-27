/*
 * Copyright 2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.NodeAggregationStrategy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.StreamOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.util.concurrent.CompletableFuture.completedFuture;

/** Runs graph streaming scenarios with local model chunks. */
public class GraphEventStreamingExample {

	private static final Logger log = LoggerFactory.getLogger(GraphEventStreamingExample.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final StreamOptions OPTIONS = new StreamOptions(
			Set.of(RunEvent.Channel.LIFECYCLE, RunEvent.Channel.MESSAGES, RunEvent.Channel.CUSTOM,
					RunEvent.Channel.CHECKPOINTS),
			true, false, false, 256, 256 * 1024);

	public static Flux<RunEvent> streamDynamicChild() throws GraphStateException {
		var child = draftGraph().compile();
		var parent = new StateGraph("workflow", Map::of)
				.addNode("compose", (state, config) -> {
					config.streamWriter().tryEmit("progress", "Writing the draft");
					return config.runChild(child, Map.of("topic", state.value("input", String.class).orElseThrow()))
							.map(result -> Map.<String, Object>of("answer",
									result.state().value("draft", AssistantMessage.class).orElseThrow().getText()))
							.toFuture();
				})
				.addEdge(START, "compose").addEdge("compose", END).compile();
		return parent.streamEvents(Map.of("input", "Spring AI"), RunnableConfig.builder().build(), OPTIONS);
	}

	public static CompiledGraph resumableChildWorkflow() throws GraphStateException {
		var saver = SaverConfig.builder().register(new MemorySaver()).build();
		var child = draftGraph().compile(CompileConfig.builder().saverConfig(saver).interruptBefore("draft").build());
		return new StateGraph("resumable-workflow", Map::of)
				.addNode("compose", child,
						state -> Map.of("topic", state.value("input", String.class).orElseThrow()),
						write -> write.get("draft") instanceof AssistantMessage draft
								? Map.of("answer", draft.getText()) : Map.of())
				.addEdge(START, "compose").addEdge("compose", END)
				.compile(CompileConfig.builder().saverConfig(saver).build());
	}

	private static StateGraph draftGraph() throws GraphStateException {
		return new StateGraph("draft-workflow", Map::of)
				.addNode("draft", (state, config) -> {
					String topic = state.value("topic", String.class).orElseThrow();
					return completedFuture(Map.of("draft", Flux.just("Draft about ", topic)
							.map(StreamingExample::responseChunk)));
				})
				.addEdge(START, "draft").addEdge("draft", END);
	}

	public static Flux<RunEvent> streamParallelBranches(NodeAggregationStrategy strategy) throws GraphStateException {
		var graph = new StateGraph("parallel-drafts", Map::of)
				.addNode("shortDraft", (state, config) -> completedFuture(Map.of("shortAnswer",
						Flux.just("Short ", "draft").map(StreamingExample::responseChunk))))
				.addNode("detailedDraft", (state, config) -> completedFuture(Map.of("detailedAnswer",
						Flux.just("Detailed ", "draft").map(StreamingExample::responseChunk))))
				.addEdge(START, "shortDraft").addEdge(START, "detailedDraft")
				.addEdge("shortDraft", END).addEdge("detailedDraft", END).compile();
		var run = RunnableConfig.builder().defaultParallelAggregationStrategy(strategy).build();
		return graph.streamEvents(Map.of(), run, OPTIONS.withCandidateLifecycle(strategy == NodeAggregationStrategy.ANY_OF));
	}

	public static void main(String[] args) throws GraphStateException {
		var dynamic = streamDynamicChild().doOnNext(GraphEventStreamingExample::logEvent).blockLast(TIMEOUT);
		log.info("Dynamic child answer = {}", finalAnswer(dynamic));

		var graph = resumableChildWorkflow();
		var run = RunnableConfig.builder().threadId("draft-42").build();
		var before = graph.streamEvents(Map.of("input", "Spring AI"), run, OPTIONS).blockLast(TIMEOUT);
		if (before == null || before.type() != RunEvent.Type.RUN_PAUSED) {
			throw new IllegalStateException("Expected the child to pause before drafting");
		}
		var after = graph.streamEvents(Map.of(), run.withResume(), OPTIONS)
				.doOnNext(GraphEventStreamingExample::logEvent).blockLast(TIMEOUT);
		log.info("Resumed child answer = {}", finalAnswer(after));

		streamParallelBranches(NodeAggregationStrategy.ALL_OF)
				.doOnNext(GraphEventStreamingExample::logEvent).blockLast(TIMEOUT);
	}

	private static Object finalAnswer(RunEvent event) {
		if (event != null && event.type() == RunEvent.Type.RUN_COMPLETED && event.data() instanceof RunEvent.State state) {
			return state.values().get("answer");
		}
		throw new IllegalStateException("The graph did not complete successfully. Outcome " + event);
	}

	private static void logEvent(RunEvent event) {
		if (event.data() instanceof RunEvent.ModelDelta delta && delta.text() != null) {
			var path = event.scope().namespace().stream().map(RunEvent.PathSegment::name).toList();
			log.info("{} {}", path, delta.text());
		}
		else if (event.data() instanceof RunEvent.Custom progress) {
			log.info("{} {}", progress.name(), progress.value());
		}
	}
}
