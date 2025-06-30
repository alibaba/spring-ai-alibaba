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

package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dispatcher.QueryRewriteDispatcher;
import com.alibaba.cloud.ai.dispatcher.SemanticConsistenceDispatcher;
import com.alibaba.cloud.ai.dispatcher.SqlGenerateDispatcher;
import com.alibaba.cloud.ai.dispatcher.SqlValidateDispatcher;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.node.*;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * @author zhangshenghang
 */
@Configuration
public class Nl2sqlConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Nl2sqlConfiguration.class);

	@Autowired
	@Qualifier("nl2SqlServiceImpl")
	private BaseNl2SqlService nl2SqlService;

	@Autowired
	@Qualifier("schemaServiceImpl")
	private BaseSchemaService schemaService;

	@Autowired
	private DbAccessor dbAccessor;

	@Autowired
	private DbConfig dbConfig;

	@Bean
	public StateGraph nl2sqlGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			// 用户输入
			keyStrategyHashMap.put(INPUT_KEY, new ReplaceStrategy());
			// queryWrite节点输出
			keyStrategyHashMap.put(QUERY_REWRITE_NODE_OUTPUT, new ReplaceStrategy());
			// keyword extract节点输出
			keyStrategyHashMap.put(KEYWORD_EXTRACT_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(EVIDENCES, new ReplaceStrategy());
			// schema recall节点输出
			keyStrategyHashMap.put(TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT, new ReplaceStrategy());
			// sql validate节点输出
			keyStrategyHashMap.put(SQL_VALIDATE_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(SQL_VALIDATE_EXCEPTION_OUTPUT, new ReplaceStrategy());
			// table relation节点输出
			keyStrategyHashMap.put(TABLE_RELATION_OUTPUT, new ReplaceStrategy());
			// sql generate节点输出
			keyStrategyHashMap.put(SQL_GENERATE_SCHEMA_MISSING_ADVICE, new ReplaceStrategy());
			keyStrategyHashMap.put(SQL_GENERATE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(SQL_GENERATE_COUNT, new ReplaceStrategy());
			// Semantic consistence节点输出
			keyStrategyHashMap.put(SEMANTIC_CONSISTENC_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(SEMANTIC_CONSISTENC_NODE_RECOMMEND_OUTPUT, new ReplaceStrategy());
			// 最终结果
			keyStrategyHashMap.put(RESULT, new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		StateGraph stateGraph = new StateGraph(NL2SQL_GRAPH_NAME, keyStrategyFactory)
			.addNode(QUERY_REWRITE_NODE, node_async(new QueryRewriteNode(chatClientBuilder, nl2SqlService)))
			.addNode(KEYWORD_EXTRACT_NODE, node_async(new KeywordExtractNode(chatClientBuilder, nl2SqlService)))
			.addNode(SCHEMA_RECALL_NODE, node_async(new SchemaRecallNode(chatClientBuilder, schemaService)))
			.addNode(TABLE_RELATION_NODE,
					node_async(new TableRelationNode(chatClientBuilder, schemaService, nl2SqlService)))
			.addNode(SQL_GENERATE_NODE, node_async(new SqlGenerateNode(chatClientBuilder, nl2SqlService, dbConfig)))
			.addNode(SQL_VALIDATE_NODE, node_async(new SqlValidateNode(chatClientBuilder, dbAccessor, dbConfig)))
			// TODO 待定：这里考虑可以添加一个自我反思的节点，进行自我反思和改进；是否需要根据使用效果再进行开发
			.addNode(SEMANTIC_CONSISTENC_NODE,
					node_async(new SemanticConsistencNode(chatClientBuilder, nl2SqlService, dbConfig)));
		// TODO 执行sql的节点

		stateGraph.addEdge(START, QUERY_REWRITE_NODE)
			.addConditionalEdges(QUERY_REWRITE_NODE, edge_async(new QueryRewriteDispatcher()),
					Map.of(KEYWORD_EXTRACT_NODE, KEYWORD_EXTRACT_NODE, END, END))
			.addEdge(KEYWORD_EXTRACT_NODE, SCHEMA_RECALL_NODE)
			.addEdge(SCHEMA_RECALL_NODE, TABLE_RELATION_NODE)
			.addEdge(TABLE_RELATION_NODE, SQL_GENERATE_NODE) // TODO 使用
																// addConditionalEdges
			.addConditionalEdges(SQL_GENERATE_NODE, edge_async(new SqlGenerateDispatcher()),
					Map.of(KEYWORD_EXTRACT_NODE, KEYWORD_EXTRACT_NODE, END, END, SQL_VALIDATE_NODE, SQL_VALIDATE_NODE))
			.addConditionalEdges(SQL_VALIDATE_NODE, edge_async(new SqlValidateDispatcher()),
					Map.of(SEMANTIC_CONSISTENC_NODE, SEMANTIC_CONSISTENC_NODE, SQL_GENERATE_NODE, SQL_GENERATE_NODE))
			.addConditionalEdges(SEMANTIC_CONSISTENC_NODE, edge_async(new SemanticConsistenceDispatcher()),
					Map.of(SQL_GENERATE_NODE, SQL_GENERATE_NODE, END, END));

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		logger.info("\n\n");
		logger.info(graphRepresentation.content());
		logger.info("\n\n");

		return stateGraph;
	}

}
