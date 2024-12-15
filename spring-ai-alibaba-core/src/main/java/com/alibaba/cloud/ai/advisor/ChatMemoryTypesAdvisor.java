package com.alibaba.cloud.ai.advisor;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.memory.store.InMemoryChatMemory;
import com.alibaba.cloud.ai.memory.strategy.ChatMemoryStrategy;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;


public class ChatMemoryTypesAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

	private ChatMemoryStrategy chatMemoryStrategy;
	private AbstractChatMemoryAdvisor advisor;

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, AbstractChatMemoryAdvisor chatMemoryAdvisor, ChatMemoryStrategy chatMemoryStrategy) {
		super(chatMemory);
		this.advisor = advisor;
		this.chatMemoryStrategy = chatMemoryStrategy;
	}

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, String defaultConversationId, int chatHistoryWindowSize) {
		this(chatMemory, defaultConversationId, chatHistoryWindowSize, Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER);
	}

	public ChatMemoryTypesAdvisor(ChatMemory chatMemory, String defaultConversationId, int chatHistoryWindowSize,
								  int order) {
		super(chatMemory, defaultConversationId, chatHistoryWindowSize, true, order);
	}

	@Override
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
		this.clearOverLimit(advisedRequest);
		if (this.advisor instanceof MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
			return messageChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		} else if (this.advisor instanceof PromptChatMemoryAdvisor promptChatMemoryAdvisor) {
			return promptChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		} else if (this.advisor instanceof VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
			return vectorStoreChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		} else {
			advisedRequest = this.before(advisedRequest);

			AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

			this.observeAfter(advisedResponse);

			return advisedResponse;
		}
	}

	@Override
	public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
		if (this.advisor instanceof MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
			return messageChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		} else if (this.advisor instanceof PromptChatMemoryAdvisor promptChatMemoryAdvisor) {
			return promptChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		} else if (this.advisor instanceof VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
			return vectorStoreChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		} else {
			Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain,
					this::before);
			return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
		}
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

	private void clearOverLimit(AdvisedRequest advisedRequest) {

		String conversationId = this.doGetConversationId(advisedRequest.adviseContext());

		int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(advisedRequest.adviseContext());

		List<Message> memoryMessages = this.getChatMemoryStore().get(conversationId, chatMemoryRetrieveSize);

		if(this.chatMemoryStrategy.getType().equals("TimeWindow")){

		}else if (this.chatMemoryStrategy.getType().equals("TokenWindow")){

		}else{
			if(this.chatMemoryStore instanceof InMemoryChatMemory inMemoryChatMemory){
				inMemoryChatMemory.clearOverLimit(conversationId,chatMemoryRetrieveSize,10);
			}
		}
	}
}
