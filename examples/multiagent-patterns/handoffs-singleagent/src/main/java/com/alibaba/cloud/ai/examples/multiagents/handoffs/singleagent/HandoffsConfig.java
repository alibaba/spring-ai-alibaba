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

import com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.support.HandoffsSupportHook;
import com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.tools.SupportTools;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the customer support handoffs agent: all workflow tools, the
 * step-config hook (model interceptor + key strategies), and a checkpointer
 * for state persistence across turns.
 */
@Configuration
public class HandoffsConfig {

	@Bean
	public MemorySaver memorySaver() {
		return new MemorySaver();
	}

	@Bean
	public ReactAgent supportAgent(ChatModel chatModel, MemorySaver memorySaver) {
		List<org.springframework.ai.tool.ToolCallback> allTools = List.of(
				SupportTools.recordWarrantyStatusTool(),
				SupportTools.recordIssueTypeTool(),
				SupportTools.provideSolutionTool(),
				SupportTools.escalateToHumanTool());

		return ReactAgent.builder()
				.name("support_agent")
				.model(chatModel)
				.tools(allTools)
				.hooks(new HandoffsSupportHook())
				.saver(memorySaver)
				.build();
	}
}
