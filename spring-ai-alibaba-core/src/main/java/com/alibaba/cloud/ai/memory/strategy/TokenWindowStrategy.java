package com.alibaba.cloud.ai.memory.strategy;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
public class TokenWindowStrategy extends AbstractChatMemoryStrategy {

	private final String id;
    private final Integer maxTokens;
    private final DashScopeApi.TokenUsage tokenUsage;

	/**
	 * 使用 build
	 * @param id
	 * @param maxTokens
	 * @param tokenUsage
	 */
	public TokenWindowStrategy(String id, Integer maxTokens, DashScopeApi.TokenUsage tokenUsage) {
		this.id = id;
		this.maxTokens = maxTokens;
		this.tokenUsage = tokenUsage;
	}

	@Override
	public void ensureCapacity() {

		// 超出淘汰
		if (this.maxTokens > DEFAULT_CAPACITY) {
			disuse();
		}
	}

}
