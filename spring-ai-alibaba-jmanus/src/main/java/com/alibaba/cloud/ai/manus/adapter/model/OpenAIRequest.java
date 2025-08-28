/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.adapter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completion Request Model Compatible with OpenAI API specification for
 * integration with Cherry Studio
 */
public class OpenAIRequest {

	private String model;

	private List<Message> messages;

	private Double temperature;

	@JsonProperty("top_p")
	private Double topP;

	@JsonProperty("max_tokens")
	private Integer maxTokens;

	private Boolean stream;

	@JsonProperty("stream_options")
	private Object streamOptions;

	private List<Function> functions;

	@JsonProperty("function_call")
	private Object functionCall;

	private List<Tool> tools;

	@JsonProperty("tool_choice")
	private Object toolChoice;

	// Additional OpenAI standard parameters
	private String user;

	private Integer seed;

	private Integer n;

	private Object stop;

	@JsonProperty("frequency_penalty")
	private Double frequencyPenalty;

	@JsonProperty("presence_penalty")
	private Double presencePenalty;

	@JsonProperty("logit_bias")
	private Object logitBias;

	private Integer logprobs;

	@JsonProperty("top_logprobs")
	private Integer topLogprobs;

	// Constructors
	public OpenAIRequest() {
	}

	// Getters and Setters
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Double getTopP() {
		return topP;
	}

	public void setTopP(Double topP) {
		this.topP = topP;
	}

	public Integer getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
	}

	public Boolean getStream() {
		return stream;
	}

	public void setStream(Boolean stream) {
		this.stream = stream;
	}

	public Object getStreamOptions() {
		return streamOptions;
	}

	public void setStreamOptions(Object streamOptions) {
		this.streamOptions = streamOptions;
	}

	public List<Function> getFunctions() {
		return functions;
	}

	public void setFunctions(List<Function> functions) {
		this.functions = functions;
	}

	public Object getFunctionCall() {
		return functionCall;
	}

	public void setFunctionCall(Object functionCall) {
		this.functionCall = functionCall;
	}

	public List<Tool> getTools() {
		return tools;
	}

	public void setTools(List<Tool> tools) {
		this.tools = tools;
	}

	public Object getToolChoice() {
		return toolChoice;
	}

	public void setToolChoice(Object toolChoice) {
		this.toolChoice = toolChoice;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Integer getSeed() {
		return seed;
	}

	public void setSeed(Integer seed) {
		this.seed = seed;
	}

	public Integer getN() {
		return n;
	}

	public void setN(Integer n) {
		this.n = n;
	}

	public Object getStop() {
		return stop;
	}

	public void setStop(Object stop) {
		this.stop = stop;
	}

	public Double getFrequencyPenalty() {
		return frequencyPenalty;
	}

	public void setFrequencyPenalty(Double frequencyPenalty) {
		this.frequencyPenalty = frequencyPenalty;
	}

	public Double getPresencePenalty() {
		return presencePenalty;
	}

	public void setPresencePenalty(Double presencePenalty) {
		this.presencePenalty = presencePenalty;
	}

	public Object getLogitBias() {
		return logitBias;
	}

	public void setLogitBias(Object logitBias) {
		this.logitBias = logitBias;
	}

	public Integer getLogprobs() {
		return logprobs;
	}

	public void setLogprobs(Integer logprobs) {
		this.logprobs = logprobs;
	}

	public Integer getTopLogprobs() {
		return topLogprobs;
	}

	public void setTopLogprobs(Integer topLogprobs) {
		this.topLogprobs = topLogprobs;
	}

	// Inner Classes
	public static class Message {

		private String role;

		private Object content;

		private String name;

		@JsonProperty("function_call")
		private FunctionCall functionCall;

		@JsonProperty("tool_calls")
		private List<ToolCall> toolCalls;

		// Constructors
		public Message() {
		}

		public Message(String role, String content) {
			this.role = role;
			this.content = content;
		}

		public Message(String role, Object content) {
			this.role = role;
			this.content = content;
		}

		// Getters and Setters
		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getContent() {
			return extractTextContent(content);
		}

		public Object getRawContent() {
			return content;
		}

		public void setContent(Object content) {
			this.content = content;
		}

		private String extractTextContent(Object content) {
			if (content == null) {
				return null;
			}

			if (content instanceof String) {
				return (String) content;
			}

			if (content instanceof List<?>) {
				List<?> contentArray = (List<?>) content;
				StringBuilder textBuilder = new StringBuilder();

				for (Object item : contentArray) {
					if (item instanceof Map<?, ?>) {
						Map<?, ?> contentItem = (Map<?, ?>) item;
						String type = (String) contentItem.get("type");

						if ("text".equals(type)) {
							String text = (String) contentItem.get("text");
							if (text != null) {
								textBuilder.append(text);
							}
						}

					}
				}

				return textBuilder.toString();
			}

			return content.toString();
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public FunctionCall getFunctionCall() {
			return functionCall;
		}

		public void setFunctionCall(FunctionCall functionCall) {
			this.functionCall = functionCall;
		}

		public List<ToolCall> getToolCalls() {
			return toolCalls;
		}

		public void setToolCalls(List<ToolCall> toolCalls) {
			this.toolCalls = toolCalls;
		}

	}

	public static class Function {

		private String name;

		private String description;

		private Map<String, Object> parameters;

		// Constructors
		public Function() {
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Map<String, Object> getParameters() {
			return parameters;
		}

		public void setParameters(Map<String, Object> parameters) {
			this.parameters = parameters;
		}

	}

	public static class Tool {

		private String type;

		private Function function;

		// Constructors
		public Tool() {
		}

		// Getters and Setters
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Function getFunction() {
			return function;
		}

		public void setFunction(Function function) {
			this.function = function;
		}

	}

	public static class FunctionCall {

		private String name;

		private String arguments;

		// Constructors
		public FunctionCall() {
		}

		// Getters and Setters
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getArguments() {
			return arguments;
		}

		public void setArguments(String arguments) {
			this.arguments = arguments;
		}

	}

	public static class ToolCall {

		private String id;

		private String type;

		private Function function;

		// Constructors
		public ToolCall() {
		}

		// Getters and Setters
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Function getFunction() {
			return function;
		}

		public void setFunction(Function function) {
			this.function = function;
		}

	}

}
