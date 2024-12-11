package com.alibaba.cloud.ai.memory.strategy;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public abstract class AbstractChatMemoryStrategy implements ChatMemoryStrategy {

	public static final int DEFAULT_CAPACITY = 100;

	protected static void disuse() {
		// no impl
	}

	/**
	 * 确保 store 不为空，
	 * 为空使用 默认
	 * 可以使用 spring 注入
	 */
	protected static void ensureStore() {

	}

}
