package com.alibaba.cloud.ai.memory.store;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public class InMemoryChatMemory implements ChatMemory {

	@Override
	public void add(String conversationId, List<Message> messages) {

	}

	@Override
	public List<Message> get(String conversationId, int lastN) {

		return null;
	}

	@Override
	public void clear(String conversationId) {

	}
}
