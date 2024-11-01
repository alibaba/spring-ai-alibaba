package com.alibaba.cloud.ai.memory.persistence;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.entity.ConversationMemoryForMySQL;
import com.alibaba.cloud.ai.memory.handler.PersistenceHandler;
import com.alibaba.cloud.ai.memory.mapper.ConversationMemoryMapper;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Title mysql persistent processing class.<br>
 * Description process the interaction of data with mysql.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class MySQLPersistence implements PersistenceHandler {

	private static final Logger logger = LoggerFactory.getLogger(MySQLPersistence.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ConversationMemoryMapper conversationMemoryMapper;

	@Autowired
	private ChatMemoryProperties memoryProperties;

	@Override
	public void saveMessage(String conversationId, List<ChatMessage> messages) {
		QueryWrapper<ConversationMemoryForMySQL> wrapper = new QueryWrapper<>();
		wrapper.eq("conversation_id", conversationId);
		wrapper.eq("memory_type", memoryProperties.getMemoryType());
		Long selectCount = conversationMemoryMapper.selectCount(wrapper);
		if (selectCount > 0) {
			UpdateWrapper<ConversationMemoryForMySQL> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("conversation_id", conversationId)
				.eq("memory_type", memoryProperties.getMemoryType())
				.set("content", JSON.toJSONString(messages));
			conversationMemoryMapper.update(null, updateWrapper);
		}
		else {
			String content = JSON.toJSONString(messages);
			ConversationMemoryForMySQL conversationMemory = new ConversationMemoryForMySQL();
			conversationMemory.setId(IdUtil.simpleUUID());
			conversationMemory.setConversationId(conversationId);
			conversationMemory.setContent(content);
			conversationMemory.setMemoryType(memoryProperties.getMemoryType());
			conversationMemoryMapper.insert(conversationMemory);
		}
	}

	@Override
	public List<ChatMessage> getMessages(String conversationId, int windowSize) {
		LambdaQueryWrapper<ConversationMemoryForMySQL> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ConversationMemoryForMySQL::getConversationId, conversationId)
			.eq(ConversationMemoryForMySQL::getMemoryType, memoryProperties.getMemoryType());

		List<ConversationMemoryForMySQL> records = conversationMemoryMapper.selectList(queryWrapper);
		if (CollUtil.isEmpty(records)) {
			return CollUtil.newArrayList();
		}
		ConversationMemoryForMySQL conversationMemoryForMySQL = records.get(0);
		if (StringUtils.isBlank(conversationMemoryForMySQL.getContent())) {
			return CollUtil.newArrayList();
		}
		String content = conversationMemoryForMySQL.getContent();
		List<ChatMessage> messagesList = JSON.parseArray(content, ChatMessage.class);
		if (windowSize > 0) {
			return messagesList.stream()
				.skip(Math.max(messagesList.size() - windowSize * 2, 0))
				.collect(Collectors.toList());
		}
		else {
			return messagesList;
		}
	}

	@Override
	public void updateHistory(String conversationId, List<ChatMessage> messages) {
		UpdateWrapper<ConversationMemoryForMySQL> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("conversation_id", conversationId)
			.eq("memory_type", memoryProperties.getMemoryType())
			.set("content", JSON.toJSONString(messages));
		conversationMemoryMapper.update(null, updateWrapper);
	}

	@Override
	public void clearMessages(String conversationId) {
		UpdateWrapper<ConversationMemoryForMySQL> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("conversation_id", conversationId)
			.eq("memory_type", memoryProperties.getMemoryType())
			.set("content", null);
		conversationMemoryMapper.update(null, updateWrapper);
	}

	@Override
	public void checkAndCreateTable() {
		String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
		int count = jdbcTemplate.queryForObject(checkSql, new Object[] { "conversation_memory" }, Integer.class);
		if (count == 0) {
			String sql = """
					CREATE TABLE IF NOT EXISTS conversation_memory (
					    id VARCHAR(64) PRIMARY KEY,
					    conversation_id VARCHAR(64),
					    content JSON,
					    memory_type VARCHAR(32),
					    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
					    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
					)
					""";
			jdbcTemplate.execute(sql);
			logger.info("Table 'conversation_memory' created successfully.");
		}
		else {
			logger.info("Table 'conversation_memory' already exists.");
		}
	}

}