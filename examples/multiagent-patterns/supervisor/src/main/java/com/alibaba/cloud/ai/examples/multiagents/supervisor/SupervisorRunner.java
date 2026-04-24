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
package com.alibaba.cloud.ai.examples.multiagents.supervisor;

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
 * Runs the supervisor personal assistant demo when {@code supervisor.run-examples=true}.
 * Executes the same two scenarios as in the reference doc: single-domain (calendar only)
 * and multi-domain (calendar + email).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "supervisor.run-examples", havingValue = "true")
public class SupervisorRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SupervisorRunner.class);

	private final ReactAgent supervisorAgent;

	public SupervisorRunner(@Qualifier("supervisorAgent") ReactAgent supervisorAgent) {
		this.supervisorAgent = supervisorAgent;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Example 1: Simple single-domain request (calendar only)
		String query1 = "Schedule a team standup for tomorrow at 9am";
		log.info("User request: {}", query1);
		log.info("---");
		AssistantMessage response1 = supervisorAgent.call(new UserMessage(query1));
		log.info("Assistant: {}", response1.getText());
		log.info("");

		// Example 2: Complex multi-domain request (calendar + email)
		String query2 = "Schedule a meeting with the design team next Tuesday at 2pm for 1 hour, "
				+ "and send them an email reminder about reviewing the new mockups.";
		log.info("User request: {}", query2);
		log.info("---");
		AssistantMessage response2 = supervisorAgent.call(new UserMessage(query2));
		log.info("Assistant: {}", response2.getText());
	}
}
