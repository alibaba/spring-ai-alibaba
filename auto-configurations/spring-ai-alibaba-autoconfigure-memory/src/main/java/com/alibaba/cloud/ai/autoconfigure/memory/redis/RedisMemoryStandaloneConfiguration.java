package com.alibaba.cloud.ai.autoconfigure.memory.redis;

/**
 * Configuration for Redis Memory using Redis Standalone
 *
 * @author benym
 * @date 2025/7/30 21:32
 */
public record RedisMemoryStandaloneConfiguration(String hostName, int port, String username, String password,
                                                 int timeout) {

}
