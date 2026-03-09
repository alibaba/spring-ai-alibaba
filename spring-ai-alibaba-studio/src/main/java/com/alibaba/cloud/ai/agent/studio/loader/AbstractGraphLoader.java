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

package com.alibaba.cloud.ai.agent.studio.loader;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Base implementation of {@link GraphLoader} that delegates to a name-to-graph map.
 */
public abstract class AbstractGraphLoader implements GraphLoader {

	private static final Logger log = LoggerFactory.getLogger(AbstractGraphLoader.class);

	private volatile Map<String, CompiledGraph> graphMap;

	/**
	 * Supplies the map of graph name to graph instance. Called once and cached.
	 *
	 * @return map of graph name to CompiledGraph (must not be null; can be empty)
	 */
	protected abstract Map<String, CompiledGraph> loadGraphMap();

	private Map<String, CompiledGraph> getGraphMap() {
		if (graphMap == null) {
			synchronized (this) {
				if (graphMap == null) {
					graphMap = loadGraphMap();
				}
			}
		}
		return graphMap;
	}

	/**
	 * Helper to build a name-to-graph map from all {@link CompiledGraph} beans in the given
	 * context. Uses {@link StateGraph#getName()} when available, otherwise bean name.
	 *
	 * @param context the Spring application context
	 * @return map of graph name to CompiledGraph instance
	 */
	protected static Map<String, CompiledGraph> discoverFromContext(ApplicationContext context) {
		Map<String, CompiledGraph> beans = context.getBeansOfType(CompiledGraph.class);
		Map<String, CompiledGraph> result = new LinkedHashMap<>();
		for (Map.Entry<String, CompiledGraph> entry : beans.entrySet()) {
			String beanName = entry.getKey();
			CompiledGraph graph = entry.getValue();
			String graphName = graph.stateGraph != null ? graph.stateGraph.getName() : beanName;
			if (graphName == null || graphName.isBlank()) {
				graphName = beanName;
			}
			if (result.putIfAbsent(graphName, graph) != null) {
				log.warn("Duplicate graph name '{}', keeping first. Consider using unique graph names for Studio.",
						graphName);
			}
		}
		return result;
	}

	@Override
	@Nonnull
	public List<String> listGraphs() {
		return List.copyOf(getGraphMap().keySet());
	}

	@Override
	public CompiledGraph loadGraph(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Graph name cannot be null or empty");
		}
		CompiledGraph graph = getGraphMap().get(name);
		if (graph == null) {
			throw new NoSuchElementException("Graph not found: " + name);
		}
		return graph;
	}
}
