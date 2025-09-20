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

import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.enums.StreamResponseType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.prompt.PromptConstant;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.BUSINESS_KNOWLEDGE;
import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.IS_ONLY_NL2SQL;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_ERROR;
import static com.alibaba.cloud.ai.constant.Constant.SEMANTIC_MODEL;
import static com.alibaba.cloud.ai.constant.Constant.TABLE_RELATION_OUTPUT;

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
		String input = (String) state.value(INPUT_KEY).orElseThrow();
		Boolean onlyNl2sql = state.value(IS_ONLY_NL2SQL, false);

		// 检查是否为修复模式
		String validationError = StateUtils.getStringValue(state, PLAN_VALIDATION_ERROR, null);
		if (validationError != null) {
			logger.info("Regenerating plan with user feedback: {}", validationError);
		}
		else {
			logger.info("Generating initial plan");
		}

		// 构建提示参数
		String businessKnowledge = (String) state.value(BUSINESS_KNOWLEDGE).orElse("");
		String semanticModel = (String) state.value(SEMANTIC_MODEL).orElse("");
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);
		String schemaStr = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);

		// 构建用户提示
		String userPrompt = buildUserPrompt(input, validationError, state);

		// 构建模板参数
		Map<String, Object> params = Map.of("user_question", userPrompt, "schema", schemaStr, "business_knowledge",
				businessKnowledge, "semantic_model", semanticModel, "plan_validation_error",
				formatValidationError(validationError));

		// 生成计划
		String plannerPrompt = (onlyNl2sql ? PromptConstant.getPlannerNl2sqlOnlyTemplate()
				: PromptConstant.getPlannerPromptTemplate())
			.render(params);

		Flux<ChatResponse> chatResponseFlux = chatClient.prompt().user(plannerPrompt).stream().chatResponse();
		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> Map.of(PLANNER_NODE_OUTPUT, v), chatResponseFlux, StreamResponseType.PLAN_GENERATION);

		return Map.of(PLANNER_NODE_OUTPUT, generator);
	}

	private String buildUserPrompt(String input, String validationError, OverAllState state) {
		if (validationError == null) {
			return input;
		}

		String previousPlan = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT, "");
		return String.format(
				"IMPORTANT: User rejected previous plan with feedback: \"%s\"\n\n" + "Original question: %s\n\n"
						+ "Previous rejected plan:\n%s\n\n"
						+ "CRITICAL: Generate new plan incorporating user feedback (\"%s\")",
				validationError, input, previousPlan, validationError);
	}

	private String formatValidationError(String validationError) {
		return validationError != null ? String
			.format("**USER FEEDBACK (CRITICAL)**: %s\n\n**Must incorporate this feedback.**", validationError) : "";
	}

}
