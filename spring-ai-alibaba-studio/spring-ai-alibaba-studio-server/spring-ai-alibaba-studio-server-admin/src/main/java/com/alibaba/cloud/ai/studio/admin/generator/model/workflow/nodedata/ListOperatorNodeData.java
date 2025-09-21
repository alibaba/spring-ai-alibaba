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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.List;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.ComparisonOperatorType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

/**
 * NodeData for ListOperatorNode, which contains all the configurable properties in the
 * Builder. inputs: Provided by variable_selector in the DSL. outputs: provided by outputs
 * in the DSL (usually only one output variable).
 */
public class ListOperatorNodeData extends NodeData {

	public static Variable defaultOutputSchema() {
		return new Variable("result", VariableType.ARRAY_OBJECT);
	}

	/**
	 * Input state variable Key to be manipulated (corresponding to input_text_key in DSL)
	 */
	private String inputKey;

	/**
	 * Key of the output state variable to be written to the result of the operation
	 * (corresponding to output_text_key in the DSL)
	 */
	private String outputKey;

	public record FilterCondition(ComparisonOperatorType condition, String value) {
		public static FilterCondition ofDify(String condition) {
			return ofDify(condition, null);
		}

		public static FilterCondition ofDify(String condition, String value) {
			if (condition == null) {
				return null;
			}
			for (ComparisonOperatorType e : ComparisonOperatorType.values()) {
				if (e.getDslValue(DSLDialectType.DIFY).equalsIgnoreCase(condition)) {
					return new FilterCondition(e, value);
				}
			}
			return null;
		}
	}

	/** Filter list (filters in DSL) */
	private List<FilterCondition> filters;

	public enum Ordered {

		ASC, DESC;

	}

	// null则不排序
	private Ordered order;

	/** Limit number of entries (corresponding to limit_number in DSL) */
	private Integer limitNumber;

	/** The type of the list element (corresponding to element_class_type in the DSL) */
	private VariableType elementClassType;

	public ListOperatorNodeData() {
		super(List.of(), List.of());
	}

	public ListOperatorNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getInputKey() {
		return inputKey;
	}

	public void setInputKey(String inputKey) {
		this.inputKey = inputKey;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public List<FilterCondition> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterCondition> filters) {
		this.filters = filters;
	}

	public Ordered getOrder() {
		return order;
	}

	public void setOrder(Ordered order) {
		this.order = order;
	}

	public Integer getLimitNumber() {
		return limitNumber;
	}

	public void setLimitNumber(Integer limitNumber) {
		this.limitNumber = limitNumber;
	}

	public VariableType getElementClassType() {
		return elementClassType;
	}

	public void setElementClassType(VariableType elementClassType) {
		this.elementClassType = elementClassType;
	}

}
