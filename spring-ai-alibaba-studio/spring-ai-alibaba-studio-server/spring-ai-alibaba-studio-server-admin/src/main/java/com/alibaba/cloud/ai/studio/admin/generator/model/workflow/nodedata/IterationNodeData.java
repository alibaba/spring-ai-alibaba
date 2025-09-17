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

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

import java.util.List;

/**
 * @author vlsmb
 * @since 2025/7/21
 */
public class IterationNodeData extends NodeData {

	public static List<Variable> getDefaultOutputSchemas() {
		return List.of(new Variable("state", VariableType.ARRAY_NUMBER), // 剩余未处理的元素索引
				new Variable("index", VariableType.NUMBER), // 迭代索引
				new Variable("isFinished", VariableType.BOOLEAN) // 迭代是否结束
		);
	}

	// NodeData的来源节点名称
	private final String sourceVarName;

	private int parallelCount = 1;

	private int maxIterationCount = Integer.MAX_VALUE;

	// Dify的迭代索引从0开始，而Studio的从1开始，故需要设置这个值
	private int indexOffset = 0;

	// itemKey和outputKey的后缀在Dify中固定，但在Studio中用户可以自定义
	private String itemKey = "item";

	private String outputKey = "output";

	// 迭代输入的Selector
	private VariableSelector inputSelector;

	// 迭代结果元素的Selector
	private VariableSelector resultSelector;

	public IterationNodeData(IterationNodeData other) {
		parallelCount = other.parallelCount;
		maxIterationCount = other.maxIterationCount;
		indexOffset = other.indexOffset;
		itemKey = other.itemKey;
		outputKey = other.outputKey;
		inputSelector = other.inputSelector;
		resultSelector = other.resultSelector;
		sourceVarName = other.getVarName();
		setVarName(other.getVarName());
	}

	public IterationNodeData() {
		super();
		sourceVarName = null;
	}

	public String getSourceVarName() {
		return sourceVarName;
	}

	public int getParallelCount() {
		return parallelCount;
	}

	public void setParallelCount(int parallelCount) {
		this.parallelCount = parallelCount;
	}

	public int getMaxIterationCount() {
		return maxIterationCount;
	}

	public void setMaxIterationCount(int maxIterationCount) {
		this.maxIterationCount = maxIterationCount;
	}

	public int getIndexOffset() {
		return indexOffset;
	}

	public void setIndexOffset(int indexOffset) {
		this.indexOffset = indexOffset;
	}

	public String getItemKey() {
		return itemKey;
	}

	public void setItemKey(String itemKey) {
		this.itemKey = itemKey;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public VariableSelector getInputSelector() {
		return inputSelector;
	}

	public void setInputSelector(VariableSelector inputSelector) {
		this.inputSelector = inputSelector;
	}

	public VariableSelector getResultSelector() {
		return resultSelector;
	}

	public void setResultSelector(VariableSelector resultSelector) {
		this.resultSelector = resultSelector;
	}

}
