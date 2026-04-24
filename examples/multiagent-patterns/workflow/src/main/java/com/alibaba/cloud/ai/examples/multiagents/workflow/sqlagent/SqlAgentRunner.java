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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Optional demo runner for the SQL agent. Set workflow.sql.enabled=true and
 * workflow.runner.enabled=true to run on startup.
 */
@Component
@ConditionalOnProperty(name = { "workflow.sql.enabled", "workflow.runner.enabled" }, havingValue = "true")
public class SqlAgentRunner {

	private static final Logger log = LoggerFactory.getLogger(SqlAgentRunner.class);

	private final SqlAgentService sqlAgentService;

	public SqlAgentRunner(SqlAgentService sqlAgentService) {
		this.sqlAgentService = sqlAgentService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		log.info("Running SQL agent demo...");
		try {
			SqlAgentService.SqlAgentResult result = sqlAgentService
					.run("Which genre on average has the longest tracks?");
			log.info("Question: {}", result.question());
			log.info("Answer: {}", result.answer());
		}
		catch (Exception e) {
			log.error("SQL agent demo failed", e);
		}
	}
}
