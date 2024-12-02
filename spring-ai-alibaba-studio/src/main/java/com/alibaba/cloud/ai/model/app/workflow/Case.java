package com.alibaba.cloud.ai.model.app.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class Case {

	private String caseId;

	private String id;

	private String logicalOperator;

	private List<Condition> conditions;

	public void setCase_id(String case_id) {
		this.caseId = case_id;
	}

	public String getCase_id() {
		return this.caseId;
	}

	public void setLogical_operator(String logical_operator) {
		this.logicalOperator = logical_operator;
	}

	public String getLogical_operator() {
		return this.logicalOperator;
	}

}
