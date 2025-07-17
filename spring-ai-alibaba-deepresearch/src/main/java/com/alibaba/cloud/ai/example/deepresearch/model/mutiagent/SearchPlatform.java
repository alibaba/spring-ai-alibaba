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

import java.util.List;

/**
 * 搜索平台枚举，定义不同主题对应的搜索平台
 *
 * @author Makoto
 * @since 2025/07/17
 */
public enum SearchPlatform {

	GOOGLE_SCHOLAR("google_scholar", "谷歌学术", "学术论文、期刊文章、学术会议论文搜索", List.of(AgentType.ACADEMIC_RESEARCH)),

	XIAOHONGSHU("xiaohongshu", "小红书", "生活方式、旅游攻略、美食推荐、购物指南", List.of(AgentType.LIFESTYLE_TRAVEL)),

	WIKIPEDIA("wikipedia", "维基百科", "百科知识、概念解释、历史文化信息", List.of(AgentType.ENCYCLOPEDIA)),

	NATIONAL_STATISTICS("national_statistics", "国家统计局", "官方统计数据、经济指标、人口数据", List.of(AgentType.DATA_ANALYSIS)),

	GOOGLE_TRENDS("google_trends", "Google Trends", "搜索趋势、热度分析、话题流行度", List.of(AgentType.DATA_ANALYSIS)),

	BAIDU_INDEX("baidu_index", "百度指数", "中文搜索趋势、关键词热度、用户行为分析", List.of(AgentType.DATA_ANALYSIS)),

	TAVILY("tavily", "Tavily搜索", "通用web搜索引擎",
			List.of(AgentType.GENERAL_RESEARCH, AgentType.ACADEMIC_RESEARCH, AgentType.ENCYCLOPEDIA)),

	ALIYUN_AI_SEARCH("aliyun", "阿里云AI搜索", "智能搜索引擎", List.of(AgentType.GENERAL_RESEARCH, AgentType.ACADEMIC_RESEARCH)),

	BAIDU_SEARCH("baidu", "百度搜索", "通用搜索引擎", List.of(AgentType.GENERAL_RESEARCH, AgentType.LIFESTYLE_TRAVEL)),

	SERPAPI("serpapi", "SerpAPI", "综合搜索API服务",
			List.of(AgentType.GENERAL_RESEARCH, AgentType.ACADEMIC_RESEARCH, AgentType.DATA_ANALYSIS));

	private final String code;

	private final String name;

	private final String description;

	private final List<AgentType> supportedAgents;

	SearchPlatform(String code, String name, String description, List<AgentType> supportedAgents) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.supportedAgents = supportedAgents;
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

	public List<AgentType> getSupportedAgents() {
		return supportedAgents;
	}

}
