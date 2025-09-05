/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.workflow.assistant.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

	private final CompiledGraph graph;

	public GraphController(CompiledGraph graph) {
		this.graph = graph;
	}

	@PostMapping("/invoke")
	public ResponseEntity<Map<String, Object>> invoke(@RequestBody Map<String, Object> inputs)
			throws GraphStateException, GraphRunnerException {

		// invoke graph
		var resultFuture = graph.invoke(inputs);

		return ResponseEntity.ok(resultFuture.get().data());
	}

	@GetMapping(path = "/mock/http")
	public String mock(@RequestParam("ticketId") String ticketId, @RequestParam("category") String category) {
		Map<String, String> resp = Map.of("status", "OK", "ticketId", ticketId, "category", category);
		return resp.toString();
	}

}
