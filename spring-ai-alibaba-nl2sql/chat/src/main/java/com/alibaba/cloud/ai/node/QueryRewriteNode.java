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

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 问题重写与意图澄清，提升意图理解准确性。
 *
 * @author zhangshenghang
 */
public class QueryRewriteNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(QueryRewriteNode.class);

	private final ChatClient chatClient;

	private final BaseNl2SqlService baseNl2SqlService;

	public QueryRewriteNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.chatClient = chatClientBuilder.build();
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取用户输入
		String input = state.value(INPUT_KEY)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("Input key not found"));

		logger.info("[{}] 处理用户输入: {}", this.getClass().getSimpleName(), input);

		// 执行问题重写
		String rewrite = baseNl2SqlService.rewrite(input);
		logger.info("[{}] 问题重写结果: {}", this.getClass().getSimpleName(), rewrite);

		// 返回处理结果
		return Map.of(QUERY_REWRITE_NODE_OUTPUT, rewrite, RESULT, rewrite);
	}

}
