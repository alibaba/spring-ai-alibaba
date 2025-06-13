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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResearcherNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ResearcherNode.class);

	private final ChatClient researchAgent;

	public ResearcherNode(ChatClient researchAgent) {
		this.researchAgent = researchAgent;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("researcher node is running.");
		Plan currentPlan = StateUtil.getPlan(state);
		List<String> observations = StateUtil.getMessagesByType(state, "observations");
		Map<String, Object> updated = new HashMap<>();

		Plan.Step unexecutedStep = null;
		for (Plan.Step step : currentPlan.getSteps()) {
			if (Plan.StepType.RESEARCH.equals(step.getStepType()) && !StringUtils.hasText(step.getExecutionRes())) {
				unexecutedStep = step;
				break;
			}
		}
		if (unexecutedStep == null) {
			logger.info("all researcher node is finished.");
			return updated;
		}

		// 添加任务消息
		List<Message> messages = new ArrayList<>();
		Message taskMessage = new UserMessage(String.format("# Current Task\n\n##title\n\n%s\n\n##description\n\n%s",
				unexecutedStep.getTitle(), unexecutedStep.getDescription()));
		messages.add(taskMessage);

		// 添加研究者特有的引用提醒
		Message citationMessage = new UserMessage(
				"IMPORTANT: DO NOT include inline citations in the text. Instead, track all sources and include a References section at the end using link reference format. Include an empty line between each citation for better readability. Use this format for each reference:\n- [Source Title](URL)\n\n- [Another Source](URL)");
		messages.add(citationMessage);

		logger.debug("researcher Node messages: {}", messages);
		// 调用agent
		var streamResult = researchAgent.prompt().messages(messages).stream().chatResponse();
		var generator = StreamingChatGenerator.builder()
			.startingNode("researcher_llm_stream")
			.startingState(state)
			.mapResult(response -> Map.of("researcher_content",
					Objects.requireNonNull(response.getResult().getOutput().getText())))
			.build(streamResult);

		return Map.of("researcher_content", generator);
	}

}
