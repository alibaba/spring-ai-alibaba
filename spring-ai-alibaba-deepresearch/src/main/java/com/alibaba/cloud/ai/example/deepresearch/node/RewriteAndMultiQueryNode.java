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

import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * @author yingzi
 * @since 2025/5/18 16:54
 */

public class RewriteAndMultiQueryNode implements NodeAction {

	private static final Integer MaxOptimizeQueryNum = 5;

	private static final Integer MinOptimizeQueryNum = 0;

	private static final Logger logger = LoggerFactory.getLogger(RewriteAndMultiQueryNode.class);

	private final QueryTransformer queryTransformer;

	ChatClient.Builder rewriteAndMultiQueryAgentBuilder;

	public RewriteAndMultiQueryNode(ChatClient.Builder rewriteAndMultiQueryAgentBuilder) {
		this.rewriteAndMultiQueryAgentBuilder = rewriteAndMultiQueryAgentBuilder;

		// 查询重写
		this.queryTransformer = RewriteQueryTransformer.builder()
			.chatClientBuilder(rewriteAndMultiQueryAgentBuilder)
			.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("rewrite_multiquery node is running.");
		Map<String, Object> updated = new HashMap<>();
		String nextStep = END;

		String queryText = StateUtil.getQuery(state);
		assert queryText != null;
		Query query = Query.builder().text(queryText).build();
		// 查询重写
		Query rewriteQuery = queryTransformer.transform(query);

		// 查询拓展
		int optimizeQueryNum = StateUtil.getOptimizeQueryNum(state);
		optimizeQueryNum = Math.max(MinOptimizeQueryNum, Math.min(MaxOptimizeQueryNum, optimizeQueryNum));
		QueryExpander queryExpander = MultiQueryExpander.builder()
			.chatClientBuilder(rewriteAndMultiQueryAgentBuilder)
			.includeOriginal(true)
			.numberOfQueries(optimizeQueryNum)
			.build();

		List<Query> multiQueries = queryExpander.expand(rewriteQuery);
		List<String> newQueries = multiQueries.stream().map(Query::text).collect(Collectors.toList());
		updated.put("optimize_queries", newQueries);
		// 判断是否需要用户上传
		if (state.value("user_upload_file", false)) {
			nextStep = "user_file_rag";
		}
		else {
			nextStep = "background_investigator";
		}
		updated.put("rewrite_multi_query_next_node", nextStep);
		return updated;
	}

}
