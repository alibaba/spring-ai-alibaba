package com.alibaba.cloud.ai.memory;

import com.alibaba.cloud.ai.advisor.ChatMemoryTypesAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.memory.strategy.TokenWindowStrategy;
import com.alibaba.cloud.ai.memory.types.ChatMemoryType;
import com.alibaba.cloud.ai.memory.types.MessageChatMemoryTypes;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
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

	private ChatMemoryType chatMemoryTypes;

	private ChatMemory chatMemory;

    public main(ChatClient.Builder builder, VectorStore vectorStore) {

		chatMemory = new InMemoryChatMemory();
		chatMemoryTypes = new MessageChatMemoryTypes(new TokenWindowStrategy(
						"id",
						10,
						(DashScopeApi.TokenUsage) new Object()
				)
		);

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
                    new ChatMemoryTypesAdvisor(chatMemory, chatMemoryTypes), // MESSAGE CHAT MEMORY
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
