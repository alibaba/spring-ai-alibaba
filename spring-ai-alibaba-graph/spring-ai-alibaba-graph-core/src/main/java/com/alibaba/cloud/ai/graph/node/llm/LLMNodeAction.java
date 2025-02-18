/*
 * Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.graph.node.llm;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.AbstractNode;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 北极星 TODO add chat memory
 */
public class LLMNodeAction extends AbstractNode implements NodeAction {

	public static final String DEFAULT_OUTPUT_KEY = "text";

	private ChatClient chatClient;

	private List<PromptTemplate> promptTemplates;

	private NodeActionDescriptor nodeActionDescriptor;

	private LLMNodeAction(ChatClient chatClient, List<PromptTemplate> promptTemplates,
			NodeActionDescriptor nodeActionDescriptor) {
		this.chatClient = chatClient;
		this.promptTemplates = promptTemplates;
		this.nodeActionDescriptor = nodeActionDescriptor;
	}

	public static Builder builder(ChatModel chatModel) {

		return new Builder(chatModel);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> partialState = reduceState(state);
		List<Message> messages = renderPromptTemplates(partialState, promptTemplates);
		List<Generation> generations = chatClient.prompt().messages(messages).call().chatResponse().getResults();
		List<Message> result = generations.stream().map(Generation::getOutput).collect(Collectors.toList());
		return formattedOutput(result);
	}

	/**
	 * reduce the global state
	 * @param state global state
	 * @return partial state
	 */
	private Map<String, Object> reduceState(OverAllState state) {
		if (nodeActionDescriptor.getInputSchema().isEmpty()) {
			return state.data();
		}
		return nodeActionDescriptor.getInputSchema()
			.stream()
			.collect(Collectors.toMap(inputKey -> inputKey, inputKey -> state.value(inputKey,  "")));
	}

	/**
	 * render the variable in the messages
	 * @param state variable's key and value
	 */
	private List<Message> renderPromptTemplates(Map<String, Object> state, List<PromptTemplate> promptTemplates) {
		return promptTemplates.stream().map(tmpl -> {
			state.forEach(tmpl::add);
			return tmpl.createMessage();
		}).toList();
	}

	/**
	 * format the llm output
	 * @param messages llm output messages
	 * @return map
	 */
	private Map<String, Object> formattedOutput(List<Message> messages) {
		String outputKey;
		if (!nodeActionDescriptor.getOutputSchema().isEmpty()) {
			// only the first output key is accepted
			outputKey = nodeActionDescriptor.getOutputSchema().get(0);
		}
		else {
			outputKey = DEFAULT_OUTPUT_KEY;
		}
		// concat all the message content into a string
		String outputValue = messages.stream().map(Message::getContent).reduce("", (a, b) -> a + b + "\n");
		return Map.of(outputKey, outputValue);
	}

	@Override
	public NodeActionDescriptor getNodeActionDescriptor() {
		return nodeActionDescriptor;
	}

	public static class Builder {

		private ChatModel chatModel;

		private List<PromptTemplate> promptTemplates;

		private String[] functions;

		private LLMNodeActionDescriptor nodeActionDescriptor;

		public Builder(ChatModel chatModel) {
			this.chatModel = chatModel;
			this.nodeActionDescriptor = new LLMNodeActionDescriptor();
			this.nodeActionDescriptor.setChatOptions(chatModel.getDefaultOptions());
		}

		public LLMNodeAction build() {
			ChatClient.Builder builder = ChatClient.builder(this.chatModel);
			if (functions != null && functions.length > 0)
				builder.defaultFunctions(functions);
			return new LLMNodeAction(builder.build(), promptTemplates, nodeActionDescriptor);
		}

		public Builder withPromptTemplates(List<PromptTemplate> promptTemplates) {
			this.promptTemplates = promptTemplates;
			this.nodeActionDescriptor.setPromptTemplates(promptTemplates);
			return this;
		}

		public Builder withFunctions(String... functionNames) {
			if (functionNames == null || functionNames.length == 0)
				return this;
			this.functions = functionNames;
			this.nodeActionDescriptor.setFunctionNames(List.of(functions));
			return this;
		}

		public Builder withFunctions(FunctionCallback... functionCallbacks) {
			if (functionCallbacks == null || functionCallbacks.length == 0)
				return this;
			this.nodeActionDescriptor
				.setFunctionNames(Arrays.stream(functionCallbacks).map(FunctionCallback::getName).toList());
			return this;
		}

		public Builder withInputKey(String... inputKey) {
			this.nodeActionDescriptor.addInputKey(inputKey);
			return this;
		}

		public Builder withOutputKey(String... outputKey) {
			this.nodeActionDescriptor.addOutputKey(outputKey);
			return this;
		}

	}

}
