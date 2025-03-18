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
        if (this == o) return true;
        if (!(o instanceof Send send)) return false;
        return Objects.equals(edge, send.edge) && Objects.equals(nodeId, send.nodeId) && graph == send.graph;
    }

    @Override
    public int hashCode() {
        return Objects.hash(edge, nodeId, graph);
    }

    @Override
    public String toString() {
        return "Send{" +
                "edge='" + edge + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", graph=" + graph +
                '}';
    }
}
