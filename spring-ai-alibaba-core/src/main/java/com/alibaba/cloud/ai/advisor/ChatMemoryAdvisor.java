package com.alibaba.cloud.ai.advisor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.memory.store.MySQLChatMemory;
import com.alibaba.cloud.ai.memory.store.RedisChatMemory;
import com.alibaba.cloud.ai.memory.strategy.ChatMemoryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class ChatMemoryAdvisor<T> extends AbstractChatMemoryAdvisor<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChatMemoryAdvisor.class);

	private ChatMemoryStrategy chatMemoryStrategy;

	private final AbstractChatMemoryAdvisor<T> advisor;

	public ChatMemoryAdvisor(T t, AbstractChatMemoryAdvisor<T> advisor) {
		super(t);
		this.advisor = advisor;
	}

	public ChatMemoryAdvisor(T t, AbstractChatMemoryAdvisor<T> advisor, ChatMemoryStrategy chatMemoryStrategy) {
		super(t);
		this.advisor = advisor;
		this.chatMemoryStrategy = chatMemoryStrategy;
	}

	public ChatMemoryAdvisor(T t, String defaultConversationId, int chatHistoryWindowSize,
			AbstractChatMemoryAdvisor<T> advisor) {
		this(t, defaultConversationId, chatHistoryWindowSize, Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER, advisor);
	}

	public ChatMemoryAdvisor(T t, String defaultConversationId, int chatHistoryWindowSize, int order,
			AbstractChatMemoryAdvisor<T> advisor) {
		super(t, defaultConversationId, chatHistoryWindowSize, true, order);
		this.advisor = advisor;
	}

	@Override
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

		this.clearOverLimit(advisedRequest);

		AdvisedResponse advisedResponse = this.checkAdvisorByCall(advisedRequest, chain);
		if (advisedResponse == null) {
			advisedRequest = this.before(advisedRequest);

			advisedResponse = chain.nextAroundCall(advisedRequest);

			this.observeAfter(advisedResponse);

			return advisedResponse;
		}
		return advisedResponse;
	}

	@Override
	public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
		Flux<AdvisedResponse> advisedResponse = this.checkAdvisorByStream(advisedRequest, chain);
		if (advisedResponse == null) {
			Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain,
					this::before);
			return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
		}
		return advisedResponse;
	}

	private AdvisedRequest before(AdvisedRequest request) {
		ChatMemory chatMemory = getSuperChatMemoryStore();
		String conversationId = this.doGetConversationId(request.adviseContext());

		int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(request.adviseContext());

		// 1. Retrieve the chat memory for the current conversation.
		List<Message> memoryMessages = chatMemory.get(conversationId, chatMemoryRetrieveSize);

		// 2. Advise the request messages list.
		List<Message> advisedMessages = new ArrayList<>(request.messages());
		advisedMessages.addAll(memoryMessages);

		// 3. Create a new request with the advised messages.
		AdvisedRequest advisedRequest = AdvisedRequest.from(request).withMessages(advisedMessages).build();

		// 4. Add the new user input to the conversation memory.
		UserMessage userMessage = new UserMessage(request.userText(), request.media());
		chatMemory.add(this.doGetConversationId(request.adviseContext()), userMessage);

		return advisedRequest;
	}

	private void observeAfter(AdvisedResponse advisedResponse) {
		List<Message> assistantMessages = advisedResponse.response()
			.getResults()
			.stream()
			.map(g -> (Message) g.getOutput())
			.toList();
		ChatMemory chatMemory = getSuperChatMemoryStore();
		chatMemory.add(this.doGetConversationId(advisedResponse.adviseContext()), assistantMessages);
	}

	private Flux<AdvisedResponse> checkAdvisorByStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
		if (this.advisor instanceof MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
			return messageChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		}
		else if (this.advisor instanceof PromptChatMemoryAdvisor promptChatMemoryAdvisor) {
			return promptChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		}
		else if (this.advisor instanceof VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
			return vectorStoreChatMemoryAdvisor.aroundStream(advisedRequest, chain);
		}
		return null;
	}

	private AdvisedResponse checkAdvisorByCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
		if (this.advisor instanceof MessageChatMemoryAdvisor messageChatMemoryAdvisor) {
			this.clearOverLimit(advisedRequest);
			return messageChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		}
		else if (this.advisor instanceof PromptChatMemoryAdvisor promptChatMemoryAdvisor) {
			this.clearOverLimit(advisedRequest);
			return promptChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		}
		else if (this.advisor instanceof VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
			return vectorStoreChatMemoryAdvisor.aroundCall(advisedRequest, chain);
		}
		return null;
	}

	private void clearOverLimit(AdvisedRequest advisedRequest) {

		String conversationId = this.doGetConversationId(advisedRequest.adviseContext());

		int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(advisedRequest.adviseContext());
		ChatMemory chatMemory = getSuperChatMemoryStore();
		if (chatMemory != null) {
			List<Message> memoryMessages = chatMemory.get(conversationId, chatMemoryRetrieveSize);

			if (this.chatMemoryStrategy != null) {
				if (this.chatMemoryStrategy.getType().equals("TimeWindow")) {
					// TODO 时间策略待讨论
					// chatMemoryStrategy.ensureCapacity(memoryMessages);
				}
				else if (this.chatMemoryStrategy.getType().equals("TokenWindow")) {
					chatMemoryStrategy.ensureCapacity(memoryMessages);
				}
				else {
					if (this.chatMemoryStore instanceof MySQLChatMemory mySQLChatMemory) {
						mySQLChatMemory.clearOverLimit(conversationId, chatMemoryRetrieveSize, 10);
					}
					if (this.chatMemoryStore instanceof RedisChatMemory redisChatMemory) {
						redisChatMemory.clearOverLimit(conversationId, chatMemoryRetrieveSize, 10);
					}
				}
			}
		}
	}

	private <t> t getSuperChatMemoryStore() {
		try {
			Method getChatMemoryStoreMethod = AbstractChatMemoryAdvisor.class.getDeclaredMethod("getChatMemoryStore");
			getChatMemoryStoreMethod.setAccessible(true);
			return (t) getChatMemoryStoreMethod.invoke(this.advisor);
		}
		catch (NoSuchMethodException e) {
			logger.error("Method not found", e);
			throw new RuntimeException("Method not found", e);
		}
		catch (IllegalAccessException e) {
			logger.error("Illegal access", e);
			throw new RuntimeException("Illegal access", e);
		}
		catch (InvocationTargetException e) {
			logger.error("Invocation target exception", e);
			throw new RuntimeException("Invocation target exception", e);
		}
	}

}
