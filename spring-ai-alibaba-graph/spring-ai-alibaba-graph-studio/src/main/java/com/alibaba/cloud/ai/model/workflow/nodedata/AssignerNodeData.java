/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.List;

public class AssignerNodeData extends NodeData {

	private List<AssignerItem> items;

	private String outputKey;

	private String title;

	private String desc;

	private String version;

	public static class AssignerItem {

		private String inputType;

		private String operation;

		private List<String> value;

		private List<String> variableSelector;

		private String writeMode;

		public String getInputType() {
			return inputType;
		}

		public void setInputType(String inputType) {
			this.inputType = inputType;
		}

		public String getOperation() {
			return operation;
		}

		public void setOperation(String operation) {
			this.operation = operation;
		}

		public List<String> getValue() {
			return value;
		}

		public void setValue(List<String> value) {
			this.value = value;
		}

		public List<String> getVariableSelector() {
			return variableSelector;
		}

		public void setVariableSelector(List<String> variableSelector) {
			this.variableSelector = variableSelector;
		}

		public String getWriteMode() {
			return writeMode;
		}

		public void setWriteMode(String writeMode) {
			this.writeMode = writeMode;
		}

	}

	public List<AssignerItem> getItems() {
		return items;
	}

	public void setItems(List<AssignerItem> items) {
		this.items = items;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
