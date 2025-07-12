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

import com.alibaba.cloud.ai.example.deepresearch.model.ParallelEnum;
import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.service.ReportService;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ReportGeneratorNode 用于将所有步骤的结果汇总
 *
 * @author Makoto
 * @since 2025/7/11
 */
public class ReportGeneratorNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorNode.class);

	private final ChatClient reportGeneratorAgent;

	private final ReportService reportService;

	private final DeepResearchProperties deepResearchProperties;

	private static final String RESEARCH_SUMMARY_FORMAT = "# 研究报告概要\n\n## 任务\n\n{0}\n\n## 描述\n\n{1}\n\n## 生成综合分析报告\n\n请根据以下所有步骤收集的结果，生成一份完整的、条理清晰的综合分析报告。";

	public ReportGeneratorNode(ChatClient reportGeneratorAgent, ReportService reportService,
			DeepResearchProperties deepResearchProperties) {
		this.reportGeneratorAgent = reportGeneratorAgent;
		this.reportService = reportService;
		this.deepResearchProperties = deepResearchProperties;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Plan currentPlan = state.value("current_plan", Plan.class)
			.orElseThrow(() -> new IllegalArgumentException("current_plan is missing"));

		String threadId = state.value("thread_id", String.class)
			.orElseThrow(() -> new IllegalArgumentException("thread_id is missing from state"));

		List<Message> messages = new ArrayList<>();

		messages.add(new UserMessage(
				MessageFormat.format(RESEARCH_SUMMARY_FORMAT, currentPlan.getTitle(), currentPlan.getThought())));

		// 添加背景调查结果
		if (state.value("enable_background_investigation", true)) {
			List<String> backgroundInvestigationResults = state.value("background_investigation_results",
					(List<String>) null);
			if (backgroundInvestigationResults != null && !backgroundInvestigationResults.isEmpty()) {
				messages.add(new UserMessage("## 背景调查结果"));
				for (String result : backgroundInvestigationResults) {
					if (StringUtils.hasText(result)) {
						messages.add(new UserMessage(result));
					}
				}
			}
		}

		// 添加planner步骤结果
		String planResults = state.value("planning_results", String.class).orElse(null);
		if (StringUtils.hasText(planResults)) {
			messages.add(new UserMessage("## 规划步骤结果\n\n" + planResults));
		}

		// 添加researcher和coder节点返回的信息
		List<String> researcherTeam = List.of(ParallelEnum.RESEARCHER.getValue(), ParallelEnum.CODER.getValue());

		List<String> teamContents = StateUtil.getParallelMessages(state, researcherTeam,
				StateUtil.getMaxStepNum(state));
		if (!teamContents.isEmpty()) {
			messages.add(new UserMessage("## 研究和代码分析结果"));
			for (String content : teamContents) {

				if (StringUtils.hasText(content)) {
					messages.add(new UserMessage(content));
				}
			}
		}

		// 添加final_report结果
		String finalReport = state.value("final_report", String.class).orElse(null);
		if (StringUtils.hasText(finalReport)) {
			messages.add(new UserMessage("## 已有的分析报告\n\n" + finalReport));
		}

		// 添加图表和可视化数据
		List<String> visualizationPaths = state.value("visualization_paths", (List<String>) null);
		if (visualizationPaths != null && !visualizationPaths.isEmpty()) {
			messages.add(new UserMessage("## 图表和可视化数据"));
			for (String path : visualizationPaths) {
				if (StringUtils.hasText(path)) {
					messages.add(new UserMessage("图表路径: " + path));
				}
			}
		}

		// 添加表格数据
		List<String> tableDatas = state.value("table_data", (List<String>) null);
		if (tableDatas != null && !tableDatas.isEmpty()) {
			messages.add(new UserMessage("## 表格数据"));
			for (String tableData : tableDatas) {
				if (StringUtils.hasText(tableData)) {
					messages.add(new UserMessage(tableData));
				}
			}
		}

		// 生成报告
		var streamResult = reportGeneratorAgent.prompt().messages(messages).stream().chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode("report_generator_llm_stream")
			.startingState(state)
			.mapResult(response -> {
				String comprehensiveReport = Objects.requireNonNull(response.getResult().getOutput().getText());
				reportService.saveReport(threadId + "_comprehensive", comprehensiveReport);
				logger.info("Comprehensive report saved successfully, Thread ID: {}", threadId);
				return Map.of("comprehensive_report", comprehensiveReport, "thread_id", threadId);
			})
			.build(streamResult);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("comprehensive_report", generator);
		resultMap.put("thread_id", threadId);
		return resultMap;
	}

}
