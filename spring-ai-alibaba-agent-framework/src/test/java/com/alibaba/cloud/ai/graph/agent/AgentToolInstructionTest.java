/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolInstructionTest {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldInjectInstructionOnce(boolean withParentConfig) {
		CapturingChatModel model = new CapturingChatModel();
		ReactAgent agent = ReactAgent.builder()
				.name("child_agent")
				.description("Answer a question")
				.model(model)
				.instruction("Answer concisely.")
				.build();

		String result = AgentTool.create(agent).call("{\"input\":\"hello\"}", toolContext(withParentConfig));

		assertThat(result).contains("child response");
		assertThat(model.messages).extracting(Message::getText).containsExactly("hello", "Answer concisely.");
		assertThat(model.messages).filteredOn(AgentInstructionMessage.class::isInstance).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldRenderInstructionWithoutLeavingRawTemplate(boolean withParentConfig) {
		CapturingChatModel model = new CapturingChatModel();
		ReactAgent agent = ReactAgent.builder()
				.name("child_agent")
				.description("Answer a question")
				.model(model)
				.instruction("Answer about {topic}.")
				.hooks(List.of(new TopicHook()))
				.build();

		AgentTool.create(agent).call("{\"input\":\"hello\"}", toolContext(withParentConfig));

		assertThat(model.messages).extracting(Message::getText).containsExactly("hello", "Answer about weather.");
		assertThat(model.messages).filteredOn(AgentInstructionMessage.class::isInstance).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldPreserveInputWithoutInstruction(boolean withParentConfig) {
		CapturingChatModel model = new CapturingChatModel();
		ReactAgent agent = ReactAgent.builder()
				.name("child_agent")
				.description("Answer a question")
				.model(model)
				.build();

		AgentTool.create(agent).call("{\"input\":\"hello\"}", toolContext(withParentConfig));

		assertThat(model.messages).extracting(Message::getText).containsExactly("hello");
	}

	private static ToolContext toolContext(boolean withParentConfig) {
		return new ToolContext(withParentConfig
				? Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY,
						RunnableConfig.builder().threadId("parent-thread").build())
				: Map.of("request_id", "test-request"));
	}

	private static class CapturingChatModel implements ChatModel {

		private List<Message> messages;

		@Override
		public ChatResponse call(Prompt prompt) {
			this.messages = List.copyOf(prompt.getInstructions());
			return new ChatResponse(List.of(new Generation(new AssistantMessage("child response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

	}

	@HookPositions(HookPosition.BEFORE_MODEL)
	private static class TopicHook extends ModelHook {

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			return CompletableFuture.completedFuture(Map.of("topic", "weather"));
		}

		@Override
		public String getName() {
			return "TopicHook";
		}

	}

}
