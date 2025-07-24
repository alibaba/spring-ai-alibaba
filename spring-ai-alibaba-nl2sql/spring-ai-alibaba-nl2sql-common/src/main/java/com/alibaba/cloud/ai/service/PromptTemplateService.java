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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.PromptTemplate;
import com.alibaba.cloud.ai.util.PromptLoadUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 提示词模板管理服务
 */
@Service
public class PromptTemplateService {

	private final Map<Long, PromptTemplate> templateStore = new ConcurrentHashMap<>();

	private final AtomicLong idGenerator = new AtomicLong(1);

	public PromptTemplateService() {
		initDefaultTemplates();
	}

	private void initDefaultTemplates() {
		PromptTemplate reportGeneratorTemplate = PromptLoadUtils.createDefaultTemplate("report-generator", "report",
				PromptLoadUtils.getDefaultReportGeneratorContent(), "默认报告生成器模板", true, true);
		save(reportGeneratorTemplate);

		PromptTemplate plannerTemplate = PromptLoadUtils.createDefaultTemplate("planner", "system",
				PromptLoadUtils.getDefaultPlannerContent(), "默认计划生成器模板", true, true);
		save(plannerTemplate);
	}

	public List<PromptTemplate> findAll() {
		return new ArrayList<>(templateStore.values());
	}

	public List<PromptTemplate> findByType(String templateType) {
		return PromptLoadUtils.filterByType(new ArrayList<>(templateStore.values()), templateType);
	}

	public PromptTemplate findById(Long id) {
		return templateStore.get(id);
	}

	public PromptTemplate findByName(String templateName) {
		return templateStore.values()
			.stream()
			.filter(t -> Objects.equals(t.getTemplateName(), templateName))
			.findFirst()
			.orElse(null);
	}

	public PromptTemplate save(PromptTemplate template) {
		if (!PromptLoadUtils.isValidTemplate(template)) {
			throw new IllegalArgumentException("模板内容无效：模板名称和内容不能为空");
		}

		if (template.getId() == null) {
			template.setId(idGenerator.getAndIncrement());
			template.setCreateTime(LocalDateTime.now());
		}
		template.setUpdateTime(LocalDateTime.now());
		templateStore.put(template.getId(), template);

		return template;
	}

	public void deleteById(Long id) {
		templateStore.remove(id);
	}

	public List<PromptTemplate> search(String keyword) {
		return PromptLoadUtils.searchTemplates(new ArrayList<>(templateStore.values()), keyword);
	}

	public void batchUpdateEnabled(String templateType, boolean enabled) {
		templateStore.values().stream().filter(t -> Objects.equals(t.getTemplateType(), templateType)).forEach(t -> {
			t.setEnabled(enabled);
			t.setUpdateTime(LocalDateTime.now());
		});
	}

	/**
	 * 获取启用的模板内容
	 */
	public String getEnabledTemplateContent(String templateName) {
		PromptTemplate template = templateStore.values()
			.stream()
			.filter(t -> Objects.equals(t.getTemplateName(), templateName) && Boolean.TRUE.equals(t.getEnabled()))
			.findFirst()
			.orElse(null);
		return template != null ? template.getTemplateContent() : null;
	}

	/**
	 * 热更新模板
	 */
	public boolean hotUpdateTemplate(String templateName, String newContent) {
		PromptTemplate template = templateStore.values()
			.stream()
			.filter(t -> Objects.equals(t.getTemplateName(), templateName))
			.findFirst()
			.orElse(null);
		if (template != null) {
			template.setTemplateContent(newContent);
			template.setUpdateTime(LocalDateTime.now());
			return true;
		}
		return false;
	}

}
