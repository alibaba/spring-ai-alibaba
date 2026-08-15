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
package com.alibaba.cloud.ai.examples.multiagents.agenticrag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Optional demo runner. Set {@code agentic-rag.runner.enabled=true} to run the three
 * demo scenarios on startup: a retrieval question, a conversational question that
 * skips retrieval, and a question that may require several retrieval rounds.
 */
@Component
@ConditionalOnProperty(name = "agentic-rag.runner.enabled", havingValue = "true")
public class AgenticRagRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AgenticRagRunner.class);

	private static final List<String> DEMO_QUESTIONS = List.of(
			"What is the refund policy for the annual plan?",
			"What is your name and what can you help me with?",
			"Compare the refund policy of the annual plan with the monthly plan.");

	private final AgenticRagService agenticRagService;

	public AgenticRagRunner(AgenticRagService agenticRagService) {
		this.agenticRagService = agenticRagService;
	}

	@Override
	public void run(ApplicationArguments args) {
		for (String question : DEMO_QUESTIONS) {
			try {
				AgenticRagService.AgenticRagResult result = agenticRagService.run(question);
				log.info("=== Question: {}", result.question());
				log.info("=== Retrieval rounds: {}", result.retrievalRounds());
				log.info("=== Answer:\n{}", result.answer());
			}
			catch (Exception e) {
				log.error("Demo question failed: {}", question, e);
			}
		}
	}
}
