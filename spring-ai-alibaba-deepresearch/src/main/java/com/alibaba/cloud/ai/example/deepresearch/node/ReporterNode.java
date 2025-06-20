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
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
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
 * @author yingzi
 * @since 2025/5/18 15:58
 */

public class ReporterNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ReporterNode.class);

	private final ChatClient reporterAgent;

	private final ReportService reportService;

	private static final String RESEARCH_FORMAT = "# Research Requirements\n\n## Task\n\n{0}\n\n## Description\n\n{1}";

	private final String REPORT_FORMAT = "IMPORTANT: Structure your report according to the format in the prompt. Remember to include:\n\n1. Key Points - A bulleted list of the most important findings\n2. Overview - A brief introduction to the topic\n3. Detailed Analysis - Organized into logical sections\n4. Survey Note (optional) - For more comprehensive reports\n5. Key Citations - List all references at the end\n\nFor citations, DO NOT include inline citations in the text. Instead, place all citations in the 'Key Citations' section at the end using the format: `- [Source Title](URL)`. Include an empty line between each citation for better readability.\n\nPRIORITIZE USING MARKDOWN TABLES for data presentation and comparison. Use tables whenever presenting comparative data, statistics, features, or options. Structure tables with clear headers and aligned columns. Example table format:\n\n| Feature | Description | Pros | Cons |\n|---------|-------------|------|------|\n| Feature 1 | Description 1 | Pros 1 | Cons 1 |\n| Feature 2 | Description 2 | Pros 2 | Cons 2 |";

	public ReporterNode(ChatClient reporterAgent, ReportService reportService) {
		this.reporterAgent = reporterAgent;
		this.reportService = reportService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("reporter node is running.");
		Plan currentPlan = state.value("current_plan", Plan.class)
			.orElseThrow(() -> new IllegalArgumentException("current_plan is missing"));

		// 从 OverAllState 中获取线程ID
		String threadId = state.value("thread_id", String.class)
			.orElseThrow(() -> new IllegalArgumentException("thread_id is missing from state"));
		logger.info("Thread ID from state: {}", threadId);

		// 1. 添加消息
		List<Message> messages = new ArrayList<>();
		// 1.1 添加预置提示消息
		messages.add(TemplateUtil.getMessage("reporter"));
		// 1.2 研究报告格式消息
		messages.add(new UserMessage(
				MessageFormat.format(RESEARCH_FORMAT, currentPlan.getTitle(), currentPlan.getThought())));
		messages.add(new UserMessage(REPORT_FORMAT));
		// 1.3 添加背景调查的消息
		String backgroundInvestigationResults = state.value("background_investigation_results", "");
		if (StringUtils.hasText(backgroundInvestigationResults)) {
			messages.add(new UserMessage(backgroundInvestigationResults));
		}
		// 1.4 添加研究组节点返回的信息
		List<String> researcherTeam = List.of(ParallelEnum.RESEARCHER.getValue(), ParallelEnum.CODER.getValue());
		for (String content : StateUtil.getParallelMessages(state, researcherTeam, StateUtil.getMaxStepNum(state))) {
			logger.info("researcherTeam_content: {}", content);
			messages.add(new UserMessage(content));
		}

		logger.debug("reporter node messages: {}", messages);

		var streamResult = reporterAgent.prompt().messages(messages).stream().chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode("reporter_llm_stream")
			.startingState(state)
			.mapResult(response -> {
				String finalReport = Objects.requireNonNull(response.getResult().getOutput().getText());
				try {
					reportService.saveReport(threadId, finalReport);
					logger.info("报告已成功保存，线程ID: {}", threadId);
				}
				catch (Exception e) {
					logger.error("保存报告失败，线程ID: {}", threadId, e);
				}
				return Map.of("final_report", finalReport, "thread_id", threadId);
			})
			.build(streamResult);
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("final_report", generator);
		resultMap.put("thread_id", threadId);
		return resultMap;
	}

}
