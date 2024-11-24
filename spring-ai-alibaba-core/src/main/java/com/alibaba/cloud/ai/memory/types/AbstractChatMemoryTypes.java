package com.alibaba.cloud.ai.memory.types;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public abstract class AbstractChatMemoryTypes<T> implements ChatMemoryType {

	private final T chatMemoryStrategy;

	public AbstractChatMemoryTypes(T chatMemoryStrategy) {

		this.chatMemoryStrategy = chatMemoryStrategy;
	}

	protected T getChatMemoryStrategy() {
		return chatMemoryStrategy;
	}

	protected static String checkTypes(String types) {

		// check types
		return types;
	}

}
