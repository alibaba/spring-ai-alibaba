package com.alibaba.cloud.ai.prompt;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.PromptTemplateActions;
import org.springframework.ai.chat.prompt.PromptTemplateMessageActions;
import org.springframework.ai.model.Media;
import org.springframework.core.io.Resource;

public class ConfigurablePromptTemplate implements PromptTemplateActions, PromptTemplateMessageActions {

	private PromptTemplate promptTemplate;

	private String name;

	ConfigurablePromptTemplate(String name, Resource resource) {
		this.promptTemplate = new PromptTemplate(resource);
		this.name = name;
	}

	ConfigurablePromptTemplate(String name, String template) {
		this.promptTemplate = new PromptTemplate(template);
		this.name = name;
	}

	ConfigurablePromptTemplate(String name, String template, Map<String, Object> model) {
		this.promptTemplate = new PromptTemplate(template, model);
		this.name = name;
	}

	ConfigurablePromptTemplate(String name, Resource resource, Map<String, Object> model) {
		this.promptTemplate = new PromptTemplate(resource, model);
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