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

package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.List;

/**
 * NodeData for ListOperatorNode, which contains all the configurable properties in the
 * Builder. inputs: Provided by variable_selector in the DSL. outputs: provided by outputs
 * in the DSL (usually only one output variable).
 */
public class ListOperatorNodeData extends NodeData {

	/**
	 * Input state variable Key to be manipulated (corresponding to input_text_key in DSL)
	 */
	private String inputTextKey;

	/**
	 * Key of the output state variable to be written to the result of the operation
	 * (corresponding to output_text_key in the DSL)
	 */
	private String outputTextKey;

	/** Filter list (filters in DSL) */
	private List<String> filters;

	/** Sort the list of comparators (comparators in the DSL) */
	private List<String> comparators;

	/** Limit number of entries (corresponding to limit_number in DSL) */
	private Long limitNumber;

	/** The type of the list element (corresponding to element_class_type in the DSL) */
	private String elementClassType;

	public ListOperatorNodeData() {
		super(List.of(), List.of());
	}

	public ListOperatorNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getInputTextKey() {
		return inputTextKey;
	}

	public void setInputTextKey(String inputTextKey) {
		this.inputTextKey = inputTextKey;
	}

	public String getOutputTextKey() {
		return outputTextKey;
	}

	public void setOutputTextKey(String outputTextKey) {
		this.outputTextKey = outputTextKey;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
	}

	public List<String> getComparators() {
		return comparators;
	}

	public void setComparators(List<String> comparators) {
		this.comparators = comparators;
	}

	public Long getLimitNumber() {
		return limitNumber;
	}

	public void setLimitNumber(Long limitNumber) {
		this.limitNumber = limitNumber;
	}

	public String getElementClassType() {
		return elementClassType;
	}

	public void setElementClassType(String elementClassType) {
		this.elementClassType = elementClassType;
	}

}
