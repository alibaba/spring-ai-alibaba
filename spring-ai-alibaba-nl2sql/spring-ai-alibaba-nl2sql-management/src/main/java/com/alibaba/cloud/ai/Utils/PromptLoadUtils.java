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
package com.alibaba.cloud.ai.Utils;

import com.alibaba.cloud.ai.entity.PromptTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 提示词加载工具类 提供提示词模板相关的通用工具方法
 */
public class PromptLoadUtils {

	/**
	 * 根据关键词搜索模板
	 * @param templates 模板列表
	 * @param keyword 搜索关键词
	 * @return 匹配的模板列表
	 */
	public static List<PromptTemplate> searchTemplates(List<PromptTemplate> templates, String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return templates;
		}

		String lowerKeyword = keyword.toLowerCase();
		return templates.stream()
			.filter(t -> t.getTemplateName().toLowerCase().contains(lowerKeyword)
					|| (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerKeyword))
					|| (t.getTemplateContent() != null && t.getTemplateContent().toLowerCase().contains(lowerKeyword)))
			.collect(Collectors.toList());
	}

	/**
	 * 根据类型筛选模板
	 * @param templates 模板列表
	 * @param templateType 模板类型
	 * @return 匹配类型的模板列表
	 */
	public static List<PromptTemplate> filterByType(List<PromptTemplate> templates, String templateType) {
		return templates.stream()
			.filter(t -> Objects.equals(t.getTemplateType(), templateType))
			.collect(Collectors.toList());
	}

	/**
	 * 验证模板内容是否有效
	 * @param template 模板对象
	 * @return 是否有效
	 */
	public static boolean isValidTemplate(PromptTemplate template) {
		return template != null && template.getTemplateName() != null && !template.getTemplateName().trim().isEmpty()
				&& template.getTemplateContent() != null && !template.getTemplateContent().trim().isEmpty();
	}

	/**
	 * 获取默认报告生成器模板内容
	 * @return 报告生成器模板内容
	 */
	public static String getDefaultReportGeneratorContent() {
		return """
				你是一个专业的数据分析报告生成器，擅长将复杂的数据查询结果转化为清晰、有洞察力的商业报告。

				## 角色定位
				- 数据分析专家：具备深厚的数据分析和解读能力
				- 商业顾问：能够从业务角度解读数据，提供有价值的建议
				- 报告撰写专家：擅长结构化、可视化地呈现分析结果

				## 任务要求
				根据用户需求、执行计划和实际数据结果，生成一份结构完整、内容丰富的分析报告。

				## 报告结构要求
				### 1. 执行摘要
				- 用户原始需求概述
				- 关键发现和核心结论
				- 重要数据指标汇总

				### 2. 数据分析过程
				- 执行的具体步骤说明
				- 使用的数据查询逻辑
				- 数据处理和分析方法

				### 3. 详细分析结果
				- 各步骤的具体数据展示
				- 数据趋势和规律分析
				- 异常值或特殊情况说明

				### 4. 业务洞察
				- 数据背后的业务含义
				- 可能的原因分析
				- 对业务的潜在影响

				### 5. 建议和行动计划
				- 基于分析结果的具体建议
				- 后续可采取的行动方案
				- 需要关注的关键指标

				## 输出格式要求
				- 使用Markdown格式输出
				- 包含适当的标题层级
				- 数据表格使用markdown表格格式
				- 重要内容使用加粗或高亮显示

				## 用户需求和计划
				{user_requirements_and_plan}

				## 分析步骤和数据结果
				{analysis_steps_and_data}

				## 总结建议要求
				{summary_and_recommendations}

				请根据以上信息生成一份专业、全面的数据分析报告。
				""";
	}

	/**
	 * 获取默认的执行计划生成器内容
	 * @return 执行计划生成器模板内容
	 */
	public static String getDefaultPlannerContent() {
		return """
				# ROLE: Senior Data Analysis Agent

				You are a Senior Data Analysis Agent. Your primary function is to interpret a user's business question and create a complete, step-by-step execution plan to answer it.

				**CRITICAL: You MUST only output a valid JSON object. Do not include any explanations, comments, or additional text outside the JSON structure.**

				# CORE TASK
				1. **Deconstruct the Request**: Deeply analyze the user's question to understand the core business objective.
				2. **Analyze Provided Schema**: Verify that all the columns needed for your analysis exist.
				3. **Formulate a Strategy**: Create a logical, multi-step plan.
				4. **Generate the Plan**: Output the strategy as a structured JSON object.

				# OUTPUT FORMAT (MUST be a valid JSON object)
				```json
				{
				  "thought_process": "A brief, narrative summary of your analysis strategy.",
				  "execution_plan": [
				    {
				      "step": 1,
				      "tool_to_use": "tool_name",
				      "tool_parameters": {
				        "param1": "value1",
				        "description": "A human-readable description of what this specific tool call does."
				      }
				    }
				  ]
				}
				```
				""";
	}

	/**
	 * 创建默认的提示词模板
	 * @param templateName 模板名称
	 * @param templateType 模板类型
	 * @param content 模板内容
	 * @param description 模板描述
	 * @param enabled 是否启用
	 * @param isDefault 是否为默认模板
	 * @return 创建的PromptTemplate对象
	 */
	public static PromptTemplate createDefaultTemplate(String templateName, String templateType, String content,
			String description, boolean enabled, boolean isDefault) {
		PromptTemplate template = new PromptTemplate();
		template.setTemplateName(templateName);
		template.setTemplateType(templateType);
		template.setTemplateContent(content);
		template.setDescription(description);
		template.setEnabled(enabled);
		template.setIsDefault(isDefault);
		return template;
	}

}
