package com.alibaba.cloud.ai.memory.strategy;

public class TokenWindowStrategy extends AbstractChatMemoryStrategy {

	private  String id = "default";
	private  String type = "TokenWindow";
    private  Integer maxTokens = 1000;
	/**
	 * 使用 build
	 * @param id
	 * @param maxTokens
	 */
	public TokenWindowStrategy(String id, Integer maxTokens) {
		this.id = id;
		this.maxTokens = maxTokens;
	}
	public TokenWindowStrategy() {}

	@Override
	public void ensureCapacity() {

		// 超出淘汰
		if (this.maxTokens > DEFAULT_CAPACITY) {
			disuse();
		}
	}

}
