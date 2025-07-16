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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.nacos.annotation.NacosConfigListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class ConfigurablePromptTemplateFactory {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurablePromptTemplateFactory.class);

	private final Map<String, ConfigurablePromptTemplate> templates = new ConcurrentHashMap<>();

	private final PromptTemplateBuilderConfigure promptTemplateBuilderConfigure;

	public ConfigurablePromptTemplateFactory(PromptTemplateBuilderConfigure promptTemplateBuilderConfigure) {
		this.promptTemplateBuilderConfigure = promptTemplateBuilderConfigure;
	}

	public ConfigurablePromptTemplate create(String name, Resource resource) {
		return templates.computeIfAbsent(name, k -> new ConfigurablePromptTemplate(name, resource));
	}

	public ConfigurablePromptTemplate create(String name, String template) {
		return templates.computeIfAbsent(name, k -> new ConfigurablePromptTemplate(name, template));
	}

	public ConfigurablePromptTemplate create(String name, String template, Map<String, Object> model) {
		return templates.computeIfAbsent(name, k -> new ConfigurablePromptTemplate(name, template, model));
	}

	public ConfigurablePromptTemplate create(String name, Resource resource, Map<String, Object> model) {
		return templates.computeIfAbsent(name, k -> new ConfigurablePromptTemplate(name, resource, model));
	}

	public ConfigurablePromptTemplate create(String name, PromptTemplate promptTemplate) {
		return templates.computeIfAbsent(name, k -> new ConfigurablePromptTemplate(name, promptTemplate));
	}

	@NacosConfigListener(dataId = "spring.ai.alibaba.configurable.prompt", group = "DEFAULT_GROUP", initNotify = true)
	protected void onConfigChange(List<ConfigurablePromptTemplateModel> configList) {
		if (CollectionUtils.isEmpty(configList)) {
			return;
		}
		for (ConfigurablePromptTemplateModel configuration : configList) {
			if (!StringUtils.hasText(configuration.name()) || !StringUtils.hasText(configuration.template())) {
				continue;
			}
			PromptTemplate.Builder promptTemplateBuilder = promptTemplateBuilderConfigure
				.configure(PromptTemplate.builder()
					.template(configuration.template())
					.variables(configuration.model() == null ? new HashMap<>() : configuration.model()));

			templates.put(configuration.name(),
					new ConfigurablePromptTemplate(configuration.name(), promptTemplateBuilder.build()));

			logger.info("OnPromptTemplateConfigChange,templateName:{},template:{},model:{}", configuration.name(),
					configuration.template(), configuration.model());
		}
	}

	public ConfigurablePromptTemplate getTemplate(String name) {
		return templates.get(name);
	}

	public record ConfigurablePromptTemplateModel(String name, String template, Map<String, Object> model) {

	}

}
