package com.alibaba.cloud.ai.memory.persistence;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.memory.constant.MemoryConstant;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.enums.MemoryTypeEnum;
import com.alibaba.cloud.ai.memory.handler.PersistenceHandler;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Title redis persistent processing class.<br>
 * Description process the interaction of data with redis.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class RedisPersistence implements PersistenceHandler {

	private static final Logger logger = LoggerFactory.getLogger(RedisPersistence.class);

	@Autowired
	private ChatMemoryProperties memoryProperties;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Override
	public void saveMessage(String conversationId, List<ChatMessage> messages) {
		redisTemplate.opsForValue().set(this.getConversationKey(conversationId), JSON.toJSONString(messages));
	}

	@Override
	public List<ChatMessage> getMessages(String conversationId, int windowSize) {
		String conversationKey = this.getConversationKey(conversationId);
		String messages = redisTemplate.opsForValue().get(conversationKey);
		if (StringUtils.isNotBlank(messages)) {
			try {
				List<ChatMessage> messagesList = JSON.parseArray(messages, ChatMessage.class);
				if (windowSize > 0) {
					return messagesList.stream()
						.skip(Math.max(messagesList.size() - windowSize * 2, 0))
						.collect(Collectors.toList());
				}
				else {
					return messagesList;
				}
			}
			catch (Exception e) {
				logger.error("Failed to parse JSON string to ChatMessage list for key: " + conversationKey, e);
				return CollUtil.newArrayList();
			}
		}
		return CollUtil.newArrayList();
	}

	@Override
	public void updateHistory(String conversationId, List<ChatMessage> messages) {
		redisTemplate.opsForValue().set(this.getConversationKey(conversationId), JSON.toJSONString(messages));
	}

	@Override
	public void clearMessages(String conversationId) {
		redisTemplate.delete(this.getConversationKey(conversationId));
	}

	@Override
	public void checkAndCreateTable() {
		logger.info("redis initialization successfully.");
	}

	private String getConversationKey(String conversationId) {
		return String.format(MemoryConstant.MEMORY_REDIS_KEYS_STORE_PREFIXES, memoryProperties.getMemoryType(),
				conversationId);
	}

}