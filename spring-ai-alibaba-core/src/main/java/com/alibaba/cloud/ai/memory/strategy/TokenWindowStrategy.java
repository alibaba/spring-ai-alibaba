package com.alibaba.cloud.ai.memory.strategy;

import org.springframework.ai.chat.messages.Message;

import java.util.Iterator;
import java.util.List;

public class TokenWindowStrategy extends AbstractChatMemoryStrategy {

	private String id = "default";

	private final String type = "TokenWindow";

	private Integer maxTokens = 1000;

	private Integer delTokens = 100;

	/**
	 * 使用 build
	 * @param id id
	 * @param maxTokens maxTokens
	 */
	public TokenWindowStrategy(String id, Integer maxTokens, Integer delTokens) {
		this.id = id;
		this.maxTokens = maxTokens;
		this.delTokens = delTokens;
	}

	public TokenWindowStrategy() {
	}

	@Override
	public void ensureCapacity(List<Message> messages) {
		if (messages == null || messages.isEmpty()) {
			return;
		}
		Integer allLen = messages.stream().map(message -> message.getContent().length()).reduce(0, Integer::sum);
		Iterator<Message> iterator = messages.iterator();

		if (allLen >= maxTokens) {
			int del = allLen - maxTokens + delTokens;
			while (iterator.hasNext() && del > 0) {
				Message message = iterator.next();
				messages.remove(message);
				del -= message.getContent().length();
			}
		}
	}

	@Override
	public String getType() {
		return this.type;
	}

}
