package com.alibaba.cloud.ai.memory.types;

import java.util.List;

import com.alibaba.cloud.ai.memory.strategy.ChatMemoryStrategy;
import com.alibaba.cloud.ai.memory.strategy.TokenWindowStrategy;

import org.springframework.ai.chat.messages.Message;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class MessageChatMemoryTypes extends AbstractChatMemoryTypes<ChatMemoryStrategy> {

	private static final String NAME = "message";

	public MessageChatMemoryTypes(TokenWindowStrategy chatMemoryStrategy) {

		super(chatMemoryStrategy);
	}

	@Override
	public String getName() {

		return checkTypes(NAME);
	}

	@Override
	public List<Message> findSystemMessages() {
		return null;
	}

	@Override
	public void add(Message e) {

	}

	@Override
	public void clear(Message e) {

	}

	@Override
	public List<Message> message() {
		return null;
	}

}
