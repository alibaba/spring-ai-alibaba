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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.prompt.PromptConstant;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * @author zhangshenghang
 */
public class PlannerNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	private final ChatClient chatClient;

	public PlannerNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());
		String input = (String) state.value(INPUT_KEY).orElseThrow();
		SchemaDTO schemaDTO = (SchemaDTO) state.value(TABLE_RELATION_OUTPUT).orElseThrow();
		String schemaStr = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);

		// Check if this is a repair attempt
		String validationError = StateUtils.getStringValue(state, PLAN_VALIDATION_ERROR, null);
		String userPrompt;
		if (validationError != null) {
			logger.warn("This is a plan repair attempt. Previous error: {}", validationError);
			String previousPlan = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT, "");
			userPrompt = String.format(
					"The previous plan you generated failed validation with the following error: %s\n\nHere is the faulty plan:\n%s\n\nPlease correct the plan and provide a new, valid one to answer the original question: %s",
					validationError, previousPlan, input);
		}
		else {
			userPrompt = input;
		}

		Map<String, Object> params = Map.of("user_question", userPrompt, "schema", schemaStr);
		String plannerPrompt = PromptConstant.getPlannerPromptTemplate().render(params);
		Flux<ChatResponse> chatResponseFlux = chatClient.prompt().user(plannerPrompt).stream().chatResponse();

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> Map.of(PLANNER_NODE_OUTPUT, v), chatResponseFlux, StreamResponseType.PLAN_GENERATION);

		return Map.of(PLANNER_NODE_OUTPUT, generator);
	}

}
