package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.Variable;
import lombok.Data;

import java.util.List;

@Data
public class Workflow {

	private WorkflowGraph graph;

	private List<Variable> workflowVars;

	private List<Variable> envVars;

}
