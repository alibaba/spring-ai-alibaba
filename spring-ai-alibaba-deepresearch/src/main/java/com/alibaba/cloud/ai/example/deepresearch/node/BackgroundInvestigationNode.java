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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.config.SmartAgentProperties;
import com.alibaba.cloud.ai.example.deepresearch.model.SessionHistory;
import com.alibaba.cloud.ai.example.deepresearch.service.InfoCheckService;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchFilterService;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchInfoService;
import com.alibaba.cloud.ai.example.deepresearch.service.SessionContextService;
import com.alibaba.cloud.ai.example.deepresearch.service.multiagent.SearchPlatformSelectionService;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.AgentIntegrationUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.example.deepresearch.service.multiagent.ToolCallingSearchService;
import com.alibaba.cloud.ai.example.deepresearch.service.multiagent.SmartAgentSelectionHelperService;
import com.alibaba.cloud.ai.example.deepresearch.service.multiagent.QuestionClassifierService;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @since 2025/5/17 18:37
 */

public class BackgroundInvestigationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(BackgroundInvestigationNode.class);

	private final InfoCheckService infoCheckService;

	private final SearchInfoService searchInfoService;

	private final SmartAgentSelectionHelperService smartAgentSelectionHelper;

	private final SessionContextService sessionContextService;

	private final ChatClient backgroundAgent;

	public BackgroundInvestigationNode(JinaCrawlerService jinaCrawlerService, InfoCheckService infoCheckService,
			SearchFilterService searchFilterService, QuestionClassifierService questionClassifierService,
			SearchPlatformSelectionService platformSelectionService, SmartAgentProperties smartAgentProperties,
			ChatClient backgroundAgent, SessionContextService sessionContextService,
			ToolCallingSearchService toolCallingSearchService) {
		this.searchInfoService = new SearchInfoService(jinaCrawlerService, searchFilterService,
				toolCallingSearchService);
		this.infoCheckService = infoCheckService;
		this.smartAgentSelectionHelper = AgentIntegrationUtil.createSelectionHelper(smartAgentProperties, null,
				questionClassifierService, platformSelectionService);
		this.backgroundAgent = backgroundAgent;
		this.sessionContextService = sessionContextService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");

		Map<String, Object> resultMap = new HashMap<>();
		List<List<Map<String, String>>> resultsList = new ArrayList<>();
		List<String> queries = StateUtil.getOptimizeQueries(state);
		assert queries != null && !queries.isEmpty();

		for (String query : queries) {
			// 使用统一的智能搜索选择方法
			SmartAgentUtil.SearchSelectionResult searchSelection = smartAgentSelectionHelper
				.intelligentSearchSelection(state, query);
			List<Map<String, String>> results;

			// 使用支持工具调用的搜索方法
			results = searchInfoService.searchInfo(StateUtil.isSearchFilter(state), searchSelection.getSearchEnum(),
					query, searchSelection.getSearchPlatform());
			resultsList.add(results);
		}
		resultMap.put("site_information", resultsList);

		List<String> backgroundResults = new ArrayList<>();
		assert resultsList.size() == queries.size();

		for (int i = 0; i < resultsList.size(); i++) {
			List<Map<String, String>> searchResults = resultsList.get(i);

			String query = queries.get(i);

			Message messages = new UserMessage(
					"搜索问题:" + query + "\n" + "以下是搜索结果：\n\n" + searchResults.stream().map(r -> {
						return String.format("标题: %s\n权重: %s\n内容: %s\n", r.get("title"), r.get("weight"),
								r.get("content"));
					}).collect(Collectors.joining("\n\n")));

			String sessionId = state.value("session_id", String.class).orElse("__default__");
			List<SessionHistory> reports = sessionContextService.getRecentReports(sessionId);
			Message lastReportMessage;
			if (reports != null && !reports.isEmpty()) {
				lastReportMessage = new AssistantMessage("这是用户前几次使用DeepResearch的报告：\r\n"
						+ reports.stream().map(SessionHistory::toString).collect(Collectors.joining("\r\n\r\n")));
			}
			else {
				lastReportMessage = new AssistantMessage("这是用户的第一次询问，因此没有上下文。");
			}

			String content = backgroundAgent.prompt().messages(lastReportMessage, messages).call().content();

			backgroundResults.add(content);

			logger.info("背景调查报告生成已完成: {}", backgroundResults.size());
		}
		resultMap.put("background_investigation_results", backgroundResults);

		String nextStep = "planner";
		if (!StateUtil.isDeepresearch(state)) {
			nextStep = "reporter";
		}
		resultMap.put("background_investigation_next_node", nextStep);
		return resultMap;
	}

}
