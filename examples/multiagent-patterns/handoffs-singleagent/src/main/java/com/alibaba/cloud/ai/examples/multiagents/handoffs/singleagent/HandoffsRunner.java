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
package com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Runs the customer support handoffs demo when {@code handoffs.run-examples=true}.
 * Executes four turns in sequence (same thread_id) to demonstrate warranty collection,
 * issue classification, and resolution.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "handoffs.run-examples", havingValue = "true")
public class HandoffsRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(HandoffsRunner.class);

	private static final String THREAD_ID = "handoffs-demo-thread";

	private final ReactAgent supportAgent;

	public HandoffsRunner(@Qualifier("supportAgent") ReactAgent supportAgent) {
		this.supportAgent = supportAgent;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		RunnableConfig config = RunnableConfig.builder().threadId(THREAD_ID).build();

		// Turn 1: Warranty collection
		log.info("=== Turn 1: Warranty Collection ===");
		AssistantMessage r1 = supportAgent.call(new UserMessage("Hi, my phone screen is cracked"), config);
		log.info("Assistant: {}", r1.getText());

		// Turn 2: User responds about warranty
		log.info("\n=== Turn 2: Warranty Response ===");
		AssistantMessage r2 = supportAgent.call(new UserMessage("Yes, it's still under warranty"), config);
		log.info("Assistant: {}", r2.getText());

		// Turn 3: User describes the issue
		log.info("\n=== Turn 3: Issue Description ===");
		AssistantMessage r3 = supportAgent.call(new UserMessage("The screen is physically cracked from dropping it"), config);
		log.info("Assistant: {}", r3.getText());

		// Turn 4: Resolution
		log.info("\n=== Turn 4: Resolution ===");
		AssistantMessage r4 = supportAgent.call(new UserMessage("What should I do?"), config);
		log.info("Assistant: {}", r4.getText());
	}
}
