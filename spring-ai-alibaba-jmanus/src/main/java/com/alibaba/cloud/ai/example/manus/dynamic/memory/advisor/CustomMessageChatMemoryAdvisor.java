package com.alibaba.cloud.ai.example.manus.dynamic.memory.advisor;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * 2025/8/7 auth: dahua
 */
public class CustomMessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {

	private final ChatMemory chatMemory;

	private final String defaultConversationId;

	private final int order;

	private final Scheduler scheduler;

	private String userRequest;

	private CustomMessageChatMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId, int order,
			Scheduler scheduler, String userRequest) {
		Assert.notNull(chatMemory, "chatMemory cannot be null");
		Assert.hasText(defaultConversationId, "defaultConversationId cannot be null or empty");
		Assert.notNull(scheduler, "scheduler cannot be null");
		this.chatMemory = chatMemory;
		this.defaultConversationId = defaultConversationId;
		this.order = order;
		this.scheduler = scheduler;
		this.userRequest = userRequest;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		String conversationId = getConversationId(chatClientRequest.context(), this.defaultConversationId);

		// 1. Retrieve the chat memory for the current conversation.
		List<Message> memoryMessages = this.chatMemory.get(conversationId);

		// 2. Advise the request messages list.
		List<Message> processedMessages = new ArrayList<>(memoryMessages);
		processedMessages.addAll(chatClientRequest.prompt().getInstructions());

		// 3. Create a new request with the advised messages.
		ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
			.build();

		// 4. Add the new user message to the conversation memory.
		UserMessage userMessage = processedChatClientRequest.prompt().getUserMessage();

		// 5. The user's question needs to be re-placed here because the default message
		// body contains template information,
		// which can be misleading when displayed on the front end.
		// Re-placement of the user's actual question does not affect the model's ability
		// to process new requests after loading memory and can also reduce token
		// consumption in the model.
		UserMessage customMessage = UserMessage.builder()
			.text(userRequest)
			.media(userMessage.getMedia())
			.metadata(userMessage.getMetadata())
			.build();
		this.chatMemory.add(conversationId, customMessage);

		return processedChatClientRequest;
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		List<Message> assistantMessages = new ArrayList<>();
		if (chatClientResponse.chatResponse() != null) {
			assistantMessages = chatClientResponse.chatResponse()
				.getResults()
				.stream()
				.map(g -> (Message) g.getOutput())
				.toList();
		}
		this.chatMemory.add(this.getConversationId(chatClientResponse.context(), this.defaultConversationId),
				assistantMessages);
		return chatClientResponse;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		// Get the scheduler from BaseAdvisor
		Scheduler scheduler = this.getScheduler();

		// Process the request with the before method
		return Mono.just(chatClientRequest)
			.publishOn(scheduler)
			.map(request -> this.before(request, streamAdvisorChain))
			.flatMapMany(streamAdvisorChain::nextStream)
			.transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
					response -> this.after(response, streamAdvisorChain)));
	}

	public static CustomMessageChatMemoryAdvisor.Builder builder(ChatMemory chatMemory, String userRequest) {
		return new CustomMessageChatMemoryAdvisor.Builder(chatMemory, userRequest);
	}

	public static final class Builder {

		private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;

		private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;

		private ChatMemory chatMemory;

		private String userRequest;

		private Builder(ChatMemory chatMemory, String userRequest) {
			this.chatMemory = chatMemory;
			this.userRequest = userRequest;
		}

		/**
		 * Set the conversation id.
		 * @param conversationId the conversation id
		 * @return the builder
		 */
		public CustomMessageChatMemoryAdvisor.Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		/**
		 * Set the order.
		 * @param order the order
		 * @return the builder
		 */
		public CustomMessageChatMemoryAdvisor.Builder order(int order) {
			this.order = order;
			return this;
		}

		public CustomMessageChatMemoryAdvisor.Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 * Build the advisor.
		 * @return the advisor
		 */
		public CustomMessageChatMemoryAdvisor build() {
			return new CustomMessageChatMemoryAdvisor(this.chatMemory, this.conversationId, this.order, this.scheduler,
					this.userRequest);
		}

	}

}
