/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author yHong
 */

package com.alibaba.cloud.ai.studio.workflow.assistant.controller;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/29 16:18
 */
@RestController
@RequestMapping("/analyze")
public class ParallelController {

	private final CompiledGraph engine;

	@Autowired
	public ParallelController(@Qualifier("parallelGraph") StateGraph parallelGraph) throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().build();
		// 编译时可设中断点
		this.engine = parallelGraph
			.compile(CompileConfig.builder().saverConfig(saverConfig).interruptBefore("merge").build());
	}

	@GetMapping
	public Map<String, Object> analyze(@RequestParam("text") String text) throws GraphRunnerException {
		return engine.invoke(Map.of("inputText", text)).get().data();
	}

	@GetMapping(path = "/stream", produces = "text/event-stream")
	public Flux<Map<String, Object>> analyzeStream(@RequestParam("text") String text) {
		RunnableConfig cfg = RunnableConfig.builder().streamMode(CompiledGraph.StreamMode.SNAPSHOTS).build();
		return engine.stream(Map.of("inputText", text), cfg)
			.map(node -> node.state().data())
			.onErrorResume(e -> Flux.error(new RuntimeException("Error in parallel stream execution: " + e.getMessage(), e)));
	}

}
