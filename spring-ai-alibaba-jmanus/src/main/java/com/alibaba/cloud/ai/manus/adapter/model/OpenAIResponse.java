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

/**
 * OpenAI Chat Completion Response Model Compatible with OpenAI API specification for
 * integration with Cherry Studio
 */
public class OpenAIResponse {

	private String id;

	private String object;

	private Long created;

	private String model;

	@JsonProperty("system_fingerprint")
	private String systemFingerprint;

	private List<Choice> choices;

	private Usage usage;

	// Constructors
	public OpenAIResponse() {
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getSystemFingerprint() {
		return systemFingerprint;
	}

	public void setSystemFingerprint(String systemFingerprint) {
		this.systemFingerprint = systemFingerprint;
	}

	public List<Choice> getChoices() {
		return choices;
	}

	public void setChoices(List<Choice> choices) {
		this.choices = choices;
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	// Inner Classes
	public static class Choice {

		private Integer index;

		private Message message;

		private Delta delta;

		@JsonProperty("finish_reason")
		private String finishReason;

		private Logprobs logprobs;

		// Constructors
		public Choice() {
		}

		// Getters and Setters
		public Integer getIndex() {
			return index;
		}

		public void setIndex(Integer index) {
			this.index = index;
		}

		public Message getMessage() {
			return message;
		}

		public void setMessage(Message message) {
			this.message = message;
		}

		public Delta getDelta() {
			return delta;
		}

		public void setDelta(Delta delta) {
			this.delta = delta;
		}

		public String getFinishReason() {
			return finishReason;
		}

		public void setFinishReason(String finishReason) {
			this.finishReason = finishReason;
		}

		public Logprobs getLogprobs() {
			return logprobs;
		}

		public void setLogprobs(Logprobs logprobs) {
			this.logprobs = logprobs;
		}

	}

	public static class Message {

		private String role;

		private String content;

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

		// Getters and Setters
		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
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

	public static class Delta {

		private String role;

		private String content;

		@JsonProperty("function_call")
		private FunctionCall functionCall;

		@JsonProperty("tool_calls")
		private List<ToolCall> toolCalls;

		// Constructors
		public Delta() {
		}

		public Delta(String role, String content) {
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
			return content;
		}

		public void setContent(String content) {
			this.content = content;
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

	public static class Usage {

		@JsonProperty("prompt_tokens")
		private Integer promptTokens;

		@JsonProperty("completion_tokens")
		private Integer completionTokens;

		@JsonProperty("total_tokens")
		private Integer totalTokens;

		@JsonProperty("prompt_tokens_details")
		private PromptTokensDetails promptTokensDetails;

		@JsonProperty("completion_tokens_details")
		private CompletionTokensDetails completionTokensDetails;

		// Constructors
		public Usage() {
		}

		// Getters and Setters
		public Integer getPromptTokens() {
			return promptTokens;
		}

		public void setPromptTokens(Integer promptTokens) {
			this.promptTokens = promptTokens;
		}

		public Integer getCompletionTokens() {
			return completionTokens;
		}

		public void setCompletionTokens(Integer completionTokens) {
			this.completionTokens = completionTokens;
		}

		public Integer getTotalTokens() {
			return totalTokens;
		}

		public void setTotalTokens(Integer totalTokens) {
			this.totalTokens = totalTokens;
		}

		public PromptTokensDetails getPromptTokensDetails() {
			return promptTokensDetails;
		}

		public void setPromptTokensDetails(PromptTokensDetails promptTokensDetails) {
			this.promptTokensDetails = promptTokensDetails;
		}

		public CompletionTokensDetails getCompletionTokensDetails() {
			return completionTokensDetails;
		}

		public void setCompletionTokensDetails(CompletionTokensDetails completionTokensDetails) {
			this.completionTokensDetails = completionTokensDetails;
		}

	}

	public static class PromptTokensDetails {

		@JsonProperty("cached_tokens")
		private Integer cachedTokens;

		@JsonProperty("audio_tokens")
		private Integer audioTokens;

		// Constructors
		public PromptTokensDetails() {
		}

		// Getters and Setters
		public Integer getCachedTokens() {
			return cachedTokens;
		}

		public void setCachedTokens(Integer cachedTokens) {
			this.cachedTokens = cachedTokens;
		}

		public Integer getAudioTokens() {
			return audioTokens;
		}

		public void setAudioTokens(Integer audioTokens) {
			this.audioTokens = audioTokens;
		}

	}

	public static class CompletionTokensDetails {

		@JsonProperty("reasoning_tokens")
		private Integer reasoningTokens;

		@JsonProperty("audio_tokens")
		private Integer audioTokens;

		@JsonProperty("accepted_prediction_tokens")
		private Integer acceptedPredictionTokens;

		@JsonProperty("rejected_prediction_tokens")
		private Integer rejectedPredictionTokens;

		// Constructors
		public CompletionTokensDetails() {
		}

		// Getters and Setters
		public Integer getReasoningTokens() {
			return reasoningTokens;
		}

		public void setReasoningTokens(Integer reasoningTokens) {
			this.reasoningTokens = reasoningTokens;
		}

		public Integer getAudioTokens() {
			return audioTokens;
		}

		public void setAudioTokens(Integer audioTokens) {
			this.audioTokens = audioTokens;
		}

		public Integer getAcceptedPredictionTokens() {
			return acceptedPredictionTokens;
		}

		public void setAcceptedPredictionTokens(Integer acceptedPredictionTokens) {
			this.acceptedPredictionTokens = acceptedPredictionTokens;
		}

		public Integer getRejectedPredictionTokens() {
			return rejectedPredictionTokens;
		}

		public void setRejectedPredictionTokens(Integer rejectedPredictionTokens) {
			this.rejectedPredictionTokens = rejectedPredictionTokens;
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

	public static class Function {

		private String name;

		private String arguments;

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

		public String getArguments() {
			return arguments;
		}

		public void setArguments(String arguments) {
			this.arguments = arguments;
		}

	}

	public static class Logprobs {

		private List<TokenLogprob> content;

		// Constructors
		public Logprobs() {
		}

		// Getters and Setters
		public List<TokenLogprob> getContent() {
			return content;
		}

		public void setContent(List<TokenLogprob> content) {
			this.content = content;
		}

	}

	public static class TokenLogprob {

		private String token;

		private Double logprob;

		private List<Integer> bytes;

		@JsonProperty("top_logprobs")
		private List<TopLogprob> topLogprobs;

		// Constructors
		public TokenLogprob() {
		}

		// Getters and Setters
		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public Double getLogprob() {
			return logprob;
		}

		public void setLogprob(Double logprob) {
			this.logprob = logprob;
		}

		public List<Integer> getBytes() {
			return bytes;
		}

		public void setBytes(List<Integer> bytes) {
			this.bytes = bytes;
		}

		public List<TopLogprob> getTopLogprobs() {
			return topLogprobs;
		}

		public void setTopLogprobs(List<TopLogprob> topLogprobs) {
			this.topLogprobs = topLogprobs;
		}

	}

	public static class TopLogprob {

		private String token;

		private Double logprob;

		private List<Integer> bytes;

		// Constructors
		public TopLogprob() {
		}

		// Getters and Setters
		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public Double getLogprob() {
			return logprob;
		}

		public void setLogprob(Double logprob) {
			this.logprob = logprob;
		}

		public List<Integer> getBytes() {
			return bytes;
		}

		public void setBytes(List<Integer> bytes) {
			this.bytes = bytes;
		}

	}

}
