package com.alibaba.cloud.ai.memory.strategy;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public interface ChatMemoryStrategy {

	/**
	 * window 容量
	 */
	void ensureCapacity();

}
