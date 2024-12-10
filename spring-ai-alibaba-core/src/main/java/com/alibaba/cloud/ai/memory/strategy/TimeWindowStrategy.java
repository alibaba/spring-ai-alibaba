package com.alibaba.cloud.ai.memory.strategy;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;

/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public class TimeWindowStrategy extends AbstractChatMemoryStrategy {

	private final String id;
    private final Integer timeRange;
    private final DashScopeApi.TokenUsage tokenUsage;

	/**
	 * 使用 build
	 * @param id id
	 * @param timeRange timeRange
	 * @param tokenUsage tokenUsage
	 */
	public TimeWindowStrategy(String id, Integer timeRange, DashScopeApi.TokenUsage tokenUsage) {
		this.id = id;
		this.timeRange = timeRange;
		this.tokenUsage = tokenUsage;
	}

	@Override
	public void ensureCapacity() {

		// 超出淘汰
		if (this.timeRange > DEFAULT_CAPACITY) {
			disuse();
		}
	}

}
