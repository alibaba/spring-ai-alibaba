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
package com.alibaba.cloud.ai.examples.multiagents.qa;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs the QA assistant demo when {@code qa-assistant.run-examples=true}.
 * Executes three scenarios: knowledge-base-only, web-search-only, and hybrid (both).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "qa-assistant.run-examples", havingValue = "true")
public class QaAssistantRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(QaAssistantRunner.class);

	private final ReactAgent qaSupervisorAgent;

	public QaAssistantRunner(@Qualifier("qaSupervisorAgent") ReactAgent qaSupervisorAgent) {
		this.qaSupervisorAgent = qaSupervisorAgent;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Example 1: Knowledge base only (company-specific question)
		String query1 = "What is our company's remote work policy?";
		log.info("User request: {}", query1);
		log.info("---");
		AssistantMessage response1 = qaSupervisorAgent.call(new UserMessage(query1));
		log.info("Assistant: {}", response1.getText());
		log.info("");

		// Example 2: Web search only (general knowledge question)
		String query2 = "What are the latest developments in AI agent frameworks in 2026?";
		log.info("User request: {}", query2);
		log.info("---");
		AssistantMessage response2 = qaSupervisorAgent.call(new UserMessage(query2));
		log.info("Assistant: {}", response2.getText());
		log.info("");

		// Example 3: Hybrid question (knowledge base + web search)
		String query3 = "How does our product compare to competitors in the market?";
		log.info("User request: {}", query3);
		log.info("---");
		AssistantMessage response3 = qaSupervisorAgent.call(new UserMessage(query3));
		log.info("Assistant: {}", response3.getText());
	}
}
