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

import com.alibaba.cloud.ai.example.deepresearch.model.Plan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.ParameterizedTypeReference;

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

	private final ToolCallback[] toolCallbacks;

	private final BeanOutputConverter<Plan> converter;

	private final InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

	private final int MAX_MESSAGES = 100;

	private final MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
		.chatMemoryRepository(chatMemoryRepository)
		.maxMessages(MAX_MESSAGES)
		.build();

	public PlannerNode(ChatClient.Builder chatClientBuilder, ToolCallback[] toolCallbacks) {
		this.chatClient = chatClientBuilder
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build())
			.build();
		this.toolCallbacks = toolCallbacks;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Planner node is running.");
		Map<String, Object> updated = new HashMap<>();
		String nextStep = "reporter";

		Integer planIterations = state.value("plan_iterations", 0);
		Integer maxStepNum = state.value("max_step_num", 3);

		String result = state.value("llm_node_generator", "");
		logger.info("Planner response: {}", result);
		assert result != null;
		Plan curPlan = null;
		try {
			curPlan = converter.convert(result);
			logger.info("反序列成功，convert: {}", curPlan);
			if (curPlan.isHasEnoughContext()) {
				logger.info("Planner response has enough context.");
				updated.put("current_plan", curPlan);
				updated.put("messages", List.of(new AssistantMessage(result)));
				updated.put("planner_next_node", nextStep);
				return updated;
			}
		}
		catch (Exception e) {
			logger.error("反序列化失败");
			if (planIterations < maxStepNum) {
				// 尝试重新生成计划
				updated.put("plan_iterations", planIterations + 1);
				nextStep = "planner";
				updated.put("planner_next_node", nextStep);
				return updated;
			}
			else {
				nextStep = END;
				updated.put("planner_next_node", nextStep);
				return updated;
			}
		}

		nextStep = "human_feedback";
		updated.put("current_plan", curPlan);
		updated.put("messages", List.of(new AssistantMessage(result)));
		updated.put("planner_next_node", nextStep);

		return updated;
	}

}
