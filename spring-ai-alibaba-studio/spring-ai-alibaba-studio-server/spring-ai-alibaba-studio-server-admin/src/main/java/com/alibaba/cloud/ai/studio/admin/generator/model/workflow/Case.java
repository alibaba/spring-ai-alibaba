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
package com.alibaba.cloud.ai.studio.admin.generator.model.workflow;

import java.util.List;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;

public class Case {

	private String id;

	private LogicalOperatorType logicalOperator;

	private List<Condition> conditions;

	public String getId() {
		return id;
	}

	public Case setId(String id) {
		this.id = id;
		return this;
	}

	public LogicalOperatorType getLogicalOperator() {
		return logicalOperator;
	}

	public Case setLogicalOperator(LogicalOperatorType logicalOperator) {
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

		// 左值数据类型
		private VariableType varType;

		// 右值数据类型
		private VariableType referenceType;

		private ComparisonOperatorType comparisonOperator;

		// 左值
		private VariableSelector targetSelector;

		// 右值
		private String referenceValue;

		private VariableSelector referenceSelector;

		// 参考值可能来自stateKey，也有可能直接是常量值，也有可能没有参考值
		public String getValue() {
			if (referenceValue != null) {
				return referenceValue;
			}
			else if (referenceSelector != null) {
				if (VariableType.NUMBER.equals(referenceType)) {
					return String.format("((%s) state.value(\"%s\").orElse(null)).doubleValue()", referenceType.value(),
							referenceSelector.getNameInCode());
				}
				return String.format("((%s) state.value(\"%s\").orElse(null))", referenceType.value(),
						referenceSelector.getNameInCode());
			}
			return null;
		}

		public String getReferenceValue() {
			return referenceValue;
		}

		public Condition setReferenceValue(String referenceValue) {
			this.referenceValue = referenceValue;
			return this;
		}

		public VariableSelector getReferenceSelector() {
			return referenceSelector;
		}

		public Condition setReferenceSelector(VariableSelector referenceSelector) {
			this.referenceSelector = referenceSelector;
			return this;
		}

		public VariableType getVarType() {
			return varType;
		}

		public Condition setVarType(VariableType varType) {
			this.varType = varType;
			return this;
		}

		public VariableType getReferenceType() {
			return referenceType;
		}

		public Condition setReferenceType(VariableType referenceType) {
			this.referenceType = referenceType;
			return this;
		}

		public ComparisonOperatorType getComparisonOperator() {
			return comparisonOperator;
		}

		public Condition setComparisonOperator(ComparisonOperatorType comparisonOperator) {
			this.comparisonOperator = comparisonOperator;
			return this;
		}

		public VariableSelector getTargetSelector() {
			return targetSelector;
		}

		public Condition setTargetSelector(VariableSelector targetSelector) {
			this.targetSelector = targetSelector;
			return this;
		}

	}

}
