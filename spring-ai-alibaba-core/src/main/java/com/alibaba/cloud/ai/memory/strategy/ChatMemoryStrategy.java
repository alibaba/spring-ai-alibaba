package com.alibaba.cloud.ai.memory.strategy;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatMemoryStrategy {

	/**
	 * window 容量
	 */
	void ensureCapacity(List<Message> messages);

	String getType();

}
