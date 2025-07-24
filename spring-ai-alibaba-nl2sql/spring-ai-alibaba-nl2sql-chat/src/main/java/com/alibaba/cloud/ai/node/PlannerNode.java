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

import com.alibaba.cloud.ai.util.PromptLoadUtils;
import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.service.PromptTemplateService;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Planner node that analyzes user requirements and generates execution plans.
 *
 * @author zhangshenghang
 */
public class PlannerNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PlannerNode.class);

	private final ChatClient chatClient;

	private final PromptTemplateService promptTemplateService;

	public PlannerNode(ChatClient.Builder chatClientBuilder, PromptTemplateService promptTemplateService) {
		this.chatClient = chatClientBuilder.build();
		this.promptTemplateService = promptTemplateService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());
		String input = (String) state.value(INPUT_KEY).orElseThrow();
		SchemaDTO schemaDTO = (SchemaDTO) state.value(TABLE_RELATION_OUTPUT).orElseThrow();
		String schemaStr = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);

		// Get dynamic template content - prioritize user-configured template
		String templateContent = getPlannerTemplate();

		// Build planner prompt using dynamic template
		String plannerPrompt = buildPlannerPromptWithTemplate(templateContent, input, schemaStr);

		Flux<ChatResponse> chatResponseFlux = chatClient.prompt().user(plannerPrompt).stream().chatResponse();

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> Map.of(PLANNER_NODE_OUTPUT, v), chatResponseFlux, StreamResponseType.PLAN_GENERATION);

		Map<String, Object> updated = Map.of(PLANNER_NODE_OUTPUT, generator);
		return updated;
	}

	/**
	 * Get planner template content, prioritizing user-configured template
	 */
	private String getPlannerTemplate() {
		// Try to get user-configured template first
		String userTemplate = promptTemplateService.getEnabledTemplateContent("planner");
		if (userTemplate != null && !userTemplate.trim().isEmpty()) {
			logger.info("Using user-configured planner template");
			// Validate the user template by trying to create a PromptTemplate instance
			try {
				Map<String, Object> testParams = new HashMap<>();
				testParams.put("user_question", "test");
				testParams.put("schema", "test");
				PromptTemplate testTemplate = new PromptTemplate(userTemplate);
				testTemplate.render(testParams); // This will throw if template is invalid
				return userTemplate;
			} catch (Exception e) {
				logger.warn("User-configured planner template is invalid, falling back to default template. Error: {}", e.getMessage());
			}
		}

		// Fallback to default template
		logger.info("Using default planner template");
		return PromptLoadUtils.getDefaultPlannerContent();
	}

	/**
	 * Build planner prompt using the provided template
	 */
	private String buildPlannerPromptWithTemplate(String templateContent, String userQuestion, String schema) {
		Map<String, Object> params = new HashMap<>();
		params.put("user_question", userQuestion);
		params.put("schema", schema);

		PromptTemplate template = new PromptTemplate(templateContent);
		return template.render(params);
	}

}
