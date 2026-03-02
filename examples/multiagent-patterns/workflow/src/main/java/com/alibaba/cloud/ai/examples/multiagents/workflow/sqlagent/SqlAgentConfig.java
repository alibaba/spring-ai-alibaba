/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent;

import com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.node.CallGetSchemaNode;
import com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.node.ListTablesNode;
import com.alibaba.cloud.ai.examples.multiagents.workflow.sqlagent.tools.SqlTools;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * SQL agent workflow using StateGraph.
 * <p>
 * Flow: START → list_tables → call_get_schema → get_schema → generate_query(ReactAgent) → END.
 * </p>
 * The generate_query phase uses ReactAgent to handle LLM↔run_query tool loop until final answer.
 * Equivalent to multiagents/code/sql-agent-workflow.md.
 */
@Configuration
@ConditionalOnProperty(name = "workflow.sql.enabled", havingValue = "true")
public class SqlAgentConfig {

	private static final String DIALECT = "H2";
	private static final int TOP_K = 5;

	private static final String GENERATE_QUERY_INSTRUCTION = """
			You are an agent designed to interact with a SQL database.
			Given an input question, create a syntactically correct %s query to run,
			then look at the results of the query and return the answer. Unless the user
			specifies a specific number of examples they wish to obtain, always limit your
			query to at most %d results.

			You can order the results by a relevant column to return the most interesting
			examples in the database. Never query for all the columns from a specific table,
			only ask for the relevant columns given the question.

			DO NOT make any DML statements (INSERT, UPDATE, DELETE, DROP etc.) to the database.
			""".formatted(DIALECT, TOP_K);

	@Bean
	public SqlTools sqlTools(JdbcTemplate jdbcTemplate) {
		return new SqlTools(jdbcTemplate);
	}

	@Bean
	public ReactAgent generateQueryAgent(ChatModel chatModel, SqlTools sqlTools) {
		List<ToolCallback> tools = List.of(sqlTools.runQueryTool());
		return ReactAgent.builder()
				.name("generate_query")
				.model(chatModel)
				.instruction(GENERATE_QUERY_INSTRUCTION)
				.tools(tools)
				.inputType(String.class)
				.build();
	}

	@Bean
	public CompiledGraph sqlGraph(ChatModel chatModel, SqlTools sqlTools, ReactAgent generateQueryAgent)
			throws GraphStateException {
		StateGraph graph = new StateGraph("sql_workflow", () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("messages", new AppendStrategy(false));
			strategies.put("llm_response", new ReplaceStrategy());
			strategies.put("question", new ReplaceStrategy());
			return strategies;
		});

		ListTablesNode listTablesNode = new ListTablesNode(sqlTools);
		CallGetSchemaNode callGetSchemaNode = new CallGetSchemaNode(chatModel, sqlTools);
		ToolNode getSchemaNode = ToolNode.builder()
				.toolCallbacks(List.of(sqlTools.getSchemaTool()))
				.toolCallbackResolver(sqlTools.resolver())
				.llmResponseKey("llm_response")
				.build();

		graph.addNode("list_tables", node_async(listTablesNode))
				.addNode("call_get_schema", node_async(callGetSchemaNode))
				.addNode("get_schema", node_async(getSchemaNode))
				.addNode("generate_query", generateQueryAgent.asNode(false, false))
				.addEdge(START, "list_tables")
				.addEdge("list_tables", "call_get_schema")
				.addEdge("call_get_schema", "get_schema")
				.addEdge("get_schema", "generate_query")
				.addEdge("generate_query", END);

		return graph.compile();
	}

	@Bean
	public SqlAgentService sqlAgentService(CompiledGraph sqlGraph) {
		return new SqlAgentService(sqlGraph);
	}
}
