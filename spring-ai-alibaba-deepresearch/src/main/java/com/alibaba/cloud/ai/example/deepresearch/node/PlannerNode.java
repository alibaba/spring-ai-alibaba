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

import com.alibaba.cloud.ai.example.deepresearch.model.enums.StreamNodePrefixEnum;
import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yingzi
 * @since 2025/5/18 16:47
 */
public class PlannerNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	private final ChatClient plannerAgent;

	private final BeanOutputConverter<Plan> converter;

	public PlannerNode(ChatClient plannerAgent) {
		this.plannerAgent = plannerAgent;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("planner node is running.");
		List<Message> messages = new ArrayList<>();
		// 1. 添加消息
		// 1.1 添加预置提示消息
		messages.add(TemplateUtil.getMessage("planner", state));
		// 1.2 添加用户提问
		messages.add(TemplateUtil.getOptQueryMessage(state));
		// 1.3 添加背景调查消息
		if (state.value("enable_deepresearch", true)) {
			List<String> backgroundInvestigationResults = state.value("background_investigation_results",
					(List<String>) null);
			assert backgroundInvestigationResults != null && !backgroundInvestigationResults.isEmpty();
			for (String backgroundInvestigationResult : backgroundInvestigationResults) {
				if (StringUtils.hasText(backgroundInvestigationResult)) {
					messages.add(new UserMessage(backgroundInvestigationResult));
				}
			}
		}
		// 1.4 添加用户反馈消息
		String feedbackContent = state.value("feedback_content", "").toString();
		if (StringUtils.hasText(feedbackContent)) {
			messages.add(new UserMessage(feedbackContent));
		}
		// 1.5 添加用户上传的RAG查询结果
		String ragContent = StateUtil.getRagContent(state);
		if (StringUtils.hasText(ragContent)) {
			messages.add(new UserMessage(ragContent));
		}

		logger.debug("messages: {}", messages);
		// 2. 规划任务
		String prefix = StreamNodePrefixEnum.PLANNER_LLM_STREAM.getPrefix();
		String stepTitleKey = prefix + "_step_title";
		state.registerKeyAndStrategy(stepTitleKey, new ReplaceStrategy());
		Map<String, Object> inputMap = new HashMap<>();
		inputMap.put(stepTitleKey, "[正在制定研究计划]");
		state.input(inputMap);

		var streamResult = plannerAgent.prompt(converter.getFormat()).messages(messages).stream().chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode(prefix)
			.startingState(state)
			.mapResult(response -> Map.of("planner_content",
					Objects.requireNonNull(response.getResult().getOutput().getText())))
			.buildWithChatResponse(streamResult);

		return Map.of("planner_content", generator);
	}

}
