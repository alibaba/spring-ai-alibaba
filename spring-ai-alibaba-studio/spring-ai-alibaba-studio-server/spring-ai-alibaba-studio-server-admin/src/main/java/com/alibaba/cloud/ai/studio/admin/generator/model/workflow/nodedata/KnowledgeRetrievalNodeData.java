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
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import java.util.List;

/**
 * NodeData for KnowledgeRetrievalNode, encapsulating all Builder properties.
 */
public class KnowledgeRetrievalNodeData extends NodeData {

	public static List<Variable> getDefaultOutputSchemas(DSLDialectType dialectType) {
		return List.of(dialectType.equals(DSLDialectType.STUDIO) ? new Variable("chunk_list", VariableType.ARRAY_OBJECT)
				: new Variable("result", VariableType.ARRAY_OBJECT));
	}

	private Integer topK;

	private Double threshold;

	// 对于Studio DSL，生成代码时可以根据知识库ID生成对应代码；对于Dify DSL，只能提示用户手动编写知识库代码
	private List<String> knowledgeBaseIds;

	private List<NodeSection.ResourceFile> resourceFiles;

	private String inputKey;

	private String outputKey;

	private DSLDialectType dialectType;

	public Integer getTopK() {
		return topK;
	}

	public void setTopK(Integer topK) {
		this.topK = topK;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public List<String> getKnowledgeBaseIds() {
		return knowledgeBaseIds;
	}

	public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) {
		this.knowledgeBaseIds = knowledgeBaseIds;
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

	public List<NodeSection.ResourceFile> getResourceFiles() {
		return resourceFiles;
	}

	public void setResourceFiles(List<NodeSection.ResourceFile> resourceFiles) {
		this.resourceFiles = resourceFiles;
	}

	public DSLDialectType getDialectType() {
		return dialectType;
	}

	public void setDialectType(DSLDialectType dialectType) {
		this.dialectType = dialectType;
	}

}
