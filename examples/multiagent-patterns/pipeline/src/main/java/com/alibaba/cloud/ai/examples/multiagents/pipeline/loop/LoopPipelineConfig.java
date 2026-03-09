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
package com.alibaba.cloud.ai.examples.multiagents.pipeline.loop;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * LoopAgent example: SQL refinement until quality threshold.
 *
 * <p>Business scenario: Generate SQL from natural language and iteratively refine until the quality
 * score exceeds 0.5. Each iteration:
 * <ol>
 *   <li>SQL Generator: Produces SQL from user request</li>
 *   <li>SQL Rater: Scores the SQL (0-1)</li>
 * </ol>
 * Loop continues until score &gt; 0.5 or max iterations reached.
 *
 * <p>Demonstrates LoopAgent with ConditionLoopStrategy for iterative refinement.
 */
@Configuration
public class LoopPipelineConfig {

	private static final Logger log = LoggerFactory.getLogger(LoopPipelineConfig.class);

	private static final double QUALITY_THRESHOLD = 0.5;

	private static final String SQL_GENERATOR_PROMPT = """
			You are a MySQL database expert. Given the user's natural language request, output the corresponding SQL statement.
			Only output valid MySQL SQL. Do not include explanations.
			""";

	private static final String SQL_RATER_PROMPT = """
			You are a SQL quality reviewer. Given the user's natural language request and the generated SQL,
			output a single float score between 0 and 1. The score indicates how well the SQL matches the user intent.
			Output ONLY the number, no other text. Example: 0.85
			""";

	@Bean("loopSqlRefinementAgent")
	public LoopAgent loopSqlRefinementAgent(ChatModel chatModel) {
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

		SequentialAgent sqlAgent = SequentialAgent.builder()
				.name("sql_agent")
				.description("Generates SQL and scores its quality")
				.subAgents(List.of(sqlGenerateAgent, sqlRatingAgent))
				.build();

		return LoopAgent.builder()
				.name("loop_sql_refinement_agent")
				.description("Iteratively refines SQL until quality score exceeds " + QUALITY_THRESHOLD)
				.subAgent(sqlAgent)
				.loopStrategy(LoopMode.condition(messages -> {
					if (messages == null || messages.isEmpty()) {
						return false;
					}
					String text = messages.get(messages.size() - 1).getText();
					if (text == null || text.isBlank()) {
						return false;
					}
					try {
						double score = Double.parseDouble(text.trim());
						boolean satisfied = score > QUALITY_THRESHOLD;
						if (satisfied) {
							log.debug("SQL quality score {} exceeds threshold {}, stopping loop", score, QUALITY_THRESHOLD);
						}
						return satisfied;
					}
					catch (NumberFormatException e) {
						log.debug("Could not parse score from: {}", text);
						return false;
					}
				}))
				.build();
	}
}
