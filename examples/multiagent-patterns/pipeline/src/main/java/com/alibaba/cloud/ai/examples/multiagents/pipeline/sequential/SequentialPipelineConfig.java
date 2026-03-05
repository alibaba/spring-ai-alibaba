/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multiagents.pipeline.sequential;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SequentialAgent example: Natural language to SQL generation pipeline.
 *
 * <p>Business scenario: User describes a query in natural language, the pipeline:
 * <ol>
 *   <li>SQL Generator: Converts natural language to MySQL SQL</li>
 *   <li>SQL Rater: Scores how well the SQL matches the user intent (0-1)</li>
 * </ol>
 *
 * <p>Demonstrates SequentialAgent with ordered sub-agents where each output feeds the next.
 */
@Configuration
public class SequentialPipelineConfig {

	private static final String SQL_GENERATOR_PROMPT = """
			You are a MySQL database expert. Given the user's natural language request, output the corresponding SQL statement.
			Only output valid MySQL SQL. Do not include explanations.
			""";

	private static final String SQL_RATER_PROMPT = """
			You are a SQL quality reviewer. Given the user's natural language request and the generated SQL,
			output a single float score between 0 and 1. The score indicates how well the SQL matches the user intent.
			Output ONLY the number, no other text. Example: 0.85
			""";

	@Bean("sequentialSqlAgent")
	public SequentialAgent sequentialSqlAgent(ChatModel chatModel) {
		ReactAgent sqlGenerateAgent = ReactAgent.builder()
				.name("sql_generator")
				.model(chatModel)
				.description("Converts natural language to MySQL SQL")
				.systemPrompt(SQL_GENERATOR_PROMPT)
				.outputKey("sql")
				.includeContents(false)
				.returnReasoningContents(false)
				.build();

		ReactAgent sqlRatingAgent = ReactAgent.builder()
				.name("sql_rater")
				.model(chatModel)
				.description("Scores SQL against user intent")
				.systemPrompt(SQL_RATER_PROMPT)
				.instruction("Here's the generated SQL:\n {sql}.\n\n Here's the original user request:\n {input}.")
				.outputKey("score")
				.includeContents(false)
				.returnReasoningContents(false)
				.build();

		return SequentialAgent.builder()
				.name("sequential_sql_agent")
				.description("Natural language to SQL pipeline: generates SQL and scores its quality")
				.subAgents(List.of(sqlGenerateAgent, sqlRatingAgent))
				.build();
	}
}
