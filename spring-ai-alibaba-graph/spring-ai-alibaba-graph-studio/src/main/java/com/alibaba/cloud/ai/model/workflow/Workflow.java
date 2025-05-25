package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.Variable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Workflow defines the AI powered flow.
 */
@Data
public class Workflow {

	private Graph graph;

	private List<Variable> workflowVars;

	private List<Variable> envVars;

}
