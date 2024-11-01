package com.alibaba.cloud.ai.memory.adapter;

import com.alibaba.cloud.ai.memory.handler.PersistenceHandler;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.persistence.ElasticsearchPersistence;
import com.alibaba.cloud.ai.memory.persistence.MySQLPersistence;
import com.alibaba.cloud.ai.memory.persistence.RedisPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Title persistence processing adapter.<br>
 * Description Used to accommodate processing of different Persistence types .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class PersistenceAdapter {

	@Autowired
	private MySQLPersistence mySQLPersistence;

	@Autowired
	private RedisPersistence redisPersistence;

	@Autowired
	private ElasticsearchPersistence elasticsearchPersistence;

	private PersistenceHandler persistenceHandler;

	public void init(String storageType) {
		switch (storageType.toLowerCase()) {
			case "mysql" -> this.persistenceHandler = mySQLPersistence;
			case "redis" -> this.persistenceHandler = redisPersistence;
			case "es" -> this.persistenceHandler = elasticsearchPersistence;
			default -> throw new IllegalArgumentException("Unsupported storage type: " + storageType);
		}
	}

	public void saveMessage(String conversationId, List<ChatMessage> messages) {
		persistenceHandler.saveMessage(conversationId, messages);
	}

	public List<ChatMessage> getMessages(String conversationId, int windowSize) {
		return persistenceHandler.getMessages(conversationId, windowSize);
	}

	public void updateHistory(String conversationId, List<ChatMessage> messages) {
		persistenceHandler.updateHistory(conversationId, messages);
	}

	public void clearMessages(String conversationId) {
		persistenceHandler.clearMessages(conversationId);
	}

	public void checkAndCreateTable() {
		persistenceHandler.checkAndCreateTable();
	}

}
