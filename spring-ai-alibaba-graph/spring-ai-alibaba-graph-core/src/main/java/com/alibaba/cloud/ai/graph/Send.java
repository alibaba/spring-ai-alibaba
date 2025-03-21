/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import java.util.Objects;

public class Send {

	String edge;

	String nodeId;

	GraphType graph;

	public String getEdge() {
		return edge;
	}

	public void setEdge(String edge) {
		this.edge = edge;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public GraphType getGraph() {
		return graph;
	}

	public void setGraph(GraphType graph) {
		this.graph = graph;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Send send))
			return false;
		return Objects.equals(edge, send.edge) && Objects.equals(nodeId, send.nodeId) && graph == send.graph;
	}

	@Override
	public int hashCode() {
		return Objects.hash(edge, nodeId, graph);
	}

	@Override
	public String toString() {
		return "Send{" + "edge='" + edge + '\'' + ", nodeId='" + nodeId + '\'' + ", graph=" + graph + '}';
	}

}
