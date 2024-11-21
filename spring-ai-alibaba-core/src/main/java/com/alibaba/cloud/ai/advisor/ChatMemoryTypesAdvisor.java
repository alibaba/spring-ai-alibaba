package com.alibaba.cloud.ai.advisor;

import java.util.Objects;

import com.alibaba.cloud.ai.memory.types.ChatMemoryType;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;

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
			System.out.println("这是一个 message 类型的 advisor ");
			System.out.println("将会从 types 中获取 message ");
			System.out.println("之后添加到 chat 对话中 ");
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

}
