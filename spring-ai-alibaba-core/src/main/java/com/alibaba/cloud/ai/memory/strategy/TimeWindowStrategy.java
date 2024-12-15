package com.alibaba.cloud.ai.memory.strategy;


public class TimeWindowStrategy extends AbstractChatMemoryStrategy {

	private  String id = "defaultId";
	private  String type = "TimeWindow";
    private  Integer timeRange = 120;
	/**
	 * 使用 build
	 * @param id id
	 * @param timeRange timeRange
	 */
	public TimeWindowStrategy(String id, Integer timeRange) {
		this.id = id;
		this.timeRange = timeRange;
	}
	public TimeWindowStrategy(){}
	@Override
	public void ensureCapacity() {

		// 超出淘汰
		if (this.timeRange > DEFAULT_CAPACITY) {
			disuse();
		}
	}

}
