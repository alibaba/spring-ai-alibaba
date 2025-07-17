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

package com.alibaba.cloud.ai.example.deepresearch.model.mutiagent;

/**
 * Agent类型枚举，定义不同类型的研究Agent
 *
 * @author Makoto
 * @since 2025/07/17
 */
public enum AgentType {

	ACADEMIC_RESEARCH("academic_research", "学术研究Agent", "专门处理学术论文、科研项目、技术研究、学术会议、期刊论文等学术相关问题"),

	LIFESTYLE_TRAVEL("lifestyle_travel", "生活&旅游Agent", "专门处理生活服务、旅游攻略、美食推荐、购物指南、城市生活等相关问题"),

	ENCYCLOPEDIA("encyclopedia", "百科Agent", "专门处理百科知识、概念解释、历史文化、科普知识、定义查询等问题"),

	DATA_ANALYSIS("data_analysis", "数据分析Agent", "专门处理数据分析、统计查询、市场研究、趋势分析、报告生成等问题"),

	GENERAL_RESEARCH("general_research", "通用研究Agent", "处理无法明确分类的综合性研究问题，或需要多领域知识融合的复杂问题");

	private final String code;

	private final String name;

	private final String description;

	AgentType(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
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

}
