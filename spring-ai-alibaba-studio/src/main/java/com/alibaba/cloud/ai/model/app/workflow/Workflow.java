package com.alibaba.cloud.ai.model.app.workflow;

import lombok.Data;

import java.util.List;

@Data
public class Workflow {

	private WorkflowGraph graph;

	private List<Variable> workflowVars;

	private List<Variable> envVars;

}
