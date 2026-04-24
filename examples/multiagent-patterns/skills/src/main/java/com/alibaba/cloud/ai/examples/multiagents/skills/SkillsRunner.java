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
package com.alibaba.cloud.ai.examples.multiagents.skills;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs a short demo of the skills (progressive disclosure) agent when
 * {@code skills.runner.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "skills.runner.enabled", havingValue = "true")
public class SkillsRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SkillsRunner.class);

	private final ReactAgent sqlAssistantAgent;

	public SkillsRunner(ReactAgent sqlAssistantAgent) {
		this.sqlAssistantAgent = sqlAssistantAgent;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String query = "Write a SQL query to find all customers who made orders over $1000 in the last month";
		log.info("User: {}", query);
		AssistantMessage response = sqlAssistantAgent.call(query);
		log.info("Assistant: {}", response.getText());
	}
}
