package com.alibaba.cloud.ai.memory.redis;

import com.alibaba.cloud.ai.memory.redis.serializer.MessageDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * Base class for Redis-based chat memory repositories
 *
 * @author benym
 * @date 2025/7/31 0:05
 */
public abstract class BaseRedisChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

    protected static final Logger logger = LoggerFactory.getLogger(BaseRedisChatMemoryRepository.class);

    protected static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory:";

    protected final ObjectMapper objectMapper;

    public BaseRedisChatMemoryRepository() {
        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Message.class, new MessageDeserializer());
        this.objectMapper.registerModule(module);
    }

    protected Message deserializeMessage(String messageStr) {
        try {
            return objectMapper.readValue(messageStr, Message.class);
        } catch (JsonProcessingException e) {
            logger.error("Deserialization error for message: {}", messageStr, e);
            return null;
        }
    }

    protected String serializeMessage(Message message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message", e);
        }
    }
}
