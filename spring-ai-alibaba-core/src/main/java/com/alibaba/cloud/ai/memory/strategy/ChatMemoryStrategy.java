package com.alibaba.cloud.ai.memory.strategy;


public interface ChatMemoryStrategy {

	/**
	 * window 容量
	 */
	void ensureCapacity();

}
