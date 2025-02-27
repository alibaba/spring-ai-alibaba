package com.alibaba.cloud.ai.memory.strategy;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public abstract class AbstractChatMemoryStrategy implements ChatMemoryStrategy {

	public static final int DEFAULT_CAPACITY = 100;

	protected static void disuse() {
		// no impl
	}

}
