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
import com.alibaba.cloud.ai.dispatcher.*;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.node.*;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
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

	@Autowired(required = false)
	private PythonExecutorTool pythonExecutorTool;

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
			keyStrategyHashMap.put(SEMANTIC_CONSISTENCY_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT, new ReplaceStrategy());
			// Planner 节点输出
			keyStrategyHashMap.put(PLANNER_NODE_OUTPUT, new ReplaceStrategy());
			// PlanExecutorNode
			keyStrategyHashMap.put(PLAN_CURRENT_STEP, new ReplaceStrategy());
			keyStrategyHashMap.put(PLAN_NEXT_NODE, new ReplaceStrategy());
			keyStrategyHashMap.put(PLAN_VALIDATION_STATUS, new ReplaceStrategy());
			keyStrategyHashMap.put(PLAN_VALIDATION_ERROR, new ReplaceStrategy());
			keyStrategyHashMap.put(PLAN_REPAIR_COUNT, new ReplaceStrategy());
			// SQL Execute 节点输出
			keyStrategyHashMap.put(SQL_EXECUTE_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, new ReplaceStrategy());
			// 最终结果
			keyStrategyHashMap.put(RESULT, new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		StateGraph stateGraph = new StateGraph(NL2SQL_GRAPH_NAME, keyStrategyFactory)
			.addNode(QUERY_REWRITE_NODE, node_async(new QueryRewriteNode(nl2SqlService)))
			.addNode(KEYWORD_EXTRACT_NODE, node_async(new KeywordExtractNode(nl2SqlService)))
			.addNode(SCHEMA_RECALL_NODE, node_async(new SchemaRecallNode(schemaService)))
			.addNode(TABLE_RELATION_NODE, node_async(new TableRelationNode(schemaService, nl2SqlService)))
			.addNode(SQL_GENERATE_NODE, node_async(new SqlGenerateNode(chatClientBuilder, nl2SqlService)))
			.addNode(PLANNER_NODE, node_async(new PlannerNode(chatClientBuilder)))
			.addNode(PLAN_EXECUTOR_NODE, node_async(new PlanExecutorNode()))
			.addNode(SQL_EXECUTE_NODE, node_async(new SqlExecuteNode(dbAccessor, dbConfig)))
			.addNode(PYTHON_EXECUTE_NODE, node_async(new PythonExecuteNode(chatClientBuilder)))
			.addNode(REPORT_GENERATOR_NODE, node_async(new ReportGeneratorNode(chatClientBuilder)))
			.addNode(SEMANTIC_CONSISTENCY_NODE, node_async(new SemanticConsistencyNode(nl2SqlService)));

		stateGraph.addEdge(START, QUERY_REWRITE_NODE)
			.addConditionalEdges(QUERY_REWRITE_NODE, edge_async(new QueryRewriteDispatcher()),
					Map.of(KEYWORD_EXTRACT_NODE, KEYWORD_EXTRACT_NODE, END, END))
			.addEdge(KEYWORD_EXTRACT_NODE, SCHEMA_RECALL_NODE)
			.addEdge(SCHEMA_RECALL_NODE, TABLE_RELATION_NODE)
			.addEdge(TABLE_RELATION_NODE, PLANNER_NODE)
			// The edge from PlannerNode now goes to PlanExecutorNode for validation and
			// execution
			.addEdge(PLANNER_NODE, PLAN_EXECUTOR_NODE)
			.addEdge(PYTHON_EXECUTE_NODE, PLAN_EXECUTOR_NODE)
			// The dispatcher at PlanExecutorNode will decide the next step
			.addConditionalEdges(PLAN_EXECUTOR_NODE, edge_async(new PlanExecutorDispatcher()), Map.of(
					// If validation fails, go back to PlannerNode to repair
					PLANNER_NODE, PLANNER_NODE,
					// If validation passes, proceed to the correct execution node
					SQL_EXECUTE_NODE, SQL_EXECUTE_NODE, PYTHON_EXECUTE_NODE, PYTHON_EXECUTE_NODE, REPORT_GENERATOR_NODE,
					REPORT_GENERATOR_NODE,
					// If max repair attempts are reached, end the process
					END, END))
			.addEdge(REPORT_GENERATOR_NODE, END)
			.addConditionalEdges(SQL_EXECUTE_NODE, edge_async(new SQLExecutorDispatcher()),
					Map.of(SQL_GENERATE_NODE, SQL_GENERATE_NODE, SEMANTIC_CONSISTENCY_NODE, SEMANTIC_CONSISTENCY_NODE))
			.addConditionalEdges(SQL_GENERATE_NODE, edge_async(new SqlGenerateDispatcher()),
					Map.of(KEYWORD_EXTRACT_NODE, KEYWORD_EXTRACT_NODE, END, END, SQL_EXECUTE_NODE, SQL_EXECUTE_NODE))
			.addConditionalEdges(SEMANTIC_CONSISTENCY_NODE, edge_async(new SemanticConsistenceDispatcher()),
					Map.of(SQL_GENERATE_NODE, SQL_GENERATE_NODE, PLAN_EXECUTOR_NODE, PLAN_EXECUTOR_NODE));

		GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
				"workflow graph");

		logger.info("\n\n");
		logger.info(graphRepresentation.content());
		logger.info("\n\n");

		return stateGraph;
	}

}
