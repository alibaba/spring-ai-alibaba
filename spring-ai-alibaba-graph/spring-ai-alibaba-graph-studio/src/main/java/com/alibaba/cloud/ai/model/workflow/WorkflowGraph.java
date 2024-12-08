package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.workflow.edge.WorkflowEdge;
import com.alibaba.cloud.ai.model.workflow.node.WorkflowNode;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowGraph {

	private List<WorkflowEdge> edges;

	private List<WorkflowNode> nodes;

}
