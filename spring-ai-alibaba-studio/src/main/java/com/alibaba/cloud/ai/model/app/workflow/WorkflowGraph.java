package com.alibaba.cloud.ai.model.app.workflow;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowGraph {

	private List<WorkflowEdge> edges;

	private List<WorkflowNode> nodes;

}
