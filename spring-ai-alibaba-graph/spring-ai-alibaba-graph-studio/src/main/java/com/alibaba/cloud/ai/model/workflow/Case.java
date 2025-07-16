/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.model.workflow;

import com.alibaba.cloud.ai.model.VariableSelector;

import java.util.List;

public class Case {

	private String id;

	private String logicalOperator;

	private List<Condition> conditions;

	public String getId() {
		return id;
	}

	public Case setId(String id) {
		this.id = id;
		return this;
	}

	public String getLogicalOperator() {
		return logicalOperator;
	}

	public Case setLogicalOperator(String logicalOperator) {
		this.logicalOperator = logicalOperator;
		return this;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public Case setConditions(List<Condition> conditions) {
		this.conditions = conditions;
		return this;
	}

	public static class Condition {

		private String value;

		private String varType;

		// TODO comparison operator enum
		private String comparisonOperator;

		private VariableSelector variableSelector;

		public String getValue() {
			return value;
		}

		public Condition setValue(String value) {
			this.value = value;
			return this;
		}

		public String getVarType() {
			return varType;
		}

		public Condition setVarType(String varType) {
			this.varType = varType;
			return this;
		}

		public String getComparisonOperator() {
			return comparisonOperator;
		}

		public Condition setComparisonOperator(String comparisonOperator) {
			this.comparisonOperator = comparisonOperator;
			return this;
		}

		public VariableSelector getVariableSelector() {
			return variableSelector;
		}

		public Condition setVariableSelector(VariableSelector variableSelector) {
			this.variableSelector = variableSelector;
			return this;
		}

	}

}
