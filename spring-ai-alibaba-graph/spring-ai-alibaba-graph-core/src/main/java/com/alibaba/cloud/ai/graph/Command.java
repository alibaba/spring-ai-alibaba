package com.alibaba.cloud.ai.graph;

import java.util.HashMap;
import java.util.Objects;

import static com.alibaba.cloud.ai.graph.Command.GraphType.CHILD;
import static com.alibaba.cloud.ai.graph.Command.GraphType.PARENT;

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

	public enum GraphType {

		CHILD, PARENT,CURRENT;

		GraphType() {
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Command command)) return false;
		if (!super.equals(o)) return false;
        return Objects.equals(getEdge(), command.getEdge()) && Objects.equals(getNodeId(), command.getNodeId()) && getGraph() == command.getGraph();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getEdge(), getNodeId(), getGraph());
	}

	@Override
	public String toString() {
		return "Command{" +
				"edge='" + edge + '\'' +
				", nodeId='" + nodeId + '\'' +
				", graph=" + graph +
				'}';
	}
}
