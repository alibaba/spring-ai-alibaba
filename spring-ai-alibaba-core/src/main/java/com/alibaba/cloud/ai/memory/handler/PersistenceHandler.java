package com.alibaba.cloud.ai.memory.handler;

import com.alibaba.cloud.ai.memory.entity.ChatMessage;

import java.util.List;

/**
 * Title persistence type handler interface.<br>
 * Description conversations adapt to different persistence styles.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

public interface PersistenceHandler {

	void saveMessage(String conversationId, List<ChatMessage> messages);

	List<ChatMessage> getMessages(String conversationId, int windowSize);

	void updateHistory(String conversationId, List<ChatMessage> messages);

	void clearMessages(String conversationId);

	void checkAndCreateTable();

}