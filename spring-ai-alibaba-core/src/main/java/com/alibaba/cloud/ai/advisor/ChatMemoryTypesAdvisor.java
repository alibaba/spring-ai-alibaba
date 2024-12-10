package com.alibaba.cloud.ai.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.cloud.ai.memory.types.ChatMemoryType;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;

/**
 * Message 消息类型的 Advisor
 * 依赖于 MessageChatMemoryType
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class ChatMemoryTypesAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

	private ChatMemoryType chatMemoryTypes;

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, ChatMemoryType chatMemoryTypes) {
		super(chatMemory);
		this.chatMemoryTypes = chatMemoryTypes;
	}

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, ChatMemoryType chatMemoryTypes, String defaultConversationId, int chatHistoryWindowSize) {
		this(chatMemory, chatMemoryTypes, defaultConversationId, chatHistoryWindowSize, Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER);
	}

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, ChatMemoryType chatMemoryTypes, String defaultConversationId, int chatHistoryWindowSize,
			int order) {
		super(chatMemory, defaultConversationId, chatHistoryWindowSize, true, order);
		this.chatMemoryTypes = chatMemoryTypes;
	}

	@Override
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

		String name = chatMemoryTypes.getName();

		if (Objects.equals(name, "message")) {

			advisedRequest = this.before(advisedRequest);

			AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

			this.observeAfter(advisedResponse);

			return advisedResponse;
		}

		if (Objects.equals(name, "prompt")) {
			throw new RuntimeException("no impl");
		}

		if (Objects.equals(name, "vector-store")) {
			throw new RuntimeException("no impl");
		}

		return null;
	}

	@Override
	public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

		return null;
	}
	private AdvisedRequest before(AdvisedRequest request) {

		String conversationId = this.doGetConversationId(request.adviseContext());

		int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(request.adviseContext());

		// 1. Retrieve the chat memory for the current conversation.
		List<Message> memoryMessages = this.getChatMemoryStore().get(conversationId, chatMemoryRetrieveSize);

		// 2. Advise the request messages list.
		List<Message> advisedMessages = new ArrayList<>(request.messages());
		advisedMessages.addAll(memoryMessages);

		// 3. Create a new request with the advised messages.
		AdvisedRequest advisedRequest = AdvisedRequest.from(request).withMessages(advisedMessages).build();

		// 4. Add the new user input to the conversation memory.
		UserMessage userMessage = new UserMessage(request.userText(), request.media());
		this.getChatMemoryStore().add(this.doGetConversationId(request.adviseContext()), userMessage);

		return advisedRequest;
	}
	private void observeAfter(AdvisedResponse advisedResponse) {

		List<Message> assistantMessages = advisedResponse.response()
				.getResults()
				.stream()
				.map(g -> (Message) g.getOutput())
				.toList();

		this.getChatMemoryStore().add(this.doGetConversationId(advisedResponse.adviseContext()), assistantMessages);
	}
}
