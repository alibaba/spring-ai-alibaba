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
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Query rewriting and intent clarification node to improve intent understanding accuracy.
 *
 * This node is responsible for: - Rewriting user queries to clarify intent - Improving
 * understanding accuracy through query transformation - Providing streaming feedback
 * during rewriting process
 *
 * @author zhangshenghang
 */
public class QueryRewriteNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public QueryRewriteNode(BaseNl2SqlService baseNl2SqlService) {
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		String input = StateUtils.getStringValue(state, INPUT_KEY);
		logger.info("[{}] Processing user input: {}", this.getClass().getSimpleName(), input);

		// Use streaming utility class for content collection and result mapping
		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				"开始进行问题重写...", "问题重写完成！",
				finalResult -> Map.of(QUERY_REWRITE_NODE_OUTPUT, finalResult, RESULT, finalResult),
				baseNl2SqlService.rewriteStream(input), StreamResponseType.REWRITE);

		return Map.of(QUERY_REWRITE_NODE_OUTPUT, generator);
	}

}
