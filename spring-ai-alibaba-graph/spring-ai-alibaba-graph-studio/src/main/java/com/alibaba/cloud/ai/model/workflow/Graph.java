package com.alibaba.cloud.ai.model.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Graph {

	private List<Edge> edges;

	private List<Node> nodes;

}
