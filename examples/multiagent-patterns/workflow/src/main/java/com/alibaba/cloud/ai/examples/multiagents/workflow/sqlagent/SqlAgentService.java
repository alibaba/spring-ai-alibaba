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

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service that invokes the SQL agent graph.
 */
public class SqlAgentService {

	private final CompiledGraph graph;

	public SqlAgentService(CompiledGraph graph) {
		this.graph = graph;
	}

	/**
	 * Run the SQL agent with the given question.
	 */
	public SqlAgentResult run(String question) throws GraphRunnerException {
		Map<String, Object> inputs = Map.of("messages", List.of(new UserMessage(question)), "question", question);
		Optional<OverAllState> resultOpt = graph.invoke(inputs);

		if (resultOpt.isEmpty()) {
			return new SqlAgentResult(question, null, null);
		}

		OverAllState state = resultOpt.get();
		@SuppressWarnings("unchecked")
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		String answer = messages.stream()
				.filter(m -> m instanceof AssistantMessage)
				.map(m -> m instanceof org.springframework.ai.chat.messages.AssistantMessage am ? am.getText() : "")
				.reduce((a, b) -> b)
				.orElse(null);

		return new SqlAgentResult(question, answer, state);
	}

	public record SqlAgentResult(String question, String answer, OverAllState state) {
	}
}
