/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dotprompt;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplateActions;
import org.springframework.ai.chat.prompt.PromptTemplateMessageActions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DotPromptTemplate implements PromptTemplateActions, PromptTemplateMessageActions {

	private final DotPromptLoader loader;

	private final DotPromptRenderer renderer;

	private final String promptName;

	private DotPrompt dotPrompt;

	private String model;

	private Map<String, Object> config;

	public DotPromptTemplate(DotPromptLoader loader, DotPromptRenderer renderer) {
		this.loader = loader;
		this.renderer = renderer;
		this.promptName = null;
	}

	public DotPromptTemplate(String promptName, DotPromptLoader loader, DotPromptRenderer renderer) {
		Assert.hasText(promptName, "promptName must not be empty");
		this.promptName = promptName;
		this.loader = loader;
		this.renderer = renderer;
	}

	@Override
	public Prompt create() {
		return create(Map.of());
	}

	@Override
	public Prompt create(ChatOptions modelOptions) {
		return null;
	}

	@Override
	public Prompt create(Map<String, Object> model) {
		return new Prompt(renderTemplate(model));
	}

	private String renderTemplate(Map<String, Object> model) {
		try {
			if (dotPrompt == null) {
				Assert.hasText(promptName,
						"promptName must be set either via constructor or by loading the prompt first");
				dotPrompt = loader.load(promptName);
			}

			if (this.model != null) {
				dotPrompt.withModel(this.model);
			}
			if (config != null) {
				dotPrompt.withConfig(config);
			}

			return renderer.render(dotPrompt, model);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load or render prompt: " + promptName, e);
		}
	}

	@Override
	public Prompt create(Map<String, Object> model, ChatOptions modelOptions) {
		return null;
	}

	@Override
	public Message createMessage() {
		return createMessage(Map.of());
	}

	@Override
	public Message createMessage(List<Media> mediaList) {
		return null;
	}

	@Override
	public Message createMessage(Map<String, Object> model) {
		return new UserMessage(renderTemplate(model));
	}

	public DotPromptTemplate withPrompt(String promptName) {
		return new DotPromptTemplate(promptName, this.loader, this.renderer);
	}

	public DotPromptTemplate withModel(String model) {
		this.model = model;
		return this;
	}

	public DotPromptTemplate withConfig(Map<String, Object> config) {
		this.config = config;
		return this;
	}

	public String getModel() {
		return dotPrompt != null ? (model != null ? model : dotPrompt.getModel()) : null;
	}

	public Map<String, Object> getConfig() {
		if (dotPrompt == null) {
			return config;
		}
		Map<String, Object> baseConfig = dotPrompt.getConfig();
		if (config != null && baseConfig != null) {
			baseConfig.putAll(config);
		}
		return baseConfig;
	}

	@Override
	public String render() {
		return "";
	}

	@Override
	public String render(Map<String, Object> model) {
		return "";
	}

}
