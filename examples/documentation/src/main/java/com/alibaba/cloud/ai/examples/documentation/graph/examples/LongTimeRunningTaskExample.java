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
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class LongTimeRunningTaskExample {

	private static CompiledGraph createGraph(Path directory, boolean failDuringPublish) throws GraphStateException {
		var saver = FileSystemSaver.builder().targetFolder(directory).build();
		var config = CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build()).build();

		return new StateGraph(() -> Map.of("steps", new AppendStrategy()))
				.addNode("prepare", node_async(state -> Map.of("steps", List.of("prepared"))))
				.addNode("publish", node_async(state -> {
					if (failDuringPublish) {
						throw new PublishUnavailableException();
					}
					return Map.of("steps", List.of("published"));
				}))
				.addEdge(START, "prepare")
				.addEdge("prepare", "publish")
				.addEdge("publish", END)
				.compile(config);
	}

	public static List<?> recoverFromCheckpoint(Path directory) throws GraphStateException {
		var run = RunnableConfig.builder().threadId("publish-" + UUID.randomUUID()).build();
		var first = createGraph(directory, true);
		try {
			first.stream(Map.of(), run).blockLast(Duration.ofSeconds(5));
		}
		catch (PublishUnavailableException expected) {
			System.out.println("Last saved node: " + first.getState(run).node());
		}

		var restarted = createGraph(directory, false);
		var result = restarted.stream(Map.of(), run.withResume()).blockLast(Duration.ofSeconds(5));
		return result.state().value("steps", List.class).orElseThrow();
	}

	public static void main(String[] args) throws Exception {
		var directory = args.length == 0 ? Files.createTempDirectory("graph-checkpoints-") : Path.of(args[0]);
		System.out.println("Checkpoints: " + directory);
		System.out.println("Steps: " + recoverFromCheckpoint(directory));
	}

	private static class PublishUnavailableException extends RuntimeException {

		PublishUnavailableException() {
			super("Publish service is temporarily unavailable");
		}

	}

}

