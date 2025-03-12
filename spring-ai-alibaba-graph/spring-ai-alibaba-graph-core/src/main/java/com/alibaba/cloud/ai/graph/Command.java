package com.alibaba.cloud.ai.graph;

import java.util.HashMap;

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

		CHILD, PARENT;

		GraphType() {
		}

	}

}
