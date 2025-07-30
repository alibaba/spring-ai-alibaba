package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import java.util.List;

/**
 * Configuration for Redis Memory using Redis Cluster
 *
 * @author benym
 * @date 2025/7/30 21:33
 */
public record RedisMemoryClusterConfiguration(List<String> nodeAddresses, String username, String password,
                                              int timeout) {

}
