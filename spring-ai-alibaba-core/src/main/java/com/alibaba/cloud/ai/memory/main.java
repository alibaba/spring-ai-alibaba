package com.alibaba.cloud.ai.memory;

import com.alibaba.cloud.ai.advisor.ChatMemoryTypesAdvisor;
import com.alibaba.cloud.ai.memory.strategy.ChatMemoryStrategy;
import com.alibaba.cloud.ai.memory.strategy.TimeWindowStrategy;
import org.springframework.ai.chat.client.advisor.*;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public class main {

	private ChatClient chatClient;

	private MessageChatMemoryAdvisor messageChatMemoryAdvisor;

	private ChatMemory chatMemory;
    private ChatMemoryStrategy chatMemoryStrategy;

    public main(ChatClient.Builder builder, VectorStore vectorStore) {

		chatMemory = new InMemoryChatMemory();
		messageChatMemoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);
        chatMemoryStrategy = new TimeWindowStrategy();
//		chatMemory = new MySQLChatMemory();
//		chatMemoryTypes = new PromptChatMemoryTypes();

        this.chatClient = builder
            .defaultSystem("""
                    You are a customer chat support agent of an airline named "Funnair". Respond in a friendly,
                    helpful, and joyful manner.

                    Before providing information about a booking or cancelling a booking, you MUST always
                    get the following information from the user: booking number, customer first name and last name.

                    Before changing a booking you MUST ensure it is permitted by the terms.

                    If there is a charge for the change, you MUST ask the user to consent before proceeding.
                    """)
            .defaultAdvisors(
//					new MessageChatMemoryAdvisor()
//					new PromptChatMemoryAdvisor()
//					new VectorStoreChatMemoryAdvisor()
                    new ChatMemoryTypesAdvisor(chatMemory,messageChatMemoryAdvisor,chatMemoryStrategy),
                    new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()), // RAG
                    new SimpleLoggerAdvisor())
            .defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking") // FUNCTION CALLING
            .build();
    }

    public Flux<String> chat(String chatId, String userMessageContent) {

        return this.chatClient.prompt()
                .user(userMessageContent)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream().content();
    }


}
