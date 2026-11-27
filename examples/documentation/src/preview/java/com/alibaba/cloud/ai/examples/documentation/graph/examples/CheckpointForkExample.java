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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import java.util.Map;

public class CheckpointForkExample {

	public static void main(String[] args) throws Exception {
		var graph = TimeTravelExample.createGraph(MemorySaver.builder().build());
		var thread = RunnableConfig.builder().threadId("report-fork").build();
		graph.invoke(Map.of("format", "html"), thread).orElseThrow();
		var draft = TimeTravelExample.findDraft(graph, thread);

		var fork = graph.forkState(draft.config(), Map.of("format", "text"), null);
		var original = graph.getState(draft.config());
		var result = graph.invoke(Map.of(), fork).orElseThrow();
		System.out.println("Original format: " + original.state().value("format").orElseThrow());
		System.out.println("Fork: " + result.value("steps").orElseThrow());
	}

}
