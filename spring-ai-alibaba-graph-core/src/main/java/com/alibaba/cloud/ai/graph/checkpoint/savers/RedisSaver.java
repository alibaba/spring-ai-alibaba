/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.checkpoint.savers;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * The type Redis saver.
 *
 * @author disaster
 * @since 1.0.0-M2
 */
public class RedisSaver implements BaseCheckpointSaver {

	private RedisClientAdapter redisClient;

	private final ObjectMapper objectMapper;

	private static final String PREFIX = "graph:checkpoint:content:";

	private static final String LOCK_PREFIX = "graph:checkpoint:lock:";

	/**
	 * Instantiates a new Redis saver with Redisson client.
	 * 
	 * @param redisson the redisson client
	 */
	public RedisSaver(RedissonClient redisson) {
		this(new RedissonClientAdapter(redisson), new ObjectMapper());
	}

	/**
	 * Instantiates a new Redis saver with Jedis pool.
	 * 
	 * @param jedisPool the jedis pool
	 */
	public RedisSaver(JedisPool jedisPool) {
		this(new JedisClientAdapter(jedisPool), new ObjectMapper());
	}

	/**
	 * Instantiates a new Redis saver with Redisson client and custom object mapper.
	 * 
	 * @param redisson the redisson client
	 * @param objectMapper the object mapper
	 */
	public RedisSaver(RedissonClient redisson, ObjectMapper objectMapper) {
		this(new RedissonClientAdapter(redisson), objectMapper);
	}

	/**
	 * Instantiates a new Redis saver with Jedis pool and custom object mapper.
	 * 
	 * @param jedisPool the jedis pool
	 * @param objectMapper the object mapper
	 */
	public RedisSaver(JedisPool jedisPool, ObjectMapper objectMapper) {
		this(new JedisClientAdapter(jedisPool), objectMapper);
	}

	/**
	 * Instantiates a new Redis saver with a Redis client adapter.
	 * 
	 * @param redisClient the redis client adapter
	 * @param objectMapper the object mapper
	 */
	private RedisSaver(RedisClientAdapter redisClient, ObjectMapper objectMapper) {
		this.redisClient = redisClient;
		this.objectMapper = BaseCheckpointSaver.configureObjectMapper(objectMapper);
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			String threadId = configOption.get();
			boolean tryLock = false;
			try {
				tryLock = redisClient.tryLock(LOCK_PREFIX + threadId, 2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					String content = redisClient.get(PREFIX + threadId);
					if (content == null) {
						return new LinkedList<>();
					}
					return objectMapper.readValue(content, new TypeReference<>() {
					});
				} else {
					return List.of();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (JsonMappingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			} finally {
				if (tryLock) {
					redisClient.unlock(LOCK_PREFIX + threadId);
				}
			}
		} else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			String threadId = configOption.get();
			boolean tryLock = false;
			try {
				tryLock = redisClient.tryLock(LOCK_PREFIX + threadId, 2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					String content = redisClient.get(PREFIX + threadId);
					List<Checkpoint> checkpoints;
					if (content == null) {
						checkpoints = new LinkedList<>();
					} else {
						checkpoints = objectMapper.readValue(content, new TypeReference<>() {
						});
					}
					if (config.checkPointId().isPresent()) {
						return config.checkPointId()
								.flatMap(id -> checkpoints.stream()
										.filter(checkpoint -> checkpoint.getId().equals(id))
										.findFirst());
					}
					return getLast(getLinkedList(checkpoints), config);
				} else {
					return Optional.empty();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (JsonMappingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to parse JSON", e);
			} finally {
				if (tryLock) {
					redisClient.unlock(LOCK_PREFIX + threadId);
				}
			}
		} else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			String threadId = configOption.get();
			boolean tryLock = false;
			try {
				tryLock = redisClient.tryLock(LOCK_PREFIX + threadId, 5, TimeUnit.MILLISECONDS);
				if (tryLock) {
					String content = redisClient.get(PREFIX + threadId);
					List<Checkpoint> checkpoints;
					if (content == null) {
						checkpoints = new LinkedList<>();
					} else {
						checkpoints = objectMapper.readValue(content, new TypeReference<>() {
						});
					}
					LinkedList<Checkpoint> linkedList = getLinkedList(checkpoints);
					if (config.checkPointId().isPresent()) { // Replace Checkpoint
						String checkPointId = config.checkPointId().get();
						int index = IntStream.range(0, checkpoints.size())
								.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
								.findFirst()
								.orElseThrow(() -> (new NoSuchElementException(
										format("Checkpoint with id %s not found!", checkPointId))));
						linkedList.set(index, checkpoint);
						redisClient.set(PREFIX + threadId, objectMapper.writeValueAsString(linkedList));
						return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
					}
					linkedList.push(checkpoint); // Add Checkpoint
					redisClient.set(PREFIX + threadId, objectMapper.writeValueAsString(linkedList));
				}
                return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				if (tryLock) {
					redisClient.unlock(LOCK_PREFIX + threadId);
				}
			}
		} else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

	@Override
	public boolean clear(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			String threadId = configOption.get();
			boolean tryLock = false;
			try {
				tryLock = redisClient.tryLock(LOCK_PREFIX + threadId, 2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					redisClient.set(PREFIX + threadId, objectMapper.writeValueAsString(List.of()));
					return tryLock;
				}
				return false;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to serialize JSON", e);
			} finally {
				if (tryLock) {
					redisClient.unlock(LOCK_PREFIX + threadId);
				}
			}
		} else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

	/**
	 * Adapter interface for different Redis clients
	 */
	private interface RedisClientAdapter {
		boolean tryLock(String lockKey, long time, TimeUnit unit) throws InterruptedException;
		void unlock(String lockKey);
		String get(String key);
		void set(String key, String value);
	}

	/**
	 * Redisson client adapter implementation
	 */
	private static class RedissonClientAdapter implements RedisClientAdapter {
		private final RedissonClient redisson;

		public RedissonClientAdapter(RedissonClient redisson) {
			this.redisson = redisson;
		}

		@Override
		public boolean tryLock(String lockKey, long time, TimeUnit unit) throws InterruptedException {
			RLock lock = redisson.getLock(lockKey);
			return lock.tryLock(time, unit);
		}

		@Override
		public void unlock(String lockKey) {
			RLock lock = redisson.getLock(lockKey);
			lock.unlock();
		}

		@Override
		public String get(String key) {
			RBucket<String> bucket = redisson.getBucket(key);
			return bucket.get();
		}

		@Override
		public void set(String key, String value) {
			RBucket<String> bucket = redisson.getBucket(key);
			bucket.set(value);
		}
	}

	/**
	 * Jedis client adapter implementation
	 */
	private static class JedisClientAdapter implements RedisClientAdapter {
		private final JedisPool jedisPool;

		public JedisClientAdapter(JedisPool jedisPool) {
			this.jedisPool = jedisPool;
		}

		@Override
		public boolean tryLock(String lockKey, long time, TimeUnit unit) throws InterruptedException {
			// Simple implementation - in production, consider using Redlock algorithm or similar
			// This is a simplified locking mechanism for demonstration purposes
			long timeoutMillis = unit.toMillis(time);
			long startTime = System.currentTimeMillis();
			
			try (Jedis jedis = jedisPool.getResource()) {
				while (System.currentTimeMillis() - startTime < timeoutMillis) {
					// Try to set the lock key with NX (only set if not exists) and PX (expire time in milliseconds)
					String result = jedis.set(lockKey, "locked", new SetParams().nx().px(10000));
					if ("OK".equals(result)) {
						return true;
					}
					// Wait a bit before retrying
					Thread.sleep(1);
				}
				return false;
			}
		}

		@Override
		public void unlock(String lockKey) {
			// In a real implementation, you might want to check if the lock belongs to this thread
			// For simplicity, we're just deleting the key
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.del(lockKey);
			}
		}

		@Override
		public String get(String key) {
			try (Jedis jedis = jedisPool.getResource()) {
				return jedis.get(key);
			}
		}

		@Override
		public void set(String key, String value) {
			try (Jedis jedis = jedisPool.getResource()) {
				jedis.set(key, value);
			}
		}
	}
}
