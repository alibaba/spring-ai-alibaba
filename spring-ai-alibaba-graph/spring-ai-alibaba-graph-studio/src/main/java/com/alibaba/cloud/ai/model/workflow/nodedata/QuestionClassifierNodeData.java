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
 * @since 2024-12-12 21:26
 */
public class QuestionClassifierNodeData extends NodeData {

	public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("class_name", VariableType.STRING.value());

	private ModelConfig model;

	private MemoryConfig memoryConfig;

	private List<PromptTemplate> promptTemplate;

	private String instruction;

	private List<ClassConfig> classes;

	private String outputKey;

	private String inputTextKey;

	public QuestionClassifierNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public ModelConfig getModel() {
		return model;
	}

	public QuestionClassifierNodeData setModel(ModelConfig model) {
		this.model = model;
		return this;
	}

	public List<PromptTemplate> getPromptTemplate() {
		return promptTemplate;
	}

	public QuestionClassifierNodeData setPromptTemplate(List<PromptTemplate> promptTemplate) {
		this.promptTemplate = promptTemplate;
		return this;
	}

	public MemoryConfig getMemoryConfig() {
		return memoryConfig;
	}

	public QuestionClassifierNodeData setMemoryConfig(MemoryConfig memoryConfig) {
		this.memoryConfig = memoryConfig;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public String getInputTextKey() {
		return inputTextKey;
	}

	public QuestionClassifierNodeData setInputTextKey(String inputTextKey) {
		this.inputTextKey = inputTextKey;
		return this;
	}

	public String getInstruction() {
		return instruction;
	}

	public QuestionClassifierNodeData setInstruction(String instruction) {
		this.instruction = instruction;
		return this;
	}

	public List<ClassConfig> getClasses() {
		return classes;
	}

	public QuestionClassifierNodeData setClasses(List<ClassConfig> classes) {
		this.classes = classes;
		return this;
	}

	public static class ClassConfig {

		private String id;

		private String text;

		public String getId() {
			return id;
		}

		public ClassConfig setId(String id) {
			this.id = id;
			return this;
		}

		public String getText() {
			return text;
		}

		public ClassConfig setText(String text) {
			this.text = text;
			return this;
		}

	}

	public static class PromptTemplate {

		private String role;

		private String text;

		public PromptTemplate() {
		}

		public PromptTemplate(String role, String text) {
			this.role = role;
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public PromptTemplate setText(String text) {
			this.text = text;
			return this;
		}

		public String getRole() {
			return role;
		}

		public PromptTemplate setRole(String role) {
			this.role = role;
			return this;
		}

	}

	public static class ModelConfig {

		public static final String MODE_COMPLETION = "completion";

		public static final String MODE_CHAT = "chat";

		private String mode;

		private String name;

		private String provider;

		private CompletionParams completionParams;

		public String getMode() {
			return mode;
		}

		public ModelConfig setMode(String mode) {
			this.mode = mode;
			return this;
		}

		public String getName() {
			return name;
		}

		public ModelConfig setName(String name) {
			this.name = name;
			return this;
		}

		public String getProvider() {
			return provider;
		}

		public ModelConfig setProvider(String provider) {
			this.provider = provider;
			return this;
		}

		public CompletionParams getCompletionParams() {
			return completionParams;
		}

		public ModelConfig setCompletionParams(CompletionParams completionParams) {
			this.completionParams = completionParams;
			return this;
		}

	}

	public static class CompletionParams {

		private Integer maxTokens;

		private Float repetitionPenalty;

		private String responseFormat;

		private Integer seed;

		private List<String> stop;

		private Float temperature;

		private Float topP;

		private Integer topK;

		private Integer frequencyPenalty;

		private Integer presencePenalty;

		public Integer getMaxTokens() {
			return maxTokens;
		}

		public CompletionParams setMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public void setRepetitionPenalty(Float repetitionPenalty) {
			this.repetitionPenalty = repetitionPenalty;
		}

		public void setResponseFormat(String responseFormat) {
			this.responseFormat = responseFormat;
		}

		public void setSeed(Integer seed) {
			this.seed = seed;
		}

		public void setStop(List<String> stop) {
			this.stop = stop;
		}

		public void setTemperature(Float temperature) {
			this.temperature = temperature;
		}

		public void setTopP(Float topP) {
			this.topP = topP;
		}

		public void setTopK(Integer topK) {
			this.topK = topK;
		}

		public void setFrequencyPenalty(Integer frequencyPenalty) {
			this.frequencyPenalty = frequencyPenalty;
		}

		public void setPresencePenalty(Integer presencePenalty) {
			this.presencePenalty = presencePenalty;
		}

	}

	public static class MemoryConfig {

		private Boolean enabled = false;

		private Integer windowSize = 20;

		private Boolean windowEnabled = true;

		private Boolean includeLastMessage = false;

		private String lastMessageTemplate;

		public Boolean getEnabled() {
			return enabled;
		}

		public MemoryConfig setEnabled(Boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Integer getWindowSize() {
			return windowSize;
		}

		public MemoryConfig setWindowSize(Integer windowSize) {
			this.windowSize = windowSize;
			return this;
		}

		public Boolean getWindowEnabled() {
			return windowEnabled;
		}

		public MemoryConfig setWindowEnabled(Boolean windowEnabled) {
			this.windowEnabled = windowEnabled;
			return this;
		}

		public Boolean getIncludeLastMessage() {
			return includeLastMessage;
		}

		public MemoryConfig setIncludeLastMessage(Boolean includeLastMessage) {
			this.includeLastMessage = includeLastMessage;
			return this;
		}

		public String getLastMessageTemplate() {
			return lastMessageTemplate;
		}

		public MemoryConfig setLastMessageTemplate(String lastMessageTemplate) {
			this.lastMessageTemplate = lastMessageTemplate;
			return this;
		}

	}

}
