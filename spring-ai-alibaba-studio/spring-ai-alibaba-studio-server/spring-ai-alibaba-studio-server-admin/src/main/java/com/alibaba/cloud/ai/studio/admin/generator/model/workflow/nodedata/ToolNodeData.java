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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

/**
 * NodeData for ToolNode, in addition to the original llmResponseKey, outputKey,
 * toolNames.
 */
public class ToolNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("text", VariableType.STRING);
	}

	private String llmResponseKey;

	private String outputKey;

	private List<String> toolNames;

	private List<String> toolCallbacks;

	// Additional Dify-specific fields
	private String toolName;

	private String toolDescription;

	private String toolLabel;

	private String providerId;

	private String providerName;

	private String providerType;

	private Map<String, Object> toolParameters;

	private Map<String, Object> toolConfigurations;

	private Boolean isTeamAuthorization;

	private Object outputSchema;

	public ToolNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public ToolNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getLlmResponseKey() {
		return llmResponseKey;
	}

	public ToolNodeData setLlmResponseKey(String llmResponseKey) {
		this.llmResponseKey = llmResponseKey;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public ToolNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public List<String> getToolNames() {
		return toolNames;
	}

	public ToolNodeData setToolNames(List<String> toolNames) {
		this.toolNames = toolNames;
		return this;
	}

	public List<String> getToolCallbacks() {
		return toolCallbacks;
	}

	public ToolNodeData setToolCallbacks(List<String> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
		return this;
	}

	public String getToolName() {
		return toolName;
	}

	public ToolNodeData setToolName(String toolName) {
		this.toolName = toolName;
		return this;
	}

	public String getToolDescription() {
		return toolDescription;
	}

	public ToolNodeData setToolDescription(String toolDescription) {
		this.toolDescription = toolDescription;
		return this;
	}

	public String getToolLabel() {
		return toolLabel;
	}

	public ToolNodeData setToolLabel(String toolLabel) {
		this.toolLabel = toolLabel;
		return this;
	}

	public String getProviderId() {
		return providerId;
	}

	public ToolNodeData setProviderId(String providerId) {
		this.providerId = providerId;
		return this;
	}

	public String getProviderName() {
		return providerName;
	}

	public ToolNodeData setProviderName(String providerName) {
		this.providerName = providerName;
		return this;
	}

	public String getProviderType() {
		return providerType;
	}

	public ToolNodeData setProviderType(String providerType) {
		this.providerType = providerType;
		return this;
	}

	public Map<String, Object> getToolParameters() {
		return toolParameters;
	}

	public ToolNodeData setToolParameters(Map<String, Object> toolParameters) {
		this.toolParameters = toolParameters;
		return this;
	}

	public Map<String, Object> getToolConfigurations() {
		return toolConfigurations;
	}

	public ToolNodeData setToolConfigurations(Map<String, Object> toolConfigurations) {
		this.toolConfigurations = toolConfigurations;
		return this;
	}

	public Boolean getIsTeamAuthorization() {
		return isTeamAuthorization;
	}

	public ToolNodeData setIsTeamAuthorization(Boolean isTeamAuthorization) {
		this.isTeamAuthorization = isTeamAuthorization;
		return this;
	}

	public Object getOutputSchema() {
		return outputSchema;
	}

	public ToolNodeData setOutputSchema(Object outputSchema) {
		this.outputSchema = outputSchema;
		return this;
	}

}
