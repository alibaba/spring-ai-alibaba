package com.alibaba.cloud.ai.model.workflow.edge;

import com.alibaba.cloud.ai.model.VariableSelector;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Case {

	private String id;

	private String logicalOperator;

	private List<Condition> conditions;

	@Data
	@Accessors(chain = true)
	public static class Condition {

		private String value;

		private String varType;

		private String comparisonOperator;

		private VariableSelector variableSelector;

	}

}
