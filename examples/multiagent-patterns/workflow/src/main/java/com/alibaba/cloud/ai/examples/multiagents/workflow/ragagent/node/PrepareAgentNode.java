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
package com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.node;

import com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.RagAgentConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

/**
 * Prepares the agent input by formatting context (retrieved documents) and question into a prompt.
 */
public class PrepareAgentNode implements NodeAction {

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String question = state.value("question").map(Object::toString).orElse("");
		@SuppressWarnings("unchecked")
		List<String> docs = (List<String>) state.value("documents").orElse(List.of());
		String context = String.join("\n\n", docs);
		String prompt = RagAgentConfig.buildAgentPrompt(context, question);
		return Map.of("messages", List.of(new UserMessage(prompt)));
	}
}
