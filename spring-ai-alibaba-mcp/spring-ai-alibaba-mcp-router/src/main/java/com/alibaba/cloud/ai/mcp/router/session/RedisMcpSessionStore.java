/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mcp.router.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based MCP Session store implementation for multi-instance deployment
 *
 * @author Libres-coder
 * @since 2025.10.16
 */
public class RedisMcpSessionStore implements McpSessionStore {

	private static final Logger logger = LoggerFactory.getLogger(RedisMcpSessionStore.class);

	private static final String DEFAULT_KEY_PREFIX = "mcp:session:";

	private static final long DEFAULT_TTL_SECONDS = 1800; // 30 minutes

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper;

	private final String keyPrefix;

	private final long ttlSeconds;

	/**
	 * 构造函数
	 * @param stringRedisTemplate Spring Redis 模板
	 * @param objectMapper JSON 序列化工具
	 */
	public RedisMcpSessionStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this(stringRedisTemplate, objectMapper, DEFAULT_KEY_PREFIX, DEFAULT_TTL_SECONDS);
	}

	/**
	 * 构造函数
	 * @param stringRedisTemplate Spring Redis 模板
	 * @param objectMapper JSON 序列化工具
	 * @param keyPrefix Redis key 前缀
	 * @param ttlSeconds Session 过期时间（秒）
	 */
	public RedisMcpSessionStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, String keyPrefix,
			long ttlSeconds) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.keyPrefix = keyPrefix != null ? keyPrefix : DEFAULT_KEY_PREFIX;
		this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
		logger.info("Initialized RedisMcpSessionStore with keyPrefix: {}, ttl: {}s", this.keyPrefix, this.ttlSeconds);
	}

	@Override
	public void put(String serviceName, Object sessionData) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null, session not stored");
			return;
		}

		try {
			String key = buildKey(serviceName);
			String value = objectMapper.writeValueAsString(sessionData);
			stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
			logger.debug("Stored session in Redis for service: {}, key: {}", serviceName, key);
		}
		catch (Exception e) {
			logger.error("Failed to store session in Redis for service: {}", serviceName, e);
			throw new RuntimeException("Failed to store session in Redis", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String serviceName) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null");
			return null;
		}

		try {
			String key = buildKey(serviceName);
			String value = stringRedisTemplate.opsForValue().get(key);

			if (value == null) {
				logger.debug("No session found in Redis for service: {}, key: {}", serviceName, key);
				return null;
			}

			// 尝试将 JSON 反序列化为 Map
			Object sessionData = objectMapper.readValue(value, Object.class);
			logger.debug("Retrieved session from Redis for service: {}, key: {}", serviceName, key);
			return (T) sessionData;
		}
		catch (Exception e) {
			logger.error("Failed to retrieve session from Redis for service: {}", serviceName, e);
			return null;
		}
	}

	@Override
	public void remove(String serviceName) {
		if (serviceName == null) {
			logger.warn("Service name cannot be null");
			return;
		}

		try {
			String key = buildKey(serviceName);
			Boolean deleted = stringRedisTemplate.delete(key);
			logger.debug("Removed session from Redis for service: {}, key: {}, deleted: {}", serviceName, key,
					deleted);
		}
		catch (Exception e) {
			logger.error("Failed to remove session from Redis for service: {}", serviceName, e);
		}
	}

	@Override
	public boolean contains(String serviceName) {
		if (serviceName == null) {
			return false;
		}

		try {
			String key = buildKey(serviceName);
			Boolean exists = stringRedisTemplate.hasKey(key);
			return exists != null && exists;
		}
		catch (Exception e) {
			logger.error("Failed to check session existence in Redis for service: {}", serviceName, e);
			return false;
		}
	}

	@Override
	public void clear() {
		try {
			String pattern = keyPrefix + "*";
			Set<String> keys = stringRedisTemplate.keys(pattern);

			if (keys != null && !keys.isEmpty()) {
				Long deletedCount = stringRedisTemplate.delete(keys);
				logger.info("Cleared all sessions from Redis, pattern: {}, count: {}", pattern, deletedCount);
			}
			else {
				logger.info("No sessions to clear in Redis, pattern: {}", pattern);
			}
		}
		catch (Exception e) {
			logger.error("Failed to clear all sessions from Redis", e);
		}
	}

	@Override
	public int size() {
		try {
			String pattern = keyPrefix + "*";
			Set<String> keys = stringRedisTemplate.keys(pattern);
			return keys != null ? keys.size() : 0;
		}
		catch (Exception e) {
			logger.error("Failed to get session count from Redis", e);
			return 0;
		}
	}

	@Override
	public Map<String, Object> getAll() {
		Map<String, Object> result = new HashMap<>();

		try {
			String pattern = keyPrefix + "*";
			Set<String> keys = stringRedisTemplate.keys(pattern);

			if (keys != null) {
				for (String key : keys) {
					String serviceName = extractServiceName(key);
					Object sessionData = get(serviceName);
					if (sessionData != null) {
						result.put(serviceName, sessionData);
					}
				}
			}

			logger.debug("Retrieved all sessions from Redis, count: {}", result.size());
		}
		catch (Exception e) {
			logger.error("Failed to retrieve all sessions from Redis", e);
		}

		return result;
	}

	/**
	 * 构建 Redis key
	 * @param serviceName 服务名称
	 * @return 完整的 Redis key
	 */
	private String buildKey(String serviceName) {
		return keyPrefix + serviceName;
	}

	/**
	 * 从 Redis key 中提取服务名称
	 * @param key Redis key
	 * @return 服务名称
	 */
	private String extractServiceName(String key) {
		if (key.startsWith(keyPrefix)) {
			return key.substring(keyPrefix.length());
		}
		return key;
	}

}

