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

package com.alibaba.cloud.ai.example.deepresearch.model.multiagent;

/**
 * Agent类型枚举，定义不同类型的研究Agent
 *
 * @author Makoto
 * @since 2025/07/17
 */
public enum AgentType {

	ACADEMIC_RESEARCH("academic_research", "学术研究Agent", "专门处理学术论文、科研项目、技术研究、学术会议、期刊论文等学术相关问题",
			"prompts/multiagent/academic-researcher.md",
			"学术研究要求：请确保引用格式规范，优先引用高质量的学术资源。文末参考资料格式：\n- [论文标题 - 作者, 期刊/会议, 年份](URL)\n\n- [另一篇论文](URL)"),

	LIFESTYLE_TRAVEL("lifestyle_travel", "生活&旅游Agent", "专门处理生活服务、旅游攻略、美食推荐、购物指南、城市生活等相关问题",
			"prompts/multiagent/lifestyle-travel.md",
			"生活旅游指南：请提供实用的建议和最新信息，引用可靠的旅游平台和生活服务信息。文末参考资料格式：\n- [资源标题 - 平台名称](URL)\n\n- [另一个资源](URL)"),

	ENCYCLOPEDIA("encyclopedia", "百科Agent", "专门处理百科知识、概念解释、历史文化、科普知识、定义查询等问题", "prompts/multiagent/encyclopedia.md",
			"百科知识解答：请确保信息准确性，引用权威的百科和官方资源。文末参考资料格式：\n- [百科条目 - 来源](URL)\n\n- [权威资料](URL)"),

	DATA_ANALYSIS("data_analysis", "数据分析Agent", "专门处理数据分析、统计查询、市场研究、趋势分析、报告生成等问题",
			"prompts/multiagent/data-analysis.md",
			"数据分析报告：请引用官方统计数据和权威数据源，提供数据的时间范围和准确性说明。文末参考资料格式：\n- [数据来源 - 发布机构, 时间](URL)\n\n- [统计资料](URL)"),

	GENERAL_RESEARCH("general_research", "通用研究Agent", "处理无法明确分类的综合性研究问题，或需要多领域知识融合的复杂问题", null,
			"重要提示：请避免在文本中使用内联引用，而是在文末添加参考资料部分，使用链接引用格式。每个引用之间留空行以提高可读性。格式：\n- [资料标题](URL)\n\n- [另一个资料](URL)");

	private final String code;

	private final String name;

	private final String description;

	private final String promptFilePath;

	private final String citationGuidance;

	AgentType(String code, String name, String description, String promptFilePath, String citationGuidance) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.promptFilePath = promptFilePath;
		this.citationGuidance = citationGuidance;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getPromptFilePath() {
		return promptFilePath;
	}

	public String getCitationGuidance() {
		return citationGuidance;
	}

	public static AgentType parse(String code) {
		for (AgentType agentType : AgentType.values()) {
			if (agentType.code.equals(code)) {
				return agentType;
			}
		}
		throw new IllegalArgumentException("未找到Agent类型: " + code);
	}

}
