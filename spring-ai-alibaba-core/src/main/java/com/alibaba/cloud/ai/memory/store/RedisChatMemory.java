package com.alibaba.cloud.ai.memory.store;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * @author wudihaoke214
 * @author yuluo
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */

public class RedisChatMemory implements ChatMemory, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemory.class);

    private static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory";

    private static final String DEFAULT_HOST = "127.0.0.1";

    private static final int DEFAULT_PORT = 6379;

    private static final String DEFAULT_PASSWORD = null;

    private final JedisPool jedisPool;

    private final Jedis jedis;

    private final ObjectMapper objectMapper;

    public RedisChatMemory() {

        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_PASSWORD);
    }

    public RedisChatMemory(String host, int port, String password) {

        JedisPoolConfig poolConfig = new JedisPoolConfig();

        this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        this.jedis = jedisPool.getResource();
        this.objectMapper = new ObjectMapper();

        logger.info("Connected to Redis at {}:{}", host, port);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {

        String key = DEFAULT_KEY_PREFIX + conversationId;

        for (Message message : messages) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                jedis.rpush(key, messageJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing message", e);
            }
        }

        logger.info("Added messages to conversationId: {}", conversationId);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {

        String key = DEFAULT_KEY_PREFIX + conversationId;

        List<String> messageStrings = jedis.lrange(key, -lastN, -1);
        List<Message> messages = new ArrayList<>();

        for (String messageString : messageStrings) {
            try {
                Message message = objectMapper.readValue(messageString, Message.class);
                messages.add(message);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        }

        logger.info("Retrieved {} messages for conversationId: {}", messages.size(), conversationId);

        return messages;
    }

    @Override
    public void clear(String conversationId) {

        String key = DEFAULT_KEY_PREFIX + conversationId;

        jedis.del(key);
        logger.info("Cleared messages for conversationId: {}", conversationId);
    }

    @Override
    public void close() {

        if (jedis != null) {

            jedis.close();

            logger.info("Redis connection closed.");
        }
        if (jedisPool != null) {

            jedisPool.close();

            logger.info("Jedis pool closed.");
        }
    }
    public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
        try {
            String key = DEFAULT_KEY_PREFIX + conversationId;

            List<String> all = jedis.lrange(key, 0, -1);

            if (all.size() >= maxLimit) {
                all = all.stream().skip(Math.max(0, deleteSize)).toList();
            }
            this.clear(conversationId);
            for (String message : all) {
                    jedis.rpush(key, message);
            }
        }
        catch (Exception e) {
            logger.error("Error clearing messages from Redis chat memory", e);
            throw new RuntimeException(e);
        }
    }
}