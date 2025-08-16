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
package com.alibaba.cloud.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatClient {

	@Schema(description = "ChatClient bean name", examples = { "chatClient", "chatClient1" })
	private String name;

	@Schema(description = "Default System Text",
			examples = { "You are a friendly chat bot that answers question in the voice of a {voice}" })
	private String defaultSystemText;

	@Schema(description = "Default System Params")
	private Map<String, Object> defaultSystemParams;

	@Schema(description = "ChatModel of ChatClient")
	private ChatModelConfig chatModel;

	private ChatOptions chatOptions;

	private List<Advisor> advisors;

	private Boolean isMemoryEnabled;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultSystemText() {
		return defaultSystemText;
	}

	public void setDefaultSystemText(String defaultSystemText) {
		this.defaultSystemText = defaultSystemText;
	}

	public Map<String, Object> getDefaultSystemParams() {
		return defaultSystemParams;
	}

	public void setDefaultSystemParams(Map<String, Object> defaultSystemParams) {
		this.defaultSystemParams = defaultSystemParams;
	}

	public ChatModelConfig getChatModel() {
		return chatModel;
	}

	public void setChatModel(ChatModelConfig chatModel) {
		this.chatModel = chatModel;
	}

	public ChatOptions getChatOptions() {
		return chatOptions;
	}

	public void setChatOptions(ChatOptions chatOptions) {
		this.chatOptions = chatOptions;
	}

	public List<Advisor> getAdvisors() {
		return advisors;
	}

	public void setAdvisors(List<Advisor> advisors) {
		this.advisors = advisors;
	}

	public Boolean getMemoryEnabled() {
		return isMemoryEnabled;
	}

	public void setIsMemoryEnabled(Boolean memoryEnabled) {
		isMemoryEnabled = memoryEnabled;
	}

	@Override
	public String toString() {
		return "ChatClient{" + "name='" + name + '\'' + ", defaultSystemText='" + defaultSystemText + '\''
				+ ", defaultSystemParams=" + defaultSystemParams + ", chatModel=" + chatModel + ", chatOptions="
				+ chatOptions + ", advisors=" + advisors + ", isMemoryEnabled=" + isMemoryEnabled + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ChatClient that = (ChatClient) o;
		return Objects.equals(name, that.name) && Objects.equals(defaultSystemText, that.defaultSystemText)
				&& Objects.equals(defaultSystemParams, that.defaultSystemParams)
				&& Objects.equals(chatModel, that.chatModel) && Objects.equals(chatOptions, that.chatOptions)
				&& Objects.equals(advisors, that.advisors) && Objects.equals(isMemoryEnabled, that.isMemoryEnabled);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, defaultSystemText, defaultSystemParams, chatModel, chatOptions, advisors,
				isMemoryEnabled);
	}

	public static ChatClientBuilder builder() {
		return new ChatClientBuilder();
	}

	public static final class ChatClientBuilder {

		private String name;

		private String defaultSystemText;

		private Map<String, Object> defaultSystemParams;

		private ChatModelConfig chatModel;

		private ChatOptions chatOptions;

		private List<Advisor> advisors;

		private Boolean isMemoryEnabled;

		private ChatClientBuilder() {
		}

		public static ChatClientBuilder aChatClient() {
			return new ChatClientBuilder();
		}

		public ChatClientBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ChatClientBuilder defaultSystemText(String defaultSystemText) {
			this.defaultSystemText = defaultSystemText;
			return this;
		}

		public ChatClientBuilder defaultSystemParams(Map<String, Object> defaultSystemParams) {
			this.defaultSystemParams = defaultSystemParams;
			return this;
		}

		public ChatClientBuilder chatModel(ChatModelConfig chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public ChatClientBuilder chatOptions(ChatOptions chatOptions) {
			this.chatOptions = chatOptions;
			return this;
		}

		public ChatClientBuilder advisors(List<Advisor> advisors) {
			this.advisors = advisors;
			return this;
		}

		public ChatClientBuilder isMemoryEnabled(Boolean isMemoryEnabled) {
			this.isMemoryEnabled = isMemoryEnabled;
			return this;
		}

		public ChatClient build() {
			ChatClient chatClient = new ChatClient();
			chatClient.setName(name);
			chatClient.setDefaultSystemText(defaultSystemText);
			chatClient.setDefaultSystemParams(defaultSystemParams);
			chatClient.setChatModel(chatModel);
			chatClient.setChatOptions(chatOptions);
			chatClient.setAdvisors(advisors);
			chatClient.isMemoryEnabled = this.isMemoryEnabled;
			return chatClient;
		}

	}

}
