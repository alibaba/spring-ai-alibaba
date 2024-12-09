package com.alibaba.cloud.ai.model.workflow;

import lombok.Data;

import java.util.List;

@Data
public class Graph {

	private List<Edge> edges;

	private List<Node> nodes;

}
