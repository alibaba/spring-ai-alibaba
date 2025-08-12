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

import java.util.List;

/**
 * 搜索平台枚举，定义不同主题对应的搜索平台
 *
 * @author Makoto
 * @since 2025/07/17
 */
public enum SearchPlatform {

	// 学术研究专用工具
	OPENALEX("openalex", "OpenAlex学术搜索", "学术论文、期刊文章、学术会议论文搜索", List.of(AgentType.ACADEMIC_RESEARCH)),

	// 旅游生活专用工具
	OPENTRIPMAP("opentripmap", "OpenTripMap景点搜索", "全球旅游景点和设施信息", List.of(AgentType.LIFESTYLE_TRAVEL)),

	TRIPADVISOR("tripadvisor", "TripAdvisor旅游搜索", "旅游景点、酒店、餐厅信息", List.of(AgentType.LIFESTYLE_TRAVEL)),

	// 百科知识专用工具
	WIKIPEDIA("wikipedia", "维基百科", "百科知识、概念解释、历史文化信息", List.of(AgentType.ENCYCLOPEDIA)),

	// 数据分析专用工具
	WORLDBANK_DATA("worldbankdata", "世界银行数据", "全球发展指标和国家统计数据", List.of(AgentType.DATA_ANALYSIS)),

	GOOGLE_SCHOLAR("googlescholar", "Google Scholar", "学术论文、引用分析、学者信息", List.of(AgentType.ACADEMIC_RESEARCH)),

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
