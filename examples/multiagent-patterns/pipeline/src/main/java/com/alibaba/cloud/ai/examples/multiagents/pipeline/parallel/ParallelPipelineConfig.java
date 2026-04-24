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
package com.alibaba.cloud.ai.examples.multiagents.pipeline.parallel;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ParallelAgent example: Multi-topic research.
 *
 * <p>Business scenario: User provides a broad topic, the pipeline researches it from multiple angles in parallel:
 * <ul>
 *   <li>Technology perspective</li>
 *   <li>Finance / business perspective</li>
 *   <li>Market / industry perspective</li>
 * </ul>
 *
 * <p>Results are merged using DefaultMergeStrategy (Map of outputKey -> content).
 * Demonstrates ParallelAgent with concurrent sub-agents and merge strategies.
 */
@Configuration
public class ParallelPipelineConfig {

	private static final String TECH_RESEARCH_PROMPT = """
			You are a technology analyst. Research the given topic from a technology perspective.
			Provide a concise 2-3 paragraph analysis covering: key technologies, trends, and innovations.
			Focus on technical aspects only.
			""";

	private static final String FINANCE_RESEARCH_PROMPT = """
			You are a financial analyst. Research the given topic from a finance and business perspective.
			Provide a concise 2-3 paragraph analysis covering: market size, investment trends, business models.
			Focus on financial and business aspects only.
			""";

	private static final String MARKET_RESEARCH_PROMPT = """
			You are a market analyst. Research the given topic from an industry and market perspective.
			Provide a concise 2-3 paragraph analysis covering: competitive landscape, growth drivers, challenges.
			Focus on market and industry aspects only.
			""";

	@Bean("parallelResearchAgent")
	public ParallelAgent parallelResearchAgent(ChatModel chatModel) {
		ReactAgent techResearcher = ReactAgent.builder()
				.name("tech_researcher")
				.model(chatModel)
				.description("Researches from technology perspective")
				.systemPrompt(TECH_RESEARCH_PROMPT)
				.instruction("Research the following topic: {input}.")
				.outputKey("tech_analysis")
				.includeContents(false)
				.returnReasoningContents(false)
				.build();

		ReactAgent financeResearcher = ReactAgent.builder()
				.name("finance_researcher")
				.model(chatModel)
				.description("Researches from finance perspective")
				.systemPrompt(FINANCE_RESEARCH_PROMPT)
				.instruction("Research the following topic: {input}.")
				.outputKey("finance_analysis")
				.includeContents(false)
				.returnReasoningContents(false)
				.build();

		ReactAgent marketResearcher = ReactAgent.builder()
				.name("market_researcher")
				.model(chatModel)
				.description("Researches from market perspective")
				.systemPrompt(MARKET_RESEARCH_PROMPT)
				.instruction("Research the following topic: {input}.")
				.outputKey("market_analysis")
				.includeContents(false)
				.returnReasoningContents(false)
				.build();

		return ParallelAgent.builder()
				.name("parallel_research_agent")
				.description("Multi-topic research: analyzes a topic from tech, finance, and market angles in parallel")
				.subAgents(List.of(techResearcher, financeResearcher, marketResearcher))
				.mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
				.mergeOutputKey("research_report")
				.maxConcurrency(3)
				.build();
	}
}
