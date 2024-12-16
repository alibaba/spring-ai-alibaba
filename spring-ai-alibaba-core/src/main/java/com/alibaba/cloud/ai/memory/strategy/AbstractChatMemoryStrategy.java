package com.alibaba.cloud.ai.memory.strategy;

public abstract class AbstractChatMemoryStrategy implements ChatMemoryStrategy {

	public static final int DEFAULT_CAPACITY = 100;

	protected static void disuse() {
		// no impl
	}
}
