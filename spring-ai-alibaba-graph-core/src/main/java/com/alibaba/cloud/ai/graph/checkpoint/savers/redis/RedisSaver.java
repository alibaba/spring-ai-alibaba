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
package com.alibaba.cloud.ai.graph.checkpoint.savers.redis;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * The type Redis saver.
 *
 * @author disaster
 * @since 1.0.0-M2
 */
public class RedisSaver implements BaseCheckpointSaver {

	// Redis key prefixes
	private static final String CHECKPOINT_PREFIX = "graph:checkpoint:content:";
	private static final String THREAD_META_PREFIX = "graph:thread:meta:";
	private static final String THREAD_REVERSE_PREFIX = "graph:thread:reverse:";
	private static final String LOCK_PREFIX = "graph:checkpoint:lock:";
	// Thread meta hash field names
	private static final String FIELD_THREAD_ID = "thread_id";
	private static final String FIELD_IS_RELEASED = "is_released";
	private static final String FIELD_THREAD_NAME = "thread_name";
	private final Serializer<Checkpoint> checkpointSerializer;
	private RedissonClient redisson;

	/**
	 * Protected constructor for RedisSaver.
	 * Use {@link #builder()} to create instances.
	 *
	 * @param redisson the redisson
	 * @param stateSerializer the state serializer
	 */
	protected RedisSaver(RedissonClient redisson, StateSerializer stateSerializer) {
		requireNonNull(redisson, "redisson cannot be null");
		requireNonNull(stateSerializer, "stateSerializer cannot be null");
		this.redisson = redisson;
		this.checkpointSerializer = new CheckPointSerializer(stateSerializer);
	}

	/**
	 * Creates a new builder for RedisSaver.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	private String serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				checkpointSerializer.write(checkpoint, oos);
			}
			oos.flush();
			byte[] bytes = baos.toByteArray();
			return Base64.getEncoder().encodeToString(bytes);
		}
	}

	private LinkedList<Checkpoint> deserializeCheckpoints(String content) throws IOException, ClassNotFoundException {
		if (content == null || content.isEmpty()) {
			return new LinkedList<>();
		}
		byte[] bytes = Base64.getDecoder().decode(content);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			 ObjectInputStream ois = new ObjectInputStream(bais)) {
			int size = ois.readInt();
			LinkedList<Checkpoint> checkpoints = new LinkedList<>();
			for (int i = 0; i < size; i++) {
				checkpoints.add(checkpointSerializer.read(ois));
			}
			return checkpoints;
		}
	}

	/**
	 * Gets or creates a thread_id for the given thread_name.
	 * If an active thread exists, returns its thread_id.
	 * If no active thread exists or the thread is released, creates a new thread_id.
	 *
	 * @param threadName the thread name
	 * @return the thread_id (UUID string)
	 */
	private String getOrCreateThreadId(String threadName) {
		String metaKey = THREAD_META_PREFIX + threadName;
		RMap<String, String> meta = redisson.getMap(metaKey);

		// Check if an active thread exists
		String threadId = meta.get(FIELD_THREAD_ID);
		String isReleased = meta.get(FIELD_IS_RELEASED);

		if (threadId != null && !"true".equals(isReleased)) {
			// Active thread exists, return its thread_id
			return threadId;
		}

		// No active thread exists or thread is released, create a new thread_id
		String newThreadId = UUID.randomUUID().toString();
		meta.put(FIELD_THREAD_ID, newThreadId);
		meta.put(FIELD_IS_RELEASED, "false");

		// Set reverse mapping
		String reverseKey = THREAD_REVERSE_PREFIX + newThreadId;
		RMap<String, String> reverse = redisson.getMap(reverseKey);
		reverse.put(FIELD_THREAD_NAME, threadName);
		reverse.put(FIELD_IS_RELEASED, "false");

		return newThreadId;
	}

	/**
	 * Gets the active thread_id for the given thread_name.
	 * Returns null if no active thread exists.
	 *
	 * @param threadName the thread name
	 * @return the active thread_id, or null if not found
	 */
	private String getActiveThreadId(String threadName) {
		String metaKey = THREAD_META_PREFIX + threadName;
		RMap<String, String> meta = redisson.getMap(metaKey);

		String threadId = meta.get(FIELD_THREAD_ID);
		String isReleased = meta.get(FIELD_IS_RELEASED);

		if (threadId != null && !"true".equals(isReleased)) {
			return threadId;
		}

		return null; // No active thread exists
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allow null");
		}

		String threadName = threadNameOpt.get();
		RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
		boolean tryLock = false;
		try {
			// 500ms timeout for read operations (list)
			tryLock = lock.tryLock(500, TimeUnit.MILLISECONDS);
			if (!tryLock) {
				return List.of();
			}

			// Get active thread_id for the thread_name
			String threadId = getActiveThreadId(threadName);
			if (threadId == null) {
				return List.of();
			}

			// Use thread_id to query checkpoints
			RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
			String content = bucket.get();
			return deserializeCheckpoints(content);

		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize checkpoints", e);
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId isn't allow null");
		}

		String threadName = threadNameOpt.get();
		RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
		boolean tryLock = false;
		try {
			// 500ms timeout for read operations (get)
			tryLock = lock.tryLock(500, TimeUnit.MILLISECONDS);
			if (!tryLock) {
				return Optional.empty();
			}

			// Get active thread_id for the thread_name
			String threadId = getActiveThreadId(threadName);
			if (threadId == null) {
				return Optional.empty();
			}

			// Use thread_id to query checkpoints
			RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
			String content = bucket.get();
			LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);

			if (config.checkPointId().isPresent()) {
				return config.checkPointId()
						.flatMap(id -> checkpoints.stream()
								.filter(checkpoint -> checkpoint.getId().equals(id))
								.findFirst());
			}
			return getLast(checkpoints, config);

		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize checkpoints", e);
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId isn't allow null");
		}

		String threadName = threadNameOpt.get();
		RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
		boolean tryLock = false;
		try {
			// 3 seconds timeout for write operations (put) - longer timeout for concurrent scenarios
			tryLock = lock.tryLock(3, TimeUnit.SECONDS);
			if (!tryLock) {
				throw new RuntimeException("Failed to acquire lock for thread: " + threadName);
			}

			// Get or create thread_id
			String threadId = getOrCreateThreadId(threadName);

			// Use thread_id as key for checkpoint storage
			RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
			String content = bucket.get();
			LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);

			if (config.checkPointId().isPresent()) {
				// Replace Checkpoint
				String checkPointId = config.checkPointId().get();
				int index = IntStream.range(0, checkpoints.size())
						.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
						.findFirst()
						.orElseThrow(() -> new NoSuchElementException(
								format("Checkpoint with id %s not found!", checkPointId)));
				checkpoints.set(index, checkpoint);
			}
			else {
				// Add Checkpoint
				checkpoints.push(checkpoint);
			}

			bucket.set(serializeCheckpoints(checkpoints));
			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();

		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to serialize/deserialize checkpoints", e);
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	public Tag release(RunnableConfig config) throws Exception {
		Optional<String> threadNameOpt = config.threadId();
		if (!threadNameOpt.isPresent()) {
			throw new IllegalArgumentException("threadId is not allow null");
		}

		String threadName = threadNameOpt.get();
		RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
		boolean tryLock = false;
		try {
			// 3 seconds timeout for write operations (release) - longer timeout for concurrent scenarios
			tryLock = lock.tryLock(3, TimeUnit.SECONDS);
			if (!tryLock) {
				throw new RuntimeException("Failed to acquire lock for thread: " + threadName);
			}

			String metaKey = THREAD_META_PREFIX + threadName;
			RMap<String, String> meta = redisson.getMap(metaKey);

			String threadId = meta.get(FIELD_THREAD_ID);
			if (threadId == null) {
				throw new IllegalStateException("Thread not found: " + threadName);
			}

			// Mark thread as released
			meta.put(FIELD_IS_RELEASED, "true");

			// Update reverse mapping
			String reverseKey = THREAD_REVERSE_PREFIX + threadId;
			RMap<String, String> reverse = redisson.getMap(reverseKey);
			if (reverse != null) {
				reverse.put(FIELD_IS_RELEASED, "true");
			}

			// Get checkpoints for Tag (using thread_id)
			String contentKey = CHECKPOINT_PREFIX + threadId;
			RBucket<String> bucket = redisson.getBucket(contentKey);
			String content = bucket.get();
			Collection<Checkpoint> checkpoints = deserializeCheckpoints(content);

			return new Tag(threadName, checkpoints);

		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize checkpoints", e);
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * Builder class for RedisSaver.
	 */
	public static class Builder {
		private RedissonClient redisson;
		private StateSerializer stateSerializer;

		/**
		 * Sets the Redisson client.
		 *
		 * @param redisson the Redisson client
		 * @return this builder
		 */
		public Builder redisson(RedissonClient redisson) {
			this.redisson = redisson;
			return this;
		}

		/**
		 * Sets the state serializer.
		 *
		 * @param stateSerializer the state serializer
		 * @return this builder
		 */
		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		/**
		 * Builds a new RedisSaver instance.
		 * @return a new RedisSaver instance
		 * @throws IllegalArgumentException if redisson or stateSerializer is null
		 */
		public RedisSaver build() {
			if (redisson == null) {
				throw new IllegalArgumentException("redisson cannot be null");
			}
			if (stateSerializer == null) {
				this.stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
			}
			return new RedisSaver(redisson, stateSerializer);
		}
	}

}
