/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.graph.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class LLmNode implements NodeAction {

	@Autowired
	private ChatModel chatModel;

	@Autowired
	private ChatClient.Builder builder;

	static String promptTemplate = """
			You are a professional AI assistant. Please generate responses based on the given content and questions.

			Content context:
			%s

			Questions to answer (respond in the question's original language, keep answers concise):
			%s

			Please carefully review the content and adhere to these requirements:
			1. Ensure responses match the question's language
			2. Break down complex questions into bullet points
			3. Avoid speculative or unverified information
			4. Prioritize accuracy over completeness""";

	@Override
	public Map<String, Object> apply(OverAllState t) {
		// Create prompt with user message
		List<String> parallelResult = t.value("parallel_result", List.class).get();
		String question = t.value("input", String.class).get();

		StringBuilder formattedContent = new StringBuilder();
		if (CollectionUtils.isNotEmpty(parallelResult)) {
			for (int i = 0; i < parallelResult.size(); i++) {
				formattedContent.append((i + 1) + ". " + parallelResult.get(i) + "\n");
			}
		}

		UserMessage message = new UserMessage(promptTemplate.formatted(formattedContent, question));
		ChatClient chatClient = builder.build();

		var flux = chatClient.prompt().messages(message).stream().chatResponse();

		var generator = StreamingChatGenerator.builder()
			.startingNode("llmNode")
			.startingState(t)
			.mapResult(
					response -> Map.of("messages", Objects.requireNonNull(response.getResult().getOutput().getText())))
			.build(flux);

		return Map.of("messages1", generator, "messages", "test");
	}

}
