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
import java.util.Map;

public class LLMNodeData extends NodeData {

	public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("text", VariableType.STRING.value());

	private ModelConfig model;

	private List<PromptTemplate> promptTemplate;

	private MemoryConfig memoryConfig;

	private String systemPromptTemplate;

	private String userPromptTemplate;

	private String systemPromptTemplateKey;

	private String userPromptTemplateKey;

	private Map<String, Object> params;

	private String paramsKey;

	private List<Message> messages;

	private String messagesKey;

	private List<Advisor> advisors;

	private List<ToolCallback> toolCallbacks;

	private String outputKey;

	public LLMNodeData() {
	}

	public LLMNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public ModelConfig getModel() {
		return model;
	}

	public LLMNodeData setModel(ModelConfig model) {
		this.model = model;
		return this;
	}

	public List<PromptTemplate> getPromptTemplate() {
		return promptTemplate;
	}

	public LLMNodeData setPromptTemplate(List<PromptTemplate> promptTemplate) {
		this.promptTemplate = promptTemplate;
		return this;
	}

	public MemoryConfig getMemoryConfig() {
		return memoryConfig;
	}

	public LLMNodeData setMemoryConfig(MemoryConfig memoryConfig) {
		this.memoryConfig = memoryConfig;
		return this;
	}

	public String getSystemPromptTemplate() {
		return systemPromptTemplate;
	}

	public LLMNodeData setSystemPromptTemplate(String systemPromptTemplate) {
		this.systemPromptTemplate = systemPromptTemplate;
		return this;
	}

	public String getUserPromptTemplate() {
		return userPromptTemplate;
	}

	public LLMNodeData setUserPromptTemplate(String userPromptTemplate) {
		this.userPromptTemplate = userPromptTemplate;
		return this;
	}

	public String getSystemPromptTemplateKey() {
		return systemPromptTemplateKey;
	}

	public LLMNodeData setSystemPromptTemplateKey(String systemPromptTemplateKey) {
		this.systemPromptTemplateKey = systemPromptTemplateKey;
		return this;
	}

	public String getUserPromptTemplateKey() {
		return userPromptTemplateKey;
	}

	public LLMNodeData setUserPromptTemplateKey(String userPromptTemplateKey) {
		this.userPromptTemplateKey = userPromptTemplateKey;
		return this;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public LLMNodeData setParams(Map<String, Object> params) {
		this.params = params;
		return this;
	}

	public String getParamsKey() {
		return paramsKey;
	}

	public LLMNodeData setParamsKey(String paramsKey) {
		this.paramsKey = paramsKey;
		return this;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public LLMNodeData setMessages(List<Message> messages) {
		this.messages = messages;
		return this;
	}

	public String getMessagesKey() {
		return messagesKey;
	}

	public LLMNodeData setMessagesKey(String messagesKey) {
		this.messagesKey = messagesKey;
		return this;
	}

	public List<Advisor> getAdvisors() {
		return advisors;
	}

	public LLMNodeData setAdvisors(List<Advisor> advisors) {
		this.advisors = advisors;
		return this;
	}

	public List<ToolCallback> getToolCallbacks() {
		return toolCallbacks;
	}

	public LLMNodeData setToolCallbacks(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public LLMNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
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

		public String getRole() {
			return role;
		}

		public PromptTemplate setRole(String role) {
			this.role = role;
			return this;
		}

		public String getText() {
			return text;
		}

		public PromptTemplate setText(String text) {
			this.text = text;
			return this;
		}

	}

	public static class ModelConfig {

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

		public Integer getMaxTokens() {
			return maxTokens;
		}

		public CompletionParams setMaxTokens(Integer maxTokens) {
			this.maxTokens = maxTokens;
			return this;
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

	public static class Message {

		private String role;

		private String content;

		public Message() {
		}

		public Message(String role, String content) {
			this.role = role;
			this.content = content;
		}

		public String getRole() {
			return role;
		}

		public Message setRole(String role) {
			this.role = role;
			return this;
		}

		public String getContent() {
			return content;
		}

		public Message setContent(String content) {
			this.content = content;
			return this;
		}

		@Override
		public String toString() {
			return String.format("new Message(\"%s\", \"%s\")", role == null ? "" : role.replace("\"", "\\\""),
					content == null ? "" : content.replace("\"", "\\\""));
		}

	}

	public static class Advisor {

		private String name;

		private String prompt;

		public Advisor() {
		}

		public Advisor(String name, String prompt) {
			this.name = name;
			this.prompt = prompt;
		}

		public String getName() {
			return name;
		}

		public Advisor setName(String name) {
			this.name = name;
			return this;
		}

		public String getPrompt() {
			return prompt;
		}

		public Advisor setPrompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		@Override
		public String toString() {
			return String.format("new Advisor(\"%s\", \"%s\")", name == null ? "" : name.replace("\"", "\\\""),
					prompt == null ? "" : prompt.replace("\"", "\\\""));
		}

	}

	public static class ToolCallback {

		private String name;

		private Map<String, Object> args;

		public ToolCallback() {
		}

		public ToolCallback(String name, Map<String, Object> args) {
			this.name = name;
			this.args = args;
		}

		public String getName() {
			return name;
		}

		public ToolCallback setName(String name) {
			this.name = name;
			return this;
		}

		public Map<String, Object> getArgs() {
			return args;
		}

		public ToolCallback setArgs(Map<String, Object> args) {
			this.args = args;
			return this;
		}

		@Override
		public String toString() {
			return String.format("new ToolCallback(\"%s\", %s)", name == null ? "" : name.replace("\"", "\\\""),
					args == null ? "Map.of()" : args.toString().replace("\"", "\\\""));
		}

	}

}
