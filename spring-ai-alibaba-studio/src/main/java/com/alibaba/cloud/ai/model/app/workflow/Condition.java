package com.alibaba.cloud.ai.model.app.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
class Condition {

	private String comparisonOperator;

	private String value;

	private String varType;

	private List<String> variableSelector;

	public void setComparison_operator(String comparison_operator) {
		this.comparisonOperator = comparison_operator;
	}

	public String getComparison_operator() {
		return this.comparisonOperator;
	}

	public void setVar_type(String var_type) {
		this.varType = var_type;
	}

	public String getVar_type() {
		return this.varType;
	}

	public void setVariable_selector(List<String> variable_selector) {
		this.variableSelector = variable_selector;
	}

	public List<String> getVariable_selector() {
		return this.variableSelector;
	}

}
