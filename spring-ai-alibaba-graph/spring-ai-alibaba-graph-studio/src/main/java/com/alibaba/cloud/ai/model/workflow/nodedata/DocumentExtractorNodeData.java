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
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.List;

/**
 * @author HeYQ
 * @since 2025-05-02 17:03
 */
public class DocumentExtractorNodeData extends NodeData {

	public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("text", VariableType.ARRAY_STRING.value());

	private List<String> fileList;

	private String outputKey;

	public DocumentExtractorNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public DocumentExtractorNodeData(List<VariableSelector> inputs, List<Variable> outputs, List<String> fileList,
			String outputKey) {
		super(inputs, outputs);
		this.fileList = fileList;
		this.outputKey = outputKey;
	}

	public List<String> getFileList() {
		return fileList;
	}

	public void setFileList(List<String> fileList) {
		this.fileList = fileList;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
