/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.model.workflow;

import java.util.List;

public class Graph {

	private List<Edge> edges;

	private List<Node> nodes;

	public Graph() {
	}

	public Graph(List<Edge> edges, List<Node> nodes) {
		this.edges = edges;
		this.nodes = nodes;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public Graph setEdges(List<Edge> edges) {
		this.edges = edges;
		return this;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public Graph setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}

}
