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

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * @author yingzi
 * @since 2025/5/18 16:47
 */

public class PlannerNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	private final ChatClient chatClient;

	private final BeanOutputConverter<Plan> converter;

	public PlannerNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
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
		messages.add(new UserMessage(state.value("query", "")));
		// 1.3 添加背景调查消息
		String backgroundInvestigationResults = state.value("background_investigation_results", "");
		if (StringUtils.hasText(backgroundInvestigationResults)) {
			messages.add(new UserMessage(backgroundInvestigationResults));
		}
		// 1.4 添加用户反馈消息
		String feedBackContent = state.value("feed_back_content", "").toString();
		if (StringUtils.hasText(feedBackContent)) {
			messages.add(new UserMessage(feedBackContent));
		}
		// 1.5 添加观察的消息(Researcher、Coder返回的消息)
		for (String observation : StateUtil.getMessagesByType(state, "observations")) {
			messages.add(new UserMessage(observation));
		}

		logger.debug("messages: {}", messages);
		// 2. 规划任务
		Flux<String> StreamResult = chatClient.prompt(converter.getFormat()).messages(messages).stream().content();

		String result = StreamResult.reduce((acc, next) -> acc + next).block();
		logger.info("Planner response: {}", result);
		assert result != null;

		String nextStep = "reporter";
		Map<String, Object> updated = new HashMap<>();
		Plan curPlan = null;
		try {
			curPlan = converter.convert(result);
			logger.info("反序列成功，convert: {}", curPlan);
			// 2.1 反序列化成功，上下文充足，跳转reporter节点
			if (curPlan.isHasEnoughContext()) {
				logger.info("Planner response has enough context.");
				updated.put("current_plan", curPlan);
				updated.put("planner_next_node", nextStep);
				logger.info("planner node -> {} node", nextStep);
				return updated;
			}
		}
		catch (Exception e) {
			// 2.2 反序列化失败，尝试重新生成计划
			logger.error("反序列化失败");
			if (StateUtil.getPlanIterations(state) < StateUtil.getPlanMaxIterations(state)) {
				// 尝试重新生成计划
				updated.put("plan_iterations", StateUtil.getPlanIterations(state) + 1);
				nextStep = "planner";
				updated.put("planner_next_node", nextStep);
				logger.info("planner node -> {} node", nextStep);
				return updated;
			}
			else {
				nextStep = END;
				updated.put("planner_next_node", nextStep);
				logger.warn("planner node -> {} node", nextStep);
				return updated;
			}
		}
		// 2.3 上下文不足
		if (StateUtil.getAutoAcceptedPlan(state)) {
			// 自动接受，直接跳转research_team节点
			nextStep = "research_team";
		}
		else {
			// 需要人类反馈，跳转human_feedback节点
			nextStep = "human_feedback";
		}
		updated.put("current_plan", curPlan);
		updated.put("planner_next_node", nextStep);
		logger.info("planner node -> {} node", nextStep);
		return updated;
	}

}
