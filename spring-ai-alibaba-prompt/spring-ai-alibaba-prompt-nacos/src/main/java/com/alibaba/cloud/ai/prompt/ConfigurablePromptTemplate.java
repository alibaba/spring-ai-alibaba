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
package com.alibaba.cloud.ai.prompt;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.PromptTemplateActions;
import org.springframework.ai.chat.prompt.PromptTemplateMessageActions;
import org.springframework.ai.content.Media;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

public class ConfigurablePromptTemplate implements PromptTemplateActions, PromptTemplateMessageActions {

	private final PromptTemplate promptTemplate;

	private final String name;

	ConfigurablePromptTemplate(String name, Resource resource) {
		this(name, new PromptTemplate(resource));
	}

	ConfigurablePromptTemplate(String name, String template) {
		this(name, new PromptTemplate(template));
	}

	ConfigurablePromptTemplate(String name, String template, Map<String, Object> model) {
		this(name, PromptTemplate.builder().template(template).variables(model).build());
	}

	ConfigurablePromptTemplate(String name, Resource resource, Map<String, Object> model) {
		this(name, PromptTemplate.builder().resource(resource).variables(model).build());
	}

	ConfigurablePromptTemplate(String name, PromptTemplate promptTemplate) {
		this.promptTemplate = promptTemplate;
		this.name = name;
	}

	@Override
	public Prompt create() {
		return promptTemplate.create();
	}

	@Override
	public Prompt create(ChatOptions modelOptions) {
		return promptTemplate.create(modelOptions);
	}

	@Override
	public Prompt create(Map<String, Object> model) {
		return promptTemplate.create(model);
	}

	@Override
	public Prompt create(Map<String, Object> model, ChatOptions modelOptions) {
		return promptTemplate.create(model, modelOptions);
	}

	@Override
	public Message createMessage() {
		return promptTemplate.createMessage();
	}

	@Override
	public Message createMessage(List<Media> mediaList) {
		return promptTemplate.createMessage(mediaList);
	}

	@Override
	public Message createMessage(Map<String, Object> model) {
		return promptTemplate.createMessage(model);
	}

	@Override
	public String render() {
		return promptTemplate.render();
	}

	@Override
	public String render(Map<String, Object> model) {
		return promptTemplate.render(model);
	}

}
