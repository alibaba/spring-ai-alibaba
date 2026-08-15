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
package com.alibaba.cloud.ai.examples.multiagents.agenticrag;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Invokes the agentic RAG graph (classify → [retrieve] → answer → check) for a question
 * and exposes the final answer together with how many retrieval rounds were performed.
 */
public class AgenticRagService {

	private static final Logger log = LoggerFactory.getLogger(AgenticRagService.class);

	private final CompiledGraph graph;

	public AgenticRagService(CompiledGraph graph) {
		this.graph = graph;
	}

	public AgenticRagResult run(String question) throws GraphRunnerException {
		Map<String, Object> inputs = Map.of("question", question);
		// Each run is an independent Q&A turn: give it a fresh thread id so the
		// checkpoint saver (a MemorySaver by default) does not replay state
		// (documents, retrieval counters) from a previous run.
		RunnableConfig config = RunnableConfig.builder()
			.threadId(UUID.randomUUID().toString())
			.build();
		Optional<OverAllState> resultOpt = graph.invoke(inputs, config);

		if (resultOpt.isEmpty()) {
			return new AgenticRagResult(question, "No result from graph.", 0);
		}

		OverAllState state = resultOpt.get();
		String answer = state.value("final_answer").map(Object::toString).orElse("");
		int retrievalRounds = state.value("retrieval_rounds")
				.map(Object::toString)
				.map(Integer::parseInt)
				.orElse(0);

		log.debug("Agentic RAG completed, retrieval rounds={}, answer length={}", retrievalRounds, answer.length());

		return new AgenticRagResult(question, answer, retrievalRounds);
	}

	public record AgenticRagResult(String question, String answer, int retrievalRounds) {
	}
}
