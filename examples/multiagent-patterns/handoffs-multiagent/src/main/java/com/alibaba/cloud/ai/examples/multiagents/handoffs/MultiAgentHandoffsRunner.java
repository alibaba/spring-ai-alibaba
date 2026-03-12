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
package com.alibaba.cloud.ai.examples.multiagents.handoffs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the multi-agent handoffs demo when {@code handoffs.runner.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "handoffs.runner.enabled", havingValue = "true")
public class MultiAgentHandoffsRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MultiAgentHandoffsRunner.class);

	private final MultiAgentHandoffsService service;

	public MultiAgentHandoffsRunner(MultiAgentHandoffsService service) {
		this.service = service;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String query = "Hi, I'm having trouble with my account login. Can you help?";
		log.info("Query: {}", query);
		MultiAgentHandoffsService.MultiAgentHandoffsResult result = service.run(query);
		log.info("Response messages:");
		result.messages().forEach(msg -> log.info("  [{}] {}", msg.getMessageType(), msg.getText()));
	}
}
