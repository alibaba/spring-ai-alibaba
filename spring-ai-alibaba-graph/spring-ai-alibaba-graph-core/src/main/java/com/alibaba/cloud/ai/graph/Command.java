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

import java.util.HashMap;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.GraphType.CHILD;
import static com.alibaba.cloud.ai.graph.GraphType.PARENT;

public class Command extends HashMap<String, Object> {

	String edge;

	String nodeId;

	GraphType graph;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public void setEdge(String edge) {
		this.edge = edge;
	}

	public void setGraph(GraphType graph) {
		this.graph = graph;
	}

	public String getEdge() {
		return edge;
	}

	public GraphType getGraph() {
		return graph;
	}

	public boolean isChild() {
		return graph == CHILD;
	}

	public boolean isParent() {
		return graph == PARENT;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Command command))
			return false;
		if (!super.equals(o))
			return false;
		return Objects.equals(getEdge(), command.getEdge()) && Objects.equals(getNodeId(), command.getNodeId())
				&& getGraph() == command.getGraph();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getEdge(), getNodeId(), getGraph());
	}

	@Override
	public String toString() {
		return "Command{" + "edge='" + edge + '\'' + ", nodeId='" + nodeId + '\'' + ", graph=" + graph + '}';
	}

}
