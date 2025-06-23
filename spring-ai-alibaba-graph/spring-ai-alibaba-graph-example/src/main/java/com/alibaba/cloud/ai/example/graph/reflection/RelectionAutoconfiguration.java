/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.graph.reflection;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReflectAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.agent.ReflectAgent.MESSAGES;

@Configuration
public class RelectionAutoconfiguration {

	public static class AssistantGraphNode implements NodeAction {

		private final LlmNode llmNode;

		private SystemPromptTemplate systemPromptTemplate;

		private final String NODE_ID = "call_model";

		private static final String CLASSIFIER_PROMPT_TEMPLATE = """
					You are an essay assistant tasked with writing excellent 5-paragraph essays.
				    Generate the best essay possible for the user's request.
				    If the user provides critique, respond with a revised version of your previous attempts.
				    Only return the main content I need, without adding any other interactive language.
				    Please answer in Chinese:
				""";

		public AssistantGraphNode(ChatClient chatClient) {
			this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
			this.llmNode = LlmNode.builder()
				.systemPromptTemplate(systemPromptTemplate.render())
				.chatClient(chatClient)
				.messagesKey("messages")
				.build();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private ChatClient chatClient;

			public Builder chatClient(ChatClient chatClient) {
				this.chatClient = chatClient;
				return this;
			}

			public AssistantGraphNode build() {
				if (chatClient == null) {
					throw new IllegalArgumentException("ChatClient must be provided");
				}
				return new AssistantGraphNode(chatClient);
			}

		}

		@Override
		public Map<String, Object> apply(OverAllState overAllState) throws Exception {

			List<Message> messages = (List<Message>) overAllState.value(MESSAGES).get();

			StateGraph stateGraph = new StateGraph(() -> {
				Map<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put(MESSAGES, new AppendStrategy());
				return strategies;
			}).addNode(this.NODE_ID, node_async(llmNode)).addEdge(START, this.NODE_ID).addEdge(this.NODE_ID, END);

			OverAllState invokeState = stateGraph.compile().invoke(Map.of(MESSAGES, messages)).get();
			List<Message> reactMessages = (List<Message>) invokeState.value(MESSAGES).orElseThrow();

			return Map.of(MESSAGES, reactMessages);

		}

	}

	public static class JudgeGraphNode implements NodeAction {

		private final LlmNode llmNode;

		private final String NODE_ID = "judge_response";

		private SystemPromptTemplate systemPromptTemplate;

		private static final String CLASSIFIER_PROMPT_TEMPLATE = """
					You are a teacher grading a student's essay submission. Provide detailed feedback and revision suggestions for the essay.

					Your feedback should cover the following aspects:

					- Length : Is the essay sufficiently developed? Does it meet the required length or need expansion/shortening?
					- Depth : Are the ideas well-developed? Is there sufficient analysis, evidence, or explanation?
					- Structure : Is the organization logical and clear? Are the introduction, transitions, and conclusion effective?
					- Style and Tone : Is the writing style appropriate for the purpose and audience? Is the tone consistent and professional?
					- Language Use : Are vocabulary, grammar, and sentence structure accurate and varied?
					- Focus only on providing actionable suggestions for improvement. Do not include grades, scores, or overall summary evaluations.

					Please respond in Chinese .
				""";

		public JudgeGraphNode(ChatClient chatClient) {
			this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
			this.llmNode = LlmNode.builder()
				.chatClient(chatClient)
				.systemPromptTemplate(systemPromptTemplate.render())
				.messagesKey(MESSAGES)
				.build();

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private ChatClient chatClient;

			public JudgeGraphNode.Builder chatClient(ChatClient chatClient) {
				this.chatClient = chatClient;
				return this;
			}

			public JudgeGraphNode build() {
				if (chatClient == null) {
					throw new IllegalArgumentException("ChatClient must be provided");
				}
				return new JudgeGraphNode(chatClient);
			}

		}

		@Override
		public Map<String, Object> apply(OverAllState allState) throws Exception {
			List<Message> messages = (List<Message>) allState.value(MESSAGES).get();

			StateGraph stateGraph = new StateGraph(() -> {
				Map<String, KeyStrategy> strategies = new HashMap<>();
				strategies.put(MESSAGES, new AppendStrategy());
				return strategies;
			}).addNode(this.NODE_ID, node_async(llmNode)).addEdge(START, this.NODE_ID).addEdge(this.NODE_ID, END);

			CompiledGraph compile = stateGraph.compile();

			OverAllState invokeState = compile.invoke(Map.of(MESSAGES, messages)).get();

			UnaryOperator<List<Message>> convertLastToUserMessage = messageList -> {
				int size = messageList.size();
				if (size == 0)
					return messageList;
				Message last = messageList.get(size - 1);
				messageList.set(size - 1, new UserMessage(last.getText()));
				return messageList;
			};

			List<Message> reactMessages = (List<Message>) invokeState.value(MESSAGES).orElseThrow();
			convertLastToUserMessage.apply(reactMessages);

			return Map.of(MESSAGES, reactMessages);

		}

	}

	@Bean
	public CompiledGraph reflectGraph(ChatModel chatModel) throws GraphStateException {

		ChatClient chatClient = ChatClient.builder(chatModel)
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.defaultOptions(OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
			.build();

		AssistantGraphNode assistantGraphNode = AssistantGraphNode.builder().chatClient(chatClient).build();
		JudgeGraphNode judgeGraphNode = JudgeGraphNode.builder().chatClient(chatClient).build();

		ReflectAgent reflectAgent = ReflectAgent.builder()
			.graph(assistantGraphNode)
			.reflection(judgeGraphNode)
			.maxIterations(2)
			.build();

		return reflectAgent.getAndCompileGraph();
	}

}
