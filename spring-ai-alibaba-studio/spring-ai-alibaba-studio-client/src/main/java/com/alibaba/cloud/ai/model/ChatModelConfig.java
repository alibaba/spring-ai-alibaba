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

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import io.swagger.v3.oas.annotations.media.Schema;

public class ChatModelConfig {

	@Schema(description = "ChatModel bean name", examples = { "chatModel", "chatModel1" })
	private String name;

	@Schema(description = "dashscope model name",
			examples = { "qwen-plus", "qwen-turbo", "qwen-max", "qwen-max-longcontext" })
	private String model;

	private ModelType modelType;

	@Schema(nullable = true)
	private DashScopeChatOptions chatOptions;

	@Schema(nullable = true)
	private DashScopeImageOptions imageOptions;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public ModelType getModelType() {
		return modelType;
	}

	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}

	public DashScopeChatOptions getChatOptions() {
		return chatOptions;
	}

	public void setChatOptions(DashScopeChatOptions chatOptions) {
		this.chatOptions = chatOptions;
	}

	public DashScopeImageOptions getImageOptions() {
		return imageOptions;
	}

	public void setImageOptions(DashScopeImageOptions imageOptions) {
		this.imageOptions = imageOptions;
	}

	@Override
	public String toString() {
		return "ChatModelConfig{" + "name='" + name + '\'' + ", model='" + model + '\'' + ", modelType=" + modelType
				+ ", chatOptions=" + chatOptions + ", imageOptions=" + imageOptions + '}';
	}

	public static ChatModelConfigBuilder builder() {
		return new ChatModelConfigBuilder();
	}

	public static final class ChatModelConfigBuilder {

		private String name;

		private String model;

		private ModelType modelType;

		private DashScopeChatOptions chatOptions;

		private DashScopeImageOptions imageOptions;

		private ChatModelConfigBuilder() {
		}

		public static ChatModelConfigBuilder aChatModelConfig() {
			return new ChatModelConfigBuilder();
		}

		public ChatModelConfigBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ChatModelConfigBuilder model(String model) {
			this.model = model;
			return this;
		}

		public ChatModelConfigBuilder modelType(ModelType modelType) {
			this.modelType = modelType;
			return this;
		}

		public ChatModelConfigBuilder chatOptions(DashScopeChatOptions chatOptions) {
			this.chatOptions = chatOptions;
			return this;
		}

		public ChatModelConfigBuilder imageOptions(DashScopeImageOptions imageOptions) {
			this.imageOptions = imageOptions;
			return this;
		}

		public ChatModelConfig build() {
			ChatModelConfig chatModelConfig = new ChatModelConfig();
			chatModelConfig.setName(name);
			chatModelConfig.setModel(model);
			chatModelConfig.setModelType(modelType);
			chatModelConfig.setChatOptions(chatOptions);
			chatModelConfig.setImageOptions(imageOptions);
			return chatModelConfig;
		}

	}

}
