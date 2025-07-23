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
import com.alibaba.cloud.ai.example.deepresearch.service.InfoCheckService;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchInfoService;
import com.alibaba.cloud.ai.example.deepresearch.service.SearchFilterService;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.SearchPlatformSelectionService;
import com.alibaba.cloud.ai.example.deepresearch.util.Multiagent.AgentIntegrationUtil;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.SmartAgentSelectionHelperService;
import com.alibaba.cloud.ai.example.deepresearch.service.mutiagent.QuestionClassifierService;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.toolcalling.jinacrawler.JinaCrawlerService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/17 18:37
 */

public class BackgroundInvestigationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(BackgroundInvestigationNode.class);

	private final InfoCheckService infoCheckService;

	private final SearchInfoService searchInfoService;

	private final SmartAgentSelectionHelperService smartAgentSelectionHelper;

	public BackgroundInvestigationNode(JinaCrawlerService jinaCrawlerService, InfoCheckService infoCheckService,
			SearchFilterService searchFilterService, QuestionClassifierService questionClassifierService,
			SearchPlatformSelectionService platformSelectionService, SmartAgentProperties smartAgentProperties) {
		this.searchInfoService = new SearchInfoService(jinaCrawlerService, searchFilterService);
		this.infoCheckService = infoCheckService;
		this.smartAgentSelectionHelper = AgentIntegrationUtil.createSelectionHelper(smartAgentProperties, null,
				questionClassifierService, platformSelectionService);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("background investigation node is running.");

		Map<String, Object> resultMap = new HashMap<>();
		List<List<Map<String, String>>> resultsList = new ArrayList<>();
		List<String> queries = StateUtil.getOptimizeQueries(state);
		assert queries != null && !queries.isEmpty();

		for (String query : queries) {
			// 如果mutiAgent功能开启且配置了专用搜索平台，则使用智能搜索引擎选择,否则使用默认的通用搜索引擎
			SearchEnum searchEnum = getSearchEnum(state, query);
			List<Map<String, String>> results = new ArrayList<>();

			results = searchInfoService.searchInfo(state.value("enable_search_filter", true), searchEnum, query);
			resultMap.put("site_information", results);
			resultsList.add(results);
		}

		if (!resultsList.isEmpty()) {
			List<String> backgroundResults = new ArrayList<>();
			assert resultsList.size() != queries.size();
			for (int i = 0; i < resultsList.size(); i++) {
				List<Map<String, String>> results = resultsList.get(i);
				String query = queries.get(i);
				// filter result
				String checkResults = infoCheckService.backgroundInfoCheck(results, query);

				String prompt = "background investigation query:\n" + query + "\n"
						+ "background investigation results:\n" + checkResults + "\n";

				backgroundResults.add(prompt);
			}
			logger.info("✅ 搜索结果: {} 组", backgroundResults.size());
			resultMap.put("background_investigation_results", backgroundResults);
		}
		else {
			logger.warn("⚠️ 搜索失败");
		}

		return resultMap;
	}

	/**
	 * 获取智能选择的搜索引擎
	 * @param state 全局状态
	 * @param query 查询内容
	 * @return 搜索引擎枚举
	 */
	private SearchEnum getSearchEnum(OverAllState state, String query) {
		return smartAgentSelectionHelper.intelligentSearchEngineSelection(state, query);
	}

}
