/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.constant;

/**
 * @author zhangshenghang
 */
public class Constant {

	public static final String INPUT_KEY = "input";

	public static final String AGENT_ID = "agentId";

	public static final String RESULT = "result";

	public static final String NL2SQL_GRAPH_NAME = "nl2sqlGraph";

	public static final String QUERY_REWRITE_NODE_OUTPUT = "QUERY_REWRITE_NODE_OUTPUT";

	public static final String KEYWORD_EXTRACT_NODE_OUTPUT = "KEYWORD_EXTRACT_NODE_OUTPUT";

	public static final String EVIDENCES = "EVIDENCES";

	public static final String TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT = "TABLE_DOCUMENTS_FOR_SCHEMA";

	public static final String SCHEMA_RECALL_NODE_OUTPUT = "SCHEMA_RECALL_NODE_OUTPUT";

	public static final String COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT = "COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT";

	public static final String TABLE_RELATION_OUTPUT = "TABLE_RELATION_OUTPUT";

	public static final String TABLE_RELATION_EXCEPTION_OUTPUT = "TABLE_RELATION_EXCEPTION_OUTPUT";

	public static final String TABLE_RELATION_RETRY_COUNT = "TABLE_RELATION_RETRY_COUNT";

	public static final String BUSINESS_KNOWLEDGE = "BUSINESS_KNOWLEDGE";

	public static final String SEMANTIC_MODEL = "SEMANTIC_MODEL";

	public static final String SQL_GENERATE_OUTPUT = "SQL_GENERATE_OUTPUT";

	public static final String SQL_GENERATE_SCHEMA_MISSING_ADVICE = "SQL_GENERATE_SCHEMA_MISSING_ADVICE";

	public static final String SQL_GENERATE_SCHEMA_MISSING = "SQL_GENERATE_SCHEMA_MISSING";

	public static final String SQL_GENERATE_COUNT = "SQL_GENERATE_COUNT";

	public static final String SQL_VALIDATE_NODE_OUTPUT = "SQL_VALIDATE_NODE_OUTPUT";

	public static final String SQL_VALIDATE_EXCEPTION_OUTPUT = "SQL_VALIDATE_EXCEPTION_OUTPUT";

	public static final String SEMANTIC_CONSISTENCY_NODE_OUTPUT = "SEMANTIC_CONSISTENCY_NODE_OUTPUT";

	public static final String SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT = "SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT";

	public static final String PLANNER_NODE_OUTPUT = "PLANNER_NODE_OUTPUT";

	public static final String SQL_EXECUTE_NODE_OUTPUT = "SQL_EXECUTE_NODE_OUTPUT";

	public static final String SQL_EXECUTE_NODE_EXCEPTION_OUTPUT = "SQL_EXECUTE_NODE_EXCEPTION_OUTPUT";

	// Plan当前需要执行的步骤编号
	public static final String PLAN_CURRENT_STEP = "PLAN_CURRENT_STEP";

	// Plan下一个需要进入的节点
	public static final String PLAN_NEXT_NODE = "PLAN_NEXT_NODE";

	// Plan validation
	public static final String PLAN_VALIDATION_STATUS = "PLAN_VALIDATION_STATUS";

	public static final String PLAN_VALIDATION_ERROR = "PLAN_VALIDATION_ERROR";

	public static final String PLAN_REPAIR_COUNT = "PLAN_REPAIR_COUNT";

	// Node KEY
	public static final String PLANNER_NODE = "PLANNER_NODE";

	public static final String PLAN_EXECUTOR_NODE = "PLAN_EXECUTOR_NODE";

	public static final String QUERY_REWRITE_NODE = "QUERY_REWRITE_NODE";

	public static final String REPORT_GENERATOR_NODE = "REPORT_GENERATOR_NODE";

	public static final String KEYWORD_EXTRACT_NODE = "KEYWORD_EXTRACT_NODE";

	public static final String SCHEMA_RECALL_NODE = "SCHEMA_RECALL_NODE";

	public static final String TABLE_RELATION_NODE = "TABLE_RELATION_NODE";

	public static final String SQL_GENERATE_NODE = "SQL_GENERATE_NODE";

	public static final String SQL_VALIDATE_NODE = "SQL_VALIDATE_NODE";

	public static final String SQL_EXECUTE_NODE = "SQL_EXECUTE_NODE";

	public static final String SEMANTIC_CONSISTENCY_NODE = "SEMANTIC_CONSISTENCY_NODE";

	public static final String SMALL_TALK_REJECT = "闲聊拒识";

	public static final String INTENT_UNCLEAR = "意图模糊需要澄清";

	// Keys related to Python code execution
	public static final String PYTHON_GENERATE_NODE = "PYTHON_GENERATE_NODE";

	public static final String PYTHON_EXECUTE_NODE = "PYTHON_EXECUTE_NODE";

	public static final String PYTHON_ANALYZE_NODE = "PYTHON_ANALYZE_NODE";

	public static final String SQL_RESULT_LIST_MEMORY = "SQL_RESULT_LIST_MEMORY";

	public static final String PYTHON_IS_SUCCESS = "PYTHON_IS_SUCCESS";

	public static final String PYTHON_TRIES_COUNT = "PYTHON_TRIES_COUNT";

	// If code execution succeeds, output code running result; if fails, output error
	// information
	public static final String PYTHON_EXECUTE_NODE_OUTPUT = "PYTHON_EXECUTE_NODE_OUTPUT";

	public static final String PYTHON_GENERATE_NODE_OUTPUT = "PYTHON_GENERATE_NODE_OUTPUT";

	public static final String PYTHON_ANALYSIS_NODE_OUTPUT = "PYTHON_ANALYSIS_NODE_OUTPUT";

	// nl2sql接口预留相关
	public static final String IS_ONLY_NL2SQL = "IS_ONLY_NL2SQL";

	public static final String ONLY_NL2SQL_OUTPUT = "ONLY_NL2SQL_OUTPUT";

	// 人类复核相关
	public static final String HUMAN_REVIEW_ENABLED = "HUMAN_REVIEW_ENABLED";

}
