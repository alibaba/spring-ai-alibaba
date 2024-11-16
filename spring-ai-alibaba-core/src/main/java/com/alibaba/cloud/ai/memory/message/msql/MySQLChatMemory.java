package com.alibaba.cloud.ai.memory.message.msql;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class MySQLChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(MySQLChatMemory.class);
    private final MySQLPersistentStorageMemory mySQLPersistentStorageMemory;

    public MySQLChatMemory(MySQLPersistentStorageMemory mySQLPersistentStorageMemory) {
        this.mySQLPersistentStorageMemory = mySQLPersistentStorageMemory;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String historyStr = mySQLPersistentStorageMemory.get(conversationId);
            List<Message> all = objectMapper.readValue(historyStr, new TypeReference<>() {
            });
            all.addAll(messages);
            mySQLPersistentStorageMemory.add(conversationId, objectMapper.writeValueAsString(all));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String historyStr = mySQLPersistentStorageMemory.get(conversationId);
            List<Message> all = objectMapper.readValue(historyStr, new TypeReference<>() {
            });
            return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            mySQLPersistentStorageMemory.delete(conversationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String historyStr = mySQLPersistentStorageMemory.get(conversationId);
            List<Message> all = objectMapper.readValue(historyStr, new TypeReference<>() {
            });
            if (all.size() > maxLimit) {
                all = all.stream().skip(Math.max(0, deleteSize)).toList();
            }
            mySQLPersistentStorageMemory.set(conversationId, objectMapper.writeValueAsString(all));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
