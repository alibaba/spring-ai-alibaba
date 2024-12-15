package com.alibaba.cloud.ai.memory.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(MySQLChatMemory.class);
    Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {
        this.conversationHistory.putIfAbsent(conversationId, new ArrayList<>());
        this.conversationHistory.get(conversationId).addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = this.conversationHistory.get(conversationId);
        return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
    }

    @Override
    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

    public List<Message> clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
        List<Message> all = this.conversationHistory.get(conversationId);
        try {
            if (all.size() >= maxLimit) {
                all = all.stream().skip(Math.max(0, deleteSize)).toList();
                return all;
            }else {
                return all;
            }
        } catch (Exception e) {
            logger.error("Error clearing messages from InMemory chat memory", e);
            throw new RuntimeException(e);
        }
    }
}
