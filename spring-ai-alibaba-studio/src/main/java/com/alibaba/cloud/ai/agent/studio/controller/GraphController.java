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

package com.alibaba.cloud.ai.agent.studio.controller;

import com.alibaba.cloud.ai.agent.studio.dto.GraphResponse;
import com.alibaba.cloud.ai.agent.studio.loader.GraphLoader;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static java.util.stream.Collectors.toList;

/**
 * REST Controller for Graph-related API endpoints. Always registered.
 * Returns empty list when no graphs are available.
 */
@RestController
public class GraphController {

	private static final Logger log = LoggerFactory.getLogger(GraphController.class);

	private final GraphLoader graphLoader;

	@Autowired
	public GraphController(GraphLoader graphLoader) {
		this.graphLoader = graphLoader;
		List<String> graphNames = this.graphLoader.listGraphs();
		log.info("GraphController initialized with {} graphs: {}", graphNames.size(), graphNames);
	}

	/**
	 * Lists available graphs.
	 *
	 * @return A list of graph names.
	 */
	@GetMapping("/list-graphs")
	public List<String> listGraphs() {
		List<String> graphNames = graphLoader.listGraphs();
		log.debug("Listing graphs. Found: {}", graphNames);
		return graphNames.stream().sorted().collect(toList());
	}

	/**
	 * Returns the graph representation (Mermaid format) for the given graph name.
	 *
	 * @param graphName The name of the graph.
	 * @return GraphResponse containing the Mermaid diagram source.
	 */
	@GetMapping("/graphs/{graphName}/representation")
	public GraphResponse getGraphRepresentation(@PathVariable String graphName) {
		if (graphName == null || graphName.isBlank()) {
			throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"graphName cannot be null or empty");
		}
		try {
			CompiledGraph graph = graphLoader.loadGraph(graphName);
			String title = graph.stateGraph != null ? graph.stateGraph.getName() : graphName;
			GraphRepresentation repr = graph.getGraph(GraphRepresentation.Type.MERMAID, title);
			return new GraphResponse(repr.content(), true);
		}
		catch (Exception e) {
			log.warn("Failed to get graph representation for: {}", graphName, e);
			throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
					"Graph not found: " + graphName, e);
		}
	}
}
