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
package com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service that invokes the RAG workflow graph (rewrite → retrieve → agent).
 */
public class RagAgentService {

	private final CompiledGraph graph;

	public RagAgentService(CompiledGraph graph) {
		this.graph = graph;
	}

	/**
	 * Run the RAG pipeline with the given question.
	 */
	public RagAgentResult run(String question) throws GraphRunnerException {
		Map<String, Object> inputs = Map.of("question", question);
		Optional<OverAllState> resultOpt = graph.invoke(inputs);

		if (resultOpt.isEmpty()) {
			return new RagAgentResult(question, null, null);
		}

		OverAllState state = resultOpt.get();
		@SuppressWarnings("unchecked")
		AssistantMessage finalAnswer = (AssistantMessage) state.value("final_answer").orElse(new AssistantMessage(""));

		return new RagAgentResult(question, finalAnswer.getText(), state);
	}

	public record RagAgentResult(String question, String answer, OverAllState state) {
	}
}
