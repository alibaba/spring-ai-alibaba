package com.alibaba.cloud.ai.memory.strategy;

import org.springframework.ai.chat.messages.Message;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TimeWindowStrategy extends AbstractChatMemoryStrategy {

	private String id = "default";

	private String type = "TimeWindow";

	// 默认10分钟
	private Integer timeRange = 10;

	/**
	 * 使用 build
	 * @param id id
	 * @param timeRange timeRange
	 */
	public TimeWindowStrategy(String id, Integer timeRange) {
		this.id = id;
		this.timeRange = timeRange;
	}

	public TimeWindowStrategy() {
	}

	@Override
	public void ensureCapacity(List<Message> messages) {
		// LocalDateTime currentTime = LocalDateTime.now();
		//
		// List<Message> messageList = messages.stream()
		// .filter(message ->
		// currentTime.minus(message.getTimestamp()).get(ChronoUnit.MINUTES) <= timeRange)
		// .toList();
		// messages.clear();
		// messages.addAll(messageList);
	}

	@Override
	public String getType() {
		return this.type;
	}

}
