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
package io.agentscope.core;

import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReAct (Reasoning and Acting) Agent implementation.
 *
 * This agent provides a simple public API with call() and stream() methods, while
 * internally supporting Spring AI's MessageChatMemoryAdvisor through a ChatMemoryAdapter
 * bridge.
 *
 * Public API: - call(Msg) / call(List<Msg>): Synchronous message processing - stream(Msg)
 * / stream(List<Msg>): Asynchronous message processing
 *
 * Internal Features: - ChatMemoryAdapter bridges the simple Memory interface to Spring
 * AI's ChatMemory - Support for MessageChatMemoryAdvisor (accessible via package-private
 * methods) - Thread-safe memory operations
 */
public class ReActAgent {

	private final String name;

	private final String sysPrompt;

	private final Model model;

	private final Toolkit toolkit;

	private final Memory memory;

	private final int maxIters;

	private final ChatMemoryAdapter chatMemoryAdapter;

	private final ChatClient chatClient;

	public ReActAgent(String name, String sysPrompt, Model model, Toolkit toolkit, Memory memory, int maxIters) {
		this.name = name;
		this.sysPrompt = sysPrompt;
		this.model = model;
		this.toolkit = toolkit;
		this.memory = memory;
		this.maxIters = maxIters;
		this.chatMemoryAdapter = new ChatMemoryAdapter(memory);

		// Create ChatClient with MessageChatMemoryAdvisor and tool configuration
		MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemoryAdapter).build();
		ChatClient.Builder clientBuilder = ChatClient.builder(model.chatModel())
			.defaultSystem(sysPrompt)
			.defaultAdvisors(memoryAdvisor);

		// Configure tools if toolkit is provided
		if (toolkit != null) {
			clientBuilder.defaultToolCallbacks(toolkit.toolCallbackProvider().getToolCallbacks());
		}

		this.chatClient = clientBuilder.build();
	}

	public Flux<Msg> stream(Msg msg) {
		return stream(List.of(msg));
	}

	public Msg call(Msg msg) {
		return call(List.of(msg));
	}

	public Flux<Msg> stream(List<Msg> msgs) {
		return Flux.fromIterable(msgs).collectList().flatMapMany(this::processMessages).map(this::convertToMsg);
	}

	public Msg call(List<Msg> msgs) {
		// Use synchronous processing for call method
		List<Message> inputMessages = msgs.stream().map(this::convertToSpringMessage).toList();
		Message responseMessage = executeReActLoopSync(inputMessages);
		return convertToMsg(responseMessage);
	}

	private Flux<Message> processMessages(List<Msg> msgs) {
		// Convert input messages to Spring AI format and pass to executeReActLoop
		List<Message> inputMessages = msgs.stream().map(this::convertToSpringMessage).toList();

		return executeReActLoopStream(inputMessages);
	}

	/**
	 * Execute ReAct loop in streaming mode using ChatClient with
	 * MessageChatMemoryAdvisor.
	 */
	private Flux<Message> executeReActLoopStream(List<Message> inputMessages) {
		// Build user input from input messages
		String userInput = buildUserInput(inputMessages);

		// Use ChatClient with MessageChatMemoryAdvisor for streaming
		// parallelToolCalls is configured in the model's defaultOptions
		return chatClient.prompt()
			.user(userInput)
			.stream()
			.content()
			.map(content -> (Message) new AssistantMessage(content));
	}

	/**
	 * Execute ReAct loop in synchronous mode using ChatClient with
	 * MessageChatMemoryAdvisor.
	 */
	private Message executeReActLoopSync(List<Message> inputMessages) {
		// Build user input from input messages
		String userInput = buildUserInput(inputMessages);

		// Use ChatClient with MessageChatMemoryAdvisor
		// parallelToolCalls is configured in the model's defaultOptions
		String response = chatClient.prompt().user(userInput).call().content();
		return new AssistantMessage(response);
	}

	/**
	 * Build user input string from input messages.
	 */
	private String buildUserInput(List<Message> inputMessages) {
		StringBuilder userInput = new StringBuilder();
		for (Message message : inputMessages) {
			if (message instanceof UserMessage userMessage) {
				userInput.append(userMessage.getText());
			}
			else if (message instanceof AssistantMessage assistantMessage) {
				userInput.append(assistantMessage.getText());
			}
			else {
				userInput.append(message.getText());
			}
			userInput.append("\n");
		}
		return userInput.toString().trim();
	}

	private Message convertToSpringMessage(Msg msg) {
		String content = extractTextContent(msg);

		switch (msg.getRole()) {
			case USER:
				return new UserMessage(content);
			case ASSISTANT:
				return new AssistantMessage(content);
			case SYSTEM:
				return new SystemMessage(content);
			default:
				return new UserMessage(content);
		}
	}

	private String extractTextContent(Msg msg) {
		if (msg.getContent() instanceof TextBlock textBlock) {
			return textBlock.getText();
		}
		return msg.getContent() != null ? msg.getContent().toString() : "";
	}

	private Msg convertToMsg(Message springMessage) {
		MsgRole role;
		if (springMessage instanceof UserMessage) {
			role = MsgRole.USER;
		}
		else if (springMessage instanceof AssistantMessage) {
			role = MsgRole.ASSISTANT;
		}
		else if (springMessage instanceof SystemMessage) {
			role = MsgRole.SYSTEM;
		}
		else {
			role = MsgRole.ASSISTANT;
		}

		String messageContent = "";
		if (springMessage instanceof UserMessage userMessage) {
			messageContent = userMessage.getText();
		}
		else if (springMessage instanceof AssistantMessage assistantMessage) {
			messageContent = assistantMessage.getText();
		}
		else if (springMessage instanceof SystemMessage systemMessage) {
			messageContent = systemMessage.getText();
		}

		TextBlock content = TextBlock.builder().text(messageContent).build();

		return Msg.builder().name(name).role(role).content(content).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name;

		private String sysPrompt;

		private Model model;

		private Toolkit toolkit;

		private Memory memory;

		private int maxIters;

		private Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder sysPrompt(String sysPrompt) {
			this.sysPrompt = sysPrompt;
			return this;
		}

		public Builder model(Model model) {
			this.model = model;
			return this;
		}

		public Builder toolkit(Toolkit toolkit) {
			this.toolkit = toolkit;
			return this;
		}

		public Builder memory(Memory memory) {
			this.memory = memory;
			return this;
		}

		public Builder maxIters(int maxIters) {
			this.maxIters = maxIters;
			return this;
		}

		public ReActAgent build() {
			return new ReActAgent(name, sysPrompt, model, toolkit, memory, maxIters);
		}

	}

	/**
	 * Internal adapter class that bridges between the original Memory interface and
	 * Spring AI's ChatMemory interface. This allows ReActAgent to work with
	 * MessageChatMemoryAdvisor while keeping the original Memory interface simple for
	 * users.
	 */
	private static class ChatMemoryAdapter implements ChatMemory {

		private final Memory memory;

		private final ConcurrentHashMap<String, List<Message>> conversationMessages = new ConcurrentHashMap<>();

		public ChatMemoryAdapter(Memory memory) {
			Assert.notNull(memory, "memory cannot be null");
			this.memory = memory;
		}

		@Override
		public void add(String conversationId, Message message) {
			Assert.notNull(message, "message cannot be null");
			// Always use the original Memory interface, ignore conversationId
			memory.addMessage(message);
		}

		@Override
		public void add(String conversationId, List<Message> messages) {
			Assert.notNull(messages, "messages cannot be null");
			// Always use the original Memory interface, ignore conversationId
			for (Message message : messages) {
				memory.addMessage(message);
			}
		}

		@Override
		public List<Message> get(String conversationId) {
			// Always use the original Memory interface, ignore conversationId
			return memory.getMessages();
		}

		@Override
		public void clear(String conversationId) {
			// Always ignore conversationId, we can't clear
			// the original Memory
			// as it doesn't have a clear method. This is a limitation.
			// Users would need to create a new Memory instance if they want to clear.
		}

	}

}
