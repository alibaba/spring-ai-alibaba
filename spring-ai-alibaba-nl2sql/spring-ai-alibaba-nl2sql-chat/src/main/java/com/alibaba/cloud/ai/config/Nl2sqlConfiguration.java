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

import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.dispatcher.PlanExecutorDispatcher;
import com.alibaba.cloud.ai.dispatcher.PythonExecutorDispatcher;
import com.alibaba.cloud.ai.dispatcher.QueryRewriteDispatcher;
import com.alibaba.cloud.ai.dispatcher.SQLExecutorDispatcher;
import com.alibaba.cloud.ai.dispatcher.SemanticConsistenceDispatcher;
import com.alibaba.cloud.ai.dispatcher.SqlGenerateDispatcher;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.node.KeywordExtractNode;
import com.alibaba.cloud.ai.node.PlanExecutorNode;
import com.alibaba.cloud.ai.node.PlannerNode;
import com.alibaba.cloud.ai.node.PythonAnalyzeNode;
import com.alibaba.cloud.ai.node.PythonExecuteNode;
import com.alibaba.cloud.ai.node.PythonGenerateNode;
import com.alibaba.cloud.ai.node.QueryRewriteNode;
import com.alibaba.cloud.ai.node.ReportGeneratorNode;
import com.alibaba.cloud.ai.node.SchemaRecallNode;
import com.alibaba.cloud.ai.node.SemanticConsistencyNode;
import com.alibaba.cloud.ai.node.SqlExecuteNode;
import com.alibaba.cloud.ai.node.SqlGenerateNode;
import com.alibaba.cloud.ai.node.TableRelationNode;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.business.BusinessKnowledgeRecallService;
import com.alibaba.cloud.ai.service.semantic.SemanticModelRecallService;
import com.alibaba.cloud.ai.service.code.CodePoolExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.AGENT_ID;
import static com.alibaba.cloud.ai.constant.Constant.BUSINESS_KNOWLEDGE;
import static com.alibaba.cloud.ai.constant.Constant.COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.DATA_SET_ID;
import static com.alibaba.cloud.ai.constant.Constant.EVIDENCES;
import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.KEYWORD_EXTRACT_NODE;
import static com.alibaba.cloud.ai.constant.Constant.KEYWORD_EXTRACT_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.NL2SQL_GRAPH_NAME;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PLANNER_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_CURRENT_STEP;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_EXECUTOR_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_NEXT_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_REPAIR_COUNT;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_ERROR;
import static com.alibaba.cloud.ai.constant.Constant.PLAN_VALIDATION_STATUS;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_ANALYSIS_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_ANALYZE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_EXECUTE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_EXECUTE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_GENERATE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_GENERATE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_IS_SUCCESS;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_TRIES_COUNT;
import static com.alibaba.cloud.ai.constant.Constant.QUERY_REWRITE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.QUERY_REWRITE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.REPORT_GENERATOR_NODE;
import static com.alibaba.cloud.ai.constant.Constant.RESULT;
import static com.alibaba.cloud.ai.constant.Constant.SCHEMA_RECALL_NODE;
import static com.alibaba.cloud.ai.constant.Constant.SEMANTIC_CONSISTENCY_NODE;
import static com.alibaba.cloud.ai.constant.Constant.SEMANTIC_CONSISTENCY_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SEMANTIC_MODEL;
import static com.alibaba.cloud.ai.constant.Constant.SQL_EXECUTE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.SQL_EXECUTE_NODE_EXCEPTION_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_EXECUTE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_GENERATE_COUNT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_GENERATE_NODE;
import static com.alibaba.cloud.ai.constant.Constant.SQL_GENERATE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_GENERATE_SCHEMA_MISSING_ADVICE;
import static com.alibaba.cloud.ai.constant.Constant.SQL_RESULT_LIST_MEMORY;
import static com.alibaba.cloud.ai.constant.Constant.SQL_VALIDATE_EXCEPTION_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_VALIDATE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.TABLE_RELATION_NODE;
import static com.alibaba.cloud.ai.constant.Constant.TABLE_RELATION_OUTPUT;
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
	@Qualifier("mysqlAccessor")
	private Accessor dbAccessor;

	@Autowired
	private DbConfig dbConfig;

	@Autowired
	private CodeExecutorProperties codeExecutorProperties;

	@Autowired
	private CodePoolExecutorService codePoolExecutor;

	@Autowired
	private SemanticModelRecallService semanticModelRecallService;

	@Autowired
	private BusinessKnowledgeRecallService businessKnowledgeRecallService;

	@Bean
	public StateGraph nl2sqlGraph(ChatClient.Builder chatClientBuilder) throws GraphStateException {

		KeyStrategyFactory keyStrategyFactory = () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			// 用户输入
			keyStrategyHashMap.put(INPUT_KEY, new ReplaceStrategy());
			// 数据集ID
			keyStrategyHashMap.put(DATA_SET_ID, new ReplaceStrategy());
			// 智能体ID
			keyStrategyHashMap.put(AGENT_ID, new ReplaceStrategy());
			// 业务知识
			keyStrategyHashMap.put(BUSINESS_KNOWLEDGE, new ReplaceStrategy());
			// 语义模型
			keyStrategyHashMap.put(SEMANTIC_MODEL, new ReplaceStrategy());
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
			// Python代码运行相关
			keyStrategyHashMap.put(SQL_RESULT_LIST_MEMORY, new ReplaceStrategy());
			keyStrategyHashMap.put(PYTHON_IS_SUCCESS, new ReplaceStrategy());
			keyStrategyHashMap.put(PYTHON_TRIES_COUNT, new ReplaceStrategy());
			keyStrategyHashMap.put(PYTHON_EXECUTE_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(PYTHON_GENERATE_NODE_OUTPUT, new ReplaceStrategy());
			keyStrategyHashMap.put(PYTHON_ANALYSIS_NODE_OUTPUT, new ReplaceStrategy());
			// 最终结果
			keyStrategyHashMap.put(RESULT, new ReplaceStrategy());
			return keyStrategyHashMap;
		};

		StateGraph stateGraph = new StateGraph(NL2SQL_GRAPH_NAME, keyStrategyFactory)
			.addNode(QUERY_REWRITE_NODE, node_async(new QueryRewriteNode(nl2SqlService)))
			.addNode(KEYWORD_EXTRACT_NODE, node_async(new KeywordExtractNode(nl2SqlService)))
			.addNode(SCHEMA_RECALL_NODE, node_async(new SchemaRecallNode(schemaService)))
			.addNode(TABLE_RELATION_NODE,
					node_async(new TableRelationNode(schemaService, nl2SqlService, businessKnowledgeRecallService,
							semanticModelRecallService)))
			.addNode(SQL_GENERATE_NODE, node_async(new SqlGenerateNode(chatClientBuilder, nl2SqlService)))
			.addNode(PLANNER_NODE, node_async(new PlannerNode(chatClientBuilder)))
			.addNode(PLAN_EXECUTOR_NODE, node_async(new PlanExecutorNode()))
			.addNode(SQL_EXECUTE_NODE, node_async(new SqlExecuteNode(dbAccessor, dbConfig)))
			.addNode(PYTHON_GENERATE_NODE,
					node_async(new PythonGenerateNode(codeExecutorProperties, chatClientBuilder)))
			.addNode(PYTHON_EXECUTE_NODE, node_async(new PythonExecuteNode(codePoolExecutor)))
			.addNode(PYTHON_ANALYZE_NODE, node_async(new PythonAnalyzeNode(chatClientBuilder)))
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
			// python nodes
			.addEdge(PYTHON_GENERATE_NODE, PYTHON_EXECUTE_NODE)
			.addConditionalEdges(PYTHON_EXECUTE_NODE, edge_async(new PythonExecutorDispatcher()),
					Map.of(PYTHON_ANALYZE_NODE, PYTHON_ANALYZE_NODE, END, END, PYTHON_GENERATE_NODE,
							PYTHON_GENERATE_NODE))
			.addEdge(PYTHON_ANALYZE_NODE, PLAN_EXECUTOR_NODE)
			// The dispatcher at PlanExecutorNode will decide the next step
			.addConditionalEdges(PLAN_EXECUTOR_NODE, edge_async(new PlanExecutorDispatcher()), Map.of(
					// If validation fails, go back to PlannerNode to repair
					PLANNER_NODE, PLANNER_NODE,
					// If validation passes, proceed to the correct execution node
					SQL_EXECUTE_NODE, SQL_EXECUTE_NODE, PYTHON_GENERATE_NODE, PYTHON_GENERATE_NODE,
					REPORT_GENERATOR_NODE, REPORT_GENERATOR_NODE,
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
