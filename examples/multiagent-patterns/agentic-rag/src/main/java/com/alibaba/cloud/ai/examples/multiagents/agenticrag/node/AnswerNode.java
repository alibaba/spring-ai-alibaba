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
package com.alibaba.cloud.ai.examples.multiagents.agenticrag.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

/**
 * Generates the answer from the retrieved context (or directly from the model when the
 * router decided no retrieval was needed). Writes the result to the {@code final_answer}
 * state key.
 */
public class AnswerNode implements NodeAction {

	private final ChatModel chatModel;

	private final String promptTemplate;

	public AnswerNode(ChatModel chatModel, String promptTemplate) {
		this.chatModel = chatModel;
		this.promptTemplate = promptTemplate;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String question = state.value("question").map(Object::toString).orElse("");
		@SuppressWarnings("unchecked")
		List<String> docs = (List<String>) state.value("documents").orElse(List.of());
		String context = docs.isEmpty() ? "(no context retrieved)" : String.join("\n\n", docs);
		String prompt = promptTemplate.formatted(context, question);
		String answer = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
		return Map.of("final_answer", answer != null ? answer.trim() : "");
	}
}
