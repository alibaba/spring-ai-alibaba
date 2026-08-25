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
package com.alibaba.cloud.ai.examples.multiagents.agenticrag.tools;

import com.alibaba.cloud.ai.examples.multiagents.agenticrag.AgenticRagService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Tool that exposes the agentic RAG graph as a single callable for the Studio entry
 * agent. The graph decides internally whether and how many times to retrieve.
 */
public class AgenticRagTools {

	private final AgenticRagService agenticRagService;

	public AgenticRagTools(AgenticRagService agenticRagService) {
		this.agenticRagService = agenticRagService;
	}

	@Tool(name = "answer_question", description = "Answers a question about the Acme Analytics product knowledge base (pricing, refunds, API rate limits, features, support) using agentic RAG.")
	public String answerQuestion(@ToolParam(description = "The question to answer") String question) {
		try {
			return agenticRagService.run(question).answer();
		}
		catch (Exception e) {
			return "Failed to answer: " + e.getMessage();
		}
	}
}
