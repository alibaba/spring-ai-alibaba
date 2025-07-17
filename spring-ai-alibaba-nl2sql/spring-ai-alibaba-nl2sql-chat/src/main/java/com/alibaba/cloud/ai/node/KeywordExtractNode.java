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
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Keyword, entity, and temporal information extraction node to prepare for subsequent
 * schema recall.
 *
 * This node is responsible for: - Extracting evidences from user input - Extracting
 * keywords based on evidences - Preparing structured information for schema recall -
 * Providing streaming feedback during extraction process
 *
 * @author zhangshenghang
 */
public class KeywordExtractNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(KeywordExtractNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public KeywordExtractNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		String input = StateUtils.getStringValue(state, QUERY_REWRITE_NODE_OUTPUT,
				StateUtils.getStringValue(state, INPUT_KEY));

		// Execute business logic first - extract evidences and keywords immediately
		List<String> evidences = baseNl2SqlService.extractEvidences(input);
		List<String> keywords = baseNl2SqlService.extractKeywords(input, evidences);

		logger.info("[{}] Extraction results - evidences: {}, keywords: {}", this.getClass().getSimpleName(), evidences,
				keywords);

		Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始提取关键词..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取证据..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("提取的证据: " + String.join(",", evidences)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取关键词..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("提取的关键词: " + String.join(",", keywords)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("关键词提取完成."));
			emitter.complete();
		});

		// Use business logic executor to avoid duplicate business logic execution
		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, keywords, EVIDENCES, evidences, RESULT, keywords), displayFlux,
				StreamResponseType.KEYWORD_EXTRACT);

		return Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, generator);
	}

}
