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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 关键词、实体、时间等信息抽取，为后续 Schema 召回做准备
 *
 * @author zhangshenghang
 */
public class KeywordExtractNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(KeywordExtractNode.class);

	private final ChatClient chatClient;

	private final BaseNl2SqlService baseNl2SqlService;

	public KeywordExtractNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.chatClient = chatClientBuilder.build();
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());
		String input = (String) state.value(INPUT_KEY).orElseThrow();

		List<String> evidences = baseNl2SqlService.extractEvidences(input);
		List<String> keywords = baseNl2SqlService.extractKeywords(input, evidences);
		logger.info("evidences：{} , keywords: {}", evidences, keywords);

		state.value(SQL_GENERATE_SCHEMA_MISSING_ADVICE).map(advice -> {
			logger.info("Schema 召回缺失补充");
			List<String> additionalKeywords = baseNl2SqlService.extractKeywords((String) advice, evidences);
			logger.info("Schema 召回缺失补充 keywords: {}", additionalKeywords);
			keywords.addAll(additionalKeywords);
			return keywords;
		});

		Map<String, Object> updated = Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, keywords, EVIDENCES, evidences, RESULT,
				keywords);

		logger.info("{} 节点输出 evidences：{} , keywords: {}", this.getClass().getSimpleName(), evidences, keywords);
		return updated;
	}

}
