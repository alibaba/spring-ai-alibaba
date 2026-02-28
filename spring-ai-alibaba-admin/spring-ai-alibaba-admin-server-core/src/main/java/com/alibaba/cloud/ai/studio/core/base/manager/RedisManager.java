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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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
	private RedissonClient redissonClient;

	/** Application name used as prefix for Redis keys */
	@Value("${spring.application.name}")
	private String prefix;

	/** Local cache for fallback when Redis is not available */
	private final Map<String, Object> localCache = new ConcurrentHashMap<>();
	private final Map<String, Long> localExpireMap = new ConcurrentHashMap<>();
	private final Map<String, Set<Object>> localSetCache = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> localCounterCache = new ConcurrentHashMap<>();
	private final Map<String, ReentrantLock> localLockMap = new ConcurrentHashMap<>();

	@Autowired(required = false)
	public void setRedissonClient(RedissonClient redissonClient) {
		this.redissonClient = redissonClient;
		if (redissonClient == null) {
			log.warn("Redis is not configured, using local memory cache as fallback. " +
					"Note: Distributed features like locks will only work within a single instance.");
		} else {
			log.info("Redis is configured and connected.");
		}
	}

	private boolean isRedisAvailable() {
		return redissonClient != null;
	}

	private void checkAndCleanExpired(String key) {
		Long expireTime = localExpireMap.get(key);
		if (expireTime != null && System.currentTimeMillis() > expireTime) {
			localCache.remove(key);
			localExpireMap.remove(key);
			localSetCache.remove(key);
			localCounterCache.remove(key);
		}
	}

	/**
	 * Stores a value in Redis with default TTL
	 */
	public <V> void put(String key, V value) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			redissonClient.getBucket(newKey).set(value, DEFAULT_MAX_TTL);
		} else {
			localCache.put(newKey, value);
			localExpireMap.put(newKey, System.currentTimeMillis() + DEFAULT_MAX_TTL.toMillis());
		}
	}

	/**
	 * Stores a value in Redis with specified TTL
	 */
	public <V> void put(String key, V value, Duration duration) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			RBucket<V> bucket = redissonClient.getBucket(newKey);
			bucket.set(value, duration);
		} else {
			localCache.put(newKey, value);
			localExpireMap.put(newKey, System.currentTimeMillis() + duration.toMillis());
		}
	}

	/**
	 * Retrieves a value from Redis
	 */
	@SuppressWarnings("unchecked")
	public <V> V get(String key) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			RBucket<V> bucket = redissonClient.getBucket(newKey);
			return bucket.get();
		} else {
			checkAndCleanExpired(newKey);
			return (V) localCache.get(newKey);
		}
	}

	/**
	 * Deletes a key from Redis
	 */
	public boolean delete(String key) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			return redissonClient.getBucket(newKey).delete();
		} else {
			localExpireMap.remove(newKey);
			localSetCache.remove(newKey);
			localCounterCache.remove(newKey);
			return localCache.remove(newKey) != null;
		}
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

		if (isRedisAvailable()) {
			return redissonClient.getKeys().delete(newKeys.toArray(new String[] {}));
		} else {
			long count = 0;
			for (String newKey : newKeys) {
				localExpireMap.remove(newKey);
				localSetCache.remove(newKey);
				localCounterCache.remove(newKey);
				if (localCache.remove(newKey) != null) {
					count++;
				}
			}
			return count;
		}
	}

	/**
	 * Checks if a key exists in Redis
	 */
	public <V> boolean exists(String key) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			RBucket<V> bucket = redissonClient.getBucket(newKey);
			return bucket.isExists();
		} else {
			checkAndCleanExpired(newKey);
			return localCache.containsKey(newKey);
		}
	}

	/**
	 * Increments and returns the value of an atomic counter
	 */
	public long incrementAndGet(String key) {
		if (isRedisAvailable()) {
			RAtomicLong counter = redissonClient.getAtomicLong(key);
			return counter.incrementAndGet();
		} else {
			AtomicLong counter = localCounterCache.computeIfAbsent(key, k -> new AtomicLong(0));
			return counter.incrementAndGet();
		}
	}

	/**
	 * Increments a counter and sets its expiration time
	 */
	public long incrementExpire(String key, Duration duration) {
		if (isRedisAvailable()) {
			RAtomicLong counter = redissonClient.getAtomicLong(key);
			counter.expireAsync(duration);
			return counter.incrementAndGet();
		} else {
			AtomicLong counter = localCounterCache.computeIfAbsent(key, k -> new AtomicLong(0));
			localExpireMap.put(key, System.currentTimeMillis() + duration.toMillis());
			return counter.incrementAndGet();
		}
	}

	/**
	 * Gets the current value of an atomic counter
	 */
	public long getIncrement(String key) {
		if (isRedisAvailable()) {
			RAtomicLong counter = redissonClient.getAtomicLong(key);
			return counter.get();
		} else {
			AtomicLong counter = localCounterCache.get(key);
			return counter != null ? counter.get() : 0;
		}
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
		if (isRedisAvailable()) {
			RAtomicLong count = redissonClient.getAtomicLong(key);
			return count.decrementAndGet();
		} else {
			AtomicLong counter = localCounterCache.computeIfAbsent(key, k -> new AtomicLong(0));
			return counter.decrementAndGet();
		}
	}

	/**
	 * Gets the size of a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public int getSetSize(String key) {
		if (StringUtils.isEmpty(key)) {
			return 0;
		}
		if (isRedisAvailable()) {
			try {
				return redissonClient.getSet(key).size();
			}
			catch (Exception e) {
				log.error("get set size error.key={}", key, e);
				return 0;
			}
		} else {
			Set<Object> set = localSetCache.get(key);
			return set != null ? set.size() : 0;
		}
	}

	/**
	 * Gets a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public <V> RSet<V> getSet(String key) {
		if (StringUtils.isEmpty(key)) {
			return null;
		}
		if (isRedisAvailable()) {
			return redissonClient.getSet(key);
		} else {
			// Local cache doesn't support RSet interface, return null for fallback
			log.warn("getSet with local cache fallback is limited. Key: {}", key);
			return null;
		}
	}

	/**
	 * Adds a single value to a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public <V> void addSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			zset.add(uniqueId);
		} else {
			localSetCache.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(uniqueId);
		}
	}

	/**
	 * Adds multiple values to a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public <V> void addSet(String key, List<V> uniqueIds) {
		if (StringUtils.isEmpty(key) || uniqueIds.isEmpty()) {
			return;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			zset.addAll(uniqueIds);
		} else {
			Set<Object> set = localSetCache.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
			set.addAll(uniqueIds);
		}
	}

	/**
	 * Adds a value to a Redis Set if its size is less than the specified limit
	 */
	@SuppressWarnings("unchecked")
	public <V> boolean addSet(String key, V uniqueId, int fixedSize) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			if (zset.size() < fixedSize) {
				zset.add(uniqueId);
				return true;
			}
			return false;
		} else {
			Set<Object> set = localSetCache.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
			synchronized (set) {
				if (set.size() < fixedSize) {
					return set.add(uniqueId);
				}
				return false;
			}
		}
	}

	/**
	 * Checks if a value exists in a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public <V> boolean containsInSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			return zset.contains(uniqueId);
		} else {
			Set<Object> set = localSetCache.get(key);
			return set != null && set.contains(uniqueId);
		}
	}

	/**
	 * Removes a value from a Redis Set
	 */
	@SuppressWarnings("unchecked")
	public <V> boolean removeSet(String key, V uniqueId) {
		if (StringUtils.isEmpty(key) || uniqueId == null) {
			return false;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			zset.remove(uniqueId);
			return true;
		} else {
			Set<Object> set = localSetCache.get(key);
			if (set != null) {
				return set.remove(uniqueId);
			}
			return false;
		}
	}

	/**
	 * Checks if a Redis Set is empty
	 */
	public <V> boolean isEmptySet(String key) {
		if (StringUtils.isEmpty(key)) {
			return false;
		}
		if (isRedisAvailable()) {
			RSet<V> zset = redissonClient.getSet(key);
			return zset.isEmpty();
		} else {
			Set<Object> set = localSetCache.get(key);
			return set == null || set.isEmpty();
		}
	}

	/**
	 * Removes a key from a Redis Map
	 */
	@SuppressWarnings("unchecked")
	public <V> boolean removeMapKey(String key, String mapKey) {
		if (isRedisAvailable()) {
			RMapCache<String, V> map = redissonClient.getMapCache(key);
			map.remove(mapKey);
			return true;
		} else {
			// Local cache fallback: store as Map in localCache
			Object obj = localCache.get(key);
			if (obj instanceof Map) {
				((Map<String, Object>) obj).remove(mapKey);
			}
			return true;
		}
	}

	/**
	 * Acquires a distributed lock with specified duration Handles deadlock scenarios by
	 * checking expiration time
	 */
	public boolean lock(String key, Duration duration) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
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
		} else {
			// Local lock fallback - only works within single JVM
			ReentrantLock lock = localLockMap.computeIfAbsent(newKey, k -> new ReentrantLock());
			try {
				return lock.tryLock(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
	}

	/**
	 * Acquires a distributed lock with blocking wait support using Redisson's built-in
	 * lock This method provides better performance and reliability
	 * @param key the lock key
	 * @param hostDuration the duration to hold the lock
	 * @param waitTimeout the maximum time to wait for the lock
	 * @return true if lock was acquired, false if timeout occurred
	 */
	public boolean lockWithRedisson(String key, Duration hostDuration, Duration waitTimeout) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			RLock lock = redissonClient.getLock(newKey);

			try {
				// 尝试获取锁，支持等待超时
				return lock.tryLock(waitTimeout.toMillis(), hostDuration.toMillis(),
						java.util.concurrent.TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Redisson lock wait interrupted for key: {}", key);
				return false;
			}
		} else {
			// Local lock fallback - only works within single JVM
			ReentrantLock lock = localLockMap.computeIfAbsent(newKey, k -> new ReentrantLock());
			try {
				return lock.tryLock(waitTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Local lock wait interrupted for key: {}", key);
				return false;
			}
		}
	}

	/**
	 * Releases a distributed lock
	 */
	public void unlock(String key) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			redissonClient.getBucket(newKey).delete();
		} else {
			ReentrantLock lock = localLockMap.get(newKey);
			if (lock != null && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * Releases a Redisson distributed lock
	 * @param key the lock key
	 */
	public void unlockRedisson(String key) {
		String newKey = getPrefix() + key;
		if (isRedisAvailable()) {
			RLock lock = redissonClient.getLock(newKey);
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		} else {
			ReentrantLock lock = localLockMap.get(newKey);
			if (lock != null && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * Gets the Redis key prefix
	 */
	public String getPrefix() {
		return prefix + ":";
	}

}
