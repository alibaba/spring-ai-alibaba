/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.core.base.entity.LimitEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Redis manager for handling Redis operations. Provides methods for key-value storage,
 * atomic operations, sets, and distributed locking.
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
public class RedisManager {

	/** Default TTL for Redis keys (24 hours) */
	private static final Duration DEFAULT_MAX_TTL = Duration.ofHours(24);

	/** Redisson client for Redis operations */
	private final RedissonClient redissonClient;

	/** Application name used as prefix for Redis keys */
	@Value("${spring.application.name}")
	private String prefix;

	public RedisManager(RedissonClient redissonClient) {
		this.redissonClient = redissonClient;
	}

	/**
	 * Stores a value in Redis with default TTL
	 */
	public <V> void put(String key, V value) {
		String newKey = getPrefix() + key;
		redissonClient.getBucket(newKey).set(value, DEFAULT_MAX_TTL);
	}

	/**
	 * Stores a value in Redis with specified TTL
	 */
	public <V> void put(String key, V value, Duration duration) {
		String newKey = getPrefix() + key;
		RBucket<V> bucket = redissonClient.getBucket(newKey);
		bucket.set(value, duration);
	}

	/**
	 * Retrieves a value from Redis
	 */
	public <V> V get(String key) {
		String newKey = getPrefix() + key;
		RBucket<V> bucket = redissonClient.getBucket(newKey);
		return bucket.get();
	}

	/**
	 * Deletes a key from Redis
	 */
	public boolean delete(String key) {
		String newKey = getPrefix() + key;
		return redissonClient.getBucket(newKey).delete();
	}

	/**
	 * Deletes multiple keys from Redis
	 */
	public long delete(List<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return 0;
		}

		List<String> newKeys = new ArrayList<>();
		for (String key : keys) {
			String newKey = getPrefix() + key;
			newKeys.add(newKey);
		}

		return redissonClient.getKeys().delete(newKeys.toArray(new String[] {}));
	}

	/**
	 * Checks if a key exists in Redis
	 */
	public <V> boolean exists(String key) {
		String newKey = getPrefix() + key;
		RBucket<V> bucket = redissonClient.getBucket(newKey);
		return bucket.isExists();
	}

	/**
	 * Increments and returns the value of an atomic counter
	 */
	public long incrementAndGet(String key) {
		RAtomicLong counter = redissonClient.getAtomicLong(key);
		return counter.incrementAndGet();
	}

	/**
	 * Increments a counter and sets its expiration time
	 */
	public long incrementExpire(String key, Duration duration) {
		RAtomicLong counter = redissonClient.getAtomicLong(key);
		counter.expireAsync(duration);
		return counter.incrementAndGet();
	}

	/**
	 * Gets the current value of an atomic counter
	 */
	public long getIncrement(String key) {
		RAtomicLong counter = redissonClient.getAtomicLong(key);
		return counter.get();
	}

	/**
	 * Increments a counter with time-based expiration If key doesn't exist, initializes
	 * with count=1 and specified TTL
	 */
	public long incrementAndGet(String key, long timeMsToLive) {
		String newKey = getPrefix() + key;
		if (Objects.isNull(get(newKey))) {
			LimitEntity limitEntity = new LimitEntity();
			limitEntity.setCount(11);

			long remainTime = System.currentTimeMillis() + timeMsToLive;
			limitEntity.setTime(remainTime);
			put(newKey, limitEntity, Duration.ofMillis(timeMsToLive));
			return 1;
		}
		else {
			LimitEntity entity = get(newKey);
			int conut = entity.getCount();
			entity.setCount(conut + 1);
			long remainTimeAt = entity.getTime();
			long currentTime = System.currentTimeMillis();
			put(newKey, entity, Duration.ofMillis(remainTimeAt - currentTime));
			return conut + 1;
		}
	}

	/**
	 * Decrements and returns the value of an atomic counter
	 */
	public long decrementAndGet(String key) {
		RAtomicLong count = redissonClient.getAtomicLong(key);
		return count.decrementAndGet();
	}

	/**
	 * Gets the size of a Redis Set
	 */
	public int getSetSize(String key) {
		if (StringUtils.isEmpty(key)) {
			return 0;
		}
		try {
			return redissonClient.getSet(key).size();
		}
		catch (Exception e) {
			log.error("get set size error.key={}", key, e);
			return 0;
		}
	}

	/**
	 * Gets a Redis Set
	 */
	public <V> RSet<V> getSet(String key) {
		if (StringUtils.isEmpty(key)) {
			return null;
		}
		return redissonClient.getSet(key);
	}

	/**
	 * Adds a single value to a Redis Set
	 */
	public <V> void addSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return;
		}
		RSet<V> zset = redissonClient.getSet(key);
		zset.add(uniqueId);
	}

	/**
	 * Adds multiple values to a Redis Set
	 */
	public <V> void addSet(String key, List<V> uniqueIds) {
		if (StringUtils.isEmpty(key) || uniqueIds.isEmpty()) {
			return;
		}
		RSet<V> zset = redissonClient.getSet(key);
		zset.addAll(uniqueIds);
	}

	/**
	 * Adds a value to a Redis Set if its size is less than the specified limit
	 */
	public <V> boolean addSet(String key, V uniqueId, int fixedSize) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		RSet<V> zset = redissonClient.getSet(key);
		if (zset.size() < fixedSize) {
			zset.add(uniqueId);
			return true;
		}
		return false;
	}

	/**
	 * Checks if a value exists in a Redis Set
	 */
	public <V> boolean containsInSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		RSet<V> zset = redissonClient.getSet(key);
		return zset.contains(uniqueId);
	}

	/**
	 * Removes a value from a Redis Set
	 */
	public <V> boolean removeSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		RSet<V> zset = redissonClient.getSet(key);
		zset.remove(uniqueId);
		return true;
	}

	/**
	 * Checks if a Redis Set is empty
	 */
	public <V> boolean isEmptySet(String key) {
		if (StringUtils.isEmpty(key)) {
			return false;
		}
		RSet<V> zset = redissonClient.getSet(key);
		return zset.isEmpty();
	}

	/**
	 * Removes a key from a Redis Map
	 */
	public <V> boolean removeMapKey(String key, String mapKey) {
		RMapCache<String, V> map = redissonClient.getMapCache(key);
		map.remove(mapKey);
		return true;
	}

	/**
	 * Acquires a distributed lock with specified duration Handles deadlock scenarios by
	 * checking expiration time
	 */
	public boolean lock(String key, Duration duration) {
		String newKey = getPrefix() + key;

		long now = System.currentTimeMillis();
		long expireTime = now;
		expireTime += duration.toMillis();

		String expireTimeStr = String.valueOf(expireTime);
		boolean result = redissonClient.getBucket(newKey).setIfAbsent(expireTimeStr, duration);
		if (result) {
			return true;
		}

		// Check for deadlock scenarios
		String currentExpireTimeStr = (String) redissonClient.getBucket(newKey).get();
		if (currentExpireTimeStr != null) {
			long currentExpireTime = Long.parseLong(currentExpireTimeStr);
			if (currentExpireTime < now) {
				// Key has expired
				String lastExpireTimeStr = (String) redissonClient.getBucket(newKey).getAndSet(expireTimeStr, duration);
				return lastExpireTimeStr != null && lastExpireTimeStr.equals(currentExpireTimeStr);
			}
		}
		return false;
	}

	/**
	 * Acquires a distributed lock with blocking wait support using Redisson's built-in
	 * lock This method provides better performance and reliability
	 * @param key the lock key
	 * @param lockDuration the duration to hold the lock
	 * @param waitTimeout the maximum time to wait for the lock
	 * @return true if lock was acquired, false if timeout occurred
	 */
	public boolean lockWithRedisson(String key, Duration lockDuration, Duration waitTimeout) {
		String newKey = getPrefix() + key;
		RLock lock = redissonClient.getLock(newKey);

		try {
			// 尝试获取锁，支持等待超时
			return lock.tryLock(waitTimeout.toMillis(), lockDuration.toMillis(),
					java.util.concurrent.TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Redisson lock wait interrupted for key: {}", key);
			return false;
		}
	}

	/**
	 * Releases a distributed lock
	 */
	public void unlock(String key) {
		String newKey = getPrefix() + key;
		redissonClient.getBucket(newKey).delete();
	}

	/**
	 * Releases a Redisson distributed lock
	 * @param key the lock key
	 */
	public void unlockRedisson(String key) {
		String newKey = getPrefix() + key;
		RLock lock = redissonClient.getLock(newKey);
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
	}

	/**
	 * Gets the Redis key prefix
	 */
	public String getPrefix() {
		return prefix + ":";
	}

}
