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
import com.alibaba.cloud.ai.example.deepresearch.util.TemplateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * @author yingzi
 * @date 2025/5/18 16:47
 */

public class PlannerNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	private final ChatClient chatClient;

	private final int MAX_PLAN_ITERATIONS = 3;

	private final BeanOutputConverter<Plan> converter;

	private final String PROMPT_FORMAT = """
			format: 以纯文本输出 json，请不要包含任何多余的文字——包括 markdown 格式;
			outputExample: {0};
			""";

	public PlannerNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Planner node is running.");
		List<Message> messages = TemplateUtil.applyPromptTemplate("planner", state);
		Integer planIterations = state.value("plan_iterations", 0);
		Boolean enableBackgroundInvestigation = state.value("enable_background_investigation", false);
		ArrayList<String> backgroundInvestigationResults = state.value("background_investigation_results",
				new ArrayList<>());

		if (planIterations == 0 && enableBackgroundInvestigation && !backgroundInvestigationResults.isEmpty()) {
			messages.add(SystemMessage.builder()
				.text("background investigation results of user query:\n" + backgroundInvestigationResults + "\n")
				.build());
		}
		String nextStep = "reporter";
		Map<String, Object> updated = new HashMap<>();
		if (planIterations > MAX_PLAN_ITERATIONS) {
			updated.put("planner_next_node", nextStep);
			return updated;
		}
		Flux<String> StreamResult = chatClient.prompt(MessageFormat.format(PROMPT_FORMAT, converter.getFormat()))
			.messages(messages)
			.stream()
			.content();

		String result = StreamResult.reduce((acc, next) -> acc + next).block();
		logger.info("Planner response: {}", result);
		assert result != null;
		Plan curPlan = null;
		try {
			curPlan = converter.convert(result);
			logger.info("反序列成功，convert: {}", curPlan);
			if (curPlan.isHasEnoughContext()) {
				logger.info("Planner response has enough context.");
				updated.put("current_plan", curPlan);
				updated.put("messages", new AssistantMessage(result));
				updated.put("planner_next_node", nextStep);
				return updated;
			}
		}
		catch (Exception e) {
			logger.error("反序列化失败");
			if (planIterations > 0) {
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
