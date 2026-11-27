/*
 * Copyright 2024-2026 the original author or authors.
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
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.redisson.RedissonObject;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * The type Redis saver.
 * A durable execution keeps the Redis thread mapping alive for its lease. The
 * configured retention still applies to checkpoint content.
 *
 * @author disaster
 * @since 1.0.0-M2
 */
public class RedisSaver implements BaseCheckpointSaver {

	private static final Logger log = LoggerFactory.getLogger(RedisSaver.class);

	// Redis key prefixes
	private static final String CHECKPOINT_PREFIX = "graph:checkpoint:content:";
	private static final String THREAD_META_PREFIX = "graph:thread:meta:";
	private static final String THREAD_REVERSE_PREFIX = "graph:thread:reverse:";
	private static final String LOCK_PREFIX = "graph:checkpoint:lock:";
	private static final String EXECUTION_LEASE_SUFFIX = "graph:execution";
	private static final String EXECUTION_LEASE_KEY = "_REDIS_EXECUTION_LEASE_";
	private static final Duration EXECUTION_LEASE_DURATION = Duration.ofSeconds(90);
	private static final Duration EXECUTION_LEASE_RENEWAL = EXECUTION_LEASE_DURATION.dividedBy(3);
	private static final Duration LEASE_METADATA_DURATION = EXECUTION_LEASE_DURATION.plus(EXECUTION_LEASE_RENEWAL);
	private static final String CLAIM_LEASE = """
			return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) and 1 or 0
			""";
	private static final String COMMIT_WITH_LEASE = """
			if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
			local ttl = tonumber(ARGV[3])
			if ttl == 0 then
				redis.call('DEL', KEYS[2])
			elseif ttl > 0 then
				redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
			else
				redis.call('SET', KEYS[2], ARGV[2])
			end
			return 1
			""";
	private static final String RENEW_LEASE = """
			if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
			return redis.call('PEXPIRE', KEYS[1], ARGV[2])
			""";
	private static final String RELEASE_LEASE = """
			if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
			return redis.call('DEL', KEYS[1])
			""";
	// Thread meta hash field names
	private static final String FIELD_THREAD_ID = "thread_id";
	private static final String FIELD_IS_RELEASED = "is_released";
	private static final String FIELD_THREAD_NAME = "thread_name";
	private static final byte[] CHECKPOINT_FORMAT_MAGIC = { 'S', 'A', 'A', 'C', 1 };
	private final Serializer<Checkpoint> checkpointSerializer;
	private RedissonClient redisson;
	private final long ttl;
	private final TimeUnit ttlUnit;

	/**
	 * Protected constructor for RedisSaver.
	 * Use {@link #builder()} to create instances.
	 *
	 * @param redisson the redisson
	 * @param stateSerializer the state serializer
	 * @param ttl the time-to-live value for Redis keys, -1 means no expiration
	 * @param ttlUnit the time unit for ttl
	 */
	protected RedisSaver(RedissonClient redisson, StateSerializer stateSerializer, long ttl, TimeUnit ttlUnit) {
		requireNonNull(redisson, "redisson cannot be null");
		requireNonNull(stateSerializer, "stateSerializer cannot be null");
		this.redisson = redisson;
		this.checkpointSerializer = new CheckPointSerializer(stateSerializer);
		this.ttl = ttl;
		this.ttlUnit = ttlUnit;
	}

	/**
	 * Creates a new builder for RedisSaver.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ExecutionLease acquireExecutionLease(RunnableConfig config) throws InterruptedException {
		String threadName = config.threadId().orElse(THREAD_ID_DEFAULT);
		RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
		if (!lock.tryLock(3, TimeUnit.SECONDS)) {
			throw new IllegalStateException("Failed to acquire checkpoint lock for thread '" + threadName + "'");
		}
		RedisExecutionLease lease;
		try {
			var threadId = getOrCreateThreadId(threadName, LEASE_METADATA_DURATION);
			lease = new RedisExecutionLease(threadName, threadId);
			if (!lease.execute(CLAIM_LEASE, List.of(lease.key), ttlBytes(EXECUTION_LEASE_DURATION.toMillis()))) {
				throw new IllegalStateException("A durable graph execution is already active for thread '" + threadName + "'");
			}
			try {
				extendThreadMetadataForLease(threadName, threadId);
			}
			catch (RuntimeException failure) {
				try {
					lease.execute(RELEASE_LEASE, List.of(lease.key));
				}
				catch (RuntimeException cleanupFailure) {
					failure.addSuppressed(cleanupFailure);
				}
				throw failure;
			}
		}
		finally {
			lock.unlock();
		}
		lease.startRenewal();
		return lease;
	}

	private void extendThreadMetadataForLease(String threadName, String threadId) {
		if (ttl <= 0) {
			return;
		}
		for (String key : List.of(THREAD_META_PREFIX + threadName, THREAD_REVERSE_PREFIX + threadId)) {
			RMap<?, ?> metadata = redisson.getMap(key);
			long remaining = metadata.remainTimeToLive();
			if (remaining >= 0 && remaining < LEASE_METADATA_DURATION.toMillis()) {
				metadata.expire(LEASE_METADATA_DURATION);
			}
		}
	}

	private void restoreThreadMetadataTtl(String threadName, String threadId) {
		var duration = Duration.ofMillis(ttlUnit.toMillis(ttl));
		redisson.getMap(THREAD_META_PREFIX + threadName).expire(duration);
		redisson.getMap(THREAD_REVERSE_PREFIX + threadId).expire(duration);
	}

	private static byte[] ttlBytes(long millis) {
		return Long.toString(millis).getBytes(UTF_8);
	}

	private RedisExecutionLease executionLease(RunnableConfig config, String threadName) {
		return config.metadata(EXECUTION_LEASE_KEY).filter(RedisExecutionLease.class::isInstance)
				.map(RedisExecutionLease.class::cast).filter(lease -> lease.belongsTo(this, threadName)).orElse(null);
	}

	private final class RedisExecutionLease implements ExecutionLease {

		private final String threadName;
		private final String threadId;
		private final String contentKey;
		private final byte[] rawContentKey;
		private final byte[] key;
		private final byte[] token = UUID.randomUUID().toString().getBytes(UTF_8);
		private Disposable renewal;

		RedisExecutionLease(String threadName, String threadId) {
			this.threadName = threadName;
			this.threadId = threadId;
			this.contentKey = CHECKPOINT_PREFIX + threadId;
			var bucket = redisson.getBucket(contentKey);
			String rawName = bucket instanceof RedissonObject object ? object.getRawName() : contentKey;
			this.rawContentKey = rawName.getBytes(UTF_8);
			// Key bytes are already mapped. Redisson's suffix keeps both keys in
			// the same cluster slot without changing the existing content key.
			this.key = RedissonObject.suffixName(rawName, EXECUTION_LEASE_SUFFIX).getBytes(UTF_8);
		}

		private void startRenewal() {
			this.renewal = Flux.interval(EXECUTION_LEASE_RENEWAL).onBackpressureDrop()
					.concatMap(tick -> Mono.fromCallable(this::renew).subscribeOn(Schedulers.boundedElastic()))
					.takeWhile(Boolean::booleanValue)
					.subscribe(renewed -> { }, error -> log.warn("Redis graph execution lease renewal stopped", error));
		}

		private boolean belongsTo(RedisSaver saver, String thread) {
			return RedisSaver.this == saver && threadName.equals(thread);
		}

		@Override
		public RunnableConfig configure(RunnableConfig config) {
			return RunnableConfig.builder(config).addMetadata(EXECUTION_LEASE_KEY, this).build();
		}

		private boolean renew() {
			RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
			try {
				if (!lock.tryLock(3, TimeUnit.SECONDS)) {
					return true;
				}
				try {
					if (!threadId.equals(getActiveThreadId(threadName))) {
						return false;
					}
					extendThreadMetadataForLease(threadName, threadId);
					return renewToken();
				}
				finally {
					lock.unlock();
				}
			}
			catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return false;
			}
			catch (RuntimeException failure) {
				log.warn("Could not renew Redis graph execution lease for thread '{}'", threadName, failure);
				return true;
			}
		}

		private IllegalStateException expired() {
			return new IllegalStateException("The graph execution lease for thread '" + threadName + "' has expired or been replaced");
		}

		private boolean renewToken() {
			return execute(RENEW_LEASE, List.of(key), ttlBytes(EXECUTION_LEASE_DURATION.toMillis()));
		}

		private boolean execute(String script, List<Object> keys, Object... arguments) {
			var values = new Object[arguments.length + 1];
			values[0] = token;
			System.arraycopy(arguments, 0, values, 1, arguments.length);
			return redisson.getScript(ByteArrayCodec.INSTANCE).<Long>eval(contentKey, RScript.Mode.READ_WRITE,
					script, RScript.ReturnType.INTEGER, keys, values) == 1L;
		}

		private void commit(byte[] bytes) {
			long retention = ttl > 0 ? ttlUnit.toMillis(ttl) : -1;
			if (!execute(COMMIT_WITH_LEASE, List.of(key, rawContentKey), bytes, ttlBytes(retention))) {
				throw expired();
			}
		}

		@Override
		public void close() {
			renewal.dispose();
			try {
				if (ttl <= 0) {
					execute(RELEASE_LEASE, List.of(key));
					return;
				}
				RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
				if (!lock.tryLock(3, TimeUnit.SECONDS)) {
					throw new IllegalStateException("Failed to acquire checkpoint lock for thread '" + threadName + "'");
				}
				try {
					if (execute(RELEASE_LEASE, List.of(key)) && threadId.equals(getActiveThreadId(threadName))) {
						restoreThreadMetadataTtl(threadName, threadId);
					}
				}
				finally {
					lock.unlock();
				}
			}
			catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while releasing Redis graph execution lease for thread '{}'; it will expire",
						threadName, interrupted);
			}
			catch (RuntimeException failure) {
				log.warn("Could not release Redis graph execution lease for thread '{}'; it will expire", threadName,
						failure);
			}
		}

	}

	private byte[] serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(CHECKPOINT_FORMAT_MAGIC);
		try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
			 ObjectOutputStream oos = new ObjectOutputStream(gzip)) {
			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				checkpointSerializer.write(checkpoint, oos);
			}
		}
		return baos.toByteArray();
	}

	private LinkedList<Checkpoint> deserializeCheckpoints(String contentKey) throws IOException, ClassNotFoundException {
		RBucket<byte[]> binaryBucket = redisson.getBucket(contentKey, ByteArrayCodec.INSTANCE);
		byte[] content = binaryBucket.get();
		if (content == null || content.length == 0) {
			return new LinkedList<>();
		}
		if (hasVersionedHeader(content)) {
			try (ByteArrayInputStream bais = new ByteArrayInputStream(content, CHECKPOINT_FORMAT_MAGIC.length,
					content.length - CHECKPOINT_FORMAT_MAGIC.length);
				 GZIPInputStream gzip = new GZIPInputStream(bais);
				 ObjectInputStream ois = new ObjectInputStream(gzip)) {
				return readCheckpoints(ois);
			}
		}

		String legacyContent = redisson.<String>getBucket(contentKey).get();
		if (legacyContent == null || legacyContent.isEmpty()) {
			return new LinkedList<>();
		}
		byte[] legacyBytes = Base64.getDecoder().decode(legacyContent);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(legacyBytes);
			 ObjectInputStream ois = new ObjectInputStream(bais)) {
			return readCheckpoints(ois);
		}
	}

	private boolean hasVersionedHeader(byte[] content) {
		if (content.length < CHECKPOINT_FORMAT_MAGIC.length) {
			return false;
		}
		for (int i = 0; i < CHECKPOINT_FORMAT_MAGIC.length; i++) {
			if (content[i] != CHECKPOINT_FORMAT_MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	private LinkedList<Checkpoint> readCheckpoints(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			int size = ois.readInt();
			LinkedList<Checkpoint> checkpoints = new LinkedList<>();
			for (int i = 0; i < size; i++) {
				checkpoints.add(checkpointSerializer.read(ois));
			}
			return checkpoints;
	}

	/**
	 * Gets or creates a thread_id for the given thread_name.
	 * If an active thread exists, returns its thread_id.
	 * If no active thread exists or the thread is released, creates a new thread_id.
	 *
	 * @param threadName the thread name
	 * @return the thread_id (UUID string)
	 */
	private String getOrCreateThreadId(String threadName, Duration minRetention) {
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
		if (ttl > 0) {
			var retention = Duration.ofMillis(Math.max(ttlUnit.toMillis(ttl), minRetention.toMillis()));
			reverse.expire(retention);
			meta.expire(retention);
		}

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
			return deserializeCheckpoints(CHECKPOINT_PREFIX + threadId);

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
			LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(CHECKPOINT_PREFIX + threadId);

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

			var lease = executionLease(config, threadName);
			String threadId = lease == null ? getOrCreateThreadId(threadName, Duration.ZERO) : getActiveThreadId(threadName);
			if (lease != null && !lease.threadId.equals(threadId)) {
				throw lease.expired();
			}

			// Use thread_id as key for checkpoint storage
			String contentKey = CHECKPOINT_PREFIX + threadId;
			LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(contentKey);

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

			byte[] content = serializeCheckpoints(checkpoints);
			if (lease != null) {
				lease.commit(content);
			}
			else {
				RBucket<byte[]> bucket = redisson.getBucket(contentKey, ByteArrayCodec.INSTANCE);
				bucket.set(content);
				if (ttl > 0) {
					bucket.expire(Duration.ofMillis(ttlUnit.toMillis(ttl)));
				}
			}
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
			var lease = executionLease(config, threadName);
			if (lease != null && (!lease.threadId.equals(threadId) || !lease.renewToken())) {
				throw lease.expired();
			}

			// Mark thread as released
			meta.put(FIELD_IS_RELEASED, "true");

			// Update reverse mapping
			String reverseKey = THREAD_REVERSE_PREFIX + threadId;
			RMap<String, String> reverse = redisson.getMap(reverseKey);
			if (reverse != null) {
				reverse.put(FIELD_IS_RELEASED, "true");
			}
			if (lease != null && ttl > 0) {
				restoreThreadMetadataTtl(threadName, threadId);
			}

			// Get checkpoints for Tag (using thread_id)
			String contentKey = CHECKPOINT_PREFIX + threadId;
			Collection<Checkpoint> checkpoints = deserializeCheckpoints(contentKey);

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
		private long ttl = -1;
		private TimeUnit ttlUnit = TimeUnit.SECONDS;

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
		 * Sets the time-to-live (TTL) for all Redis keys managed by RedisSaver.
		 * When set, checkpoint data, thread metadata, and reverse mappings will
		 * automatically expire after the specified duration.
		 * <p>
		 * Default is -1 (no expiration), which preserves backward compatibility.
		 *
		 * @param ttl the time-to-live value, must be positive; -1 means no expiration
		 * @param ttlUnit the time unit for ttl, must not be null
		 * @return this builder
		 * @throws IllegalArgumentException if ttlUnit is null or ttl is 0 or less than -1
		 */
		public Builder ttl(long ttl, TimeUnit ttlUnit) {
			if (ttlUnit == null) {
				throw new IllegalArgumentException("ttlUnit cannot be null");
			}
			if (ttl == 0 || ttl < -1) {
				throw new IllegalArgumentException("ttl must be positive or -1 (no expiration), got: " + ttl);
			}
			this.ttl = ttl;
			this.ttlUnit = ttlUnit;
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
			return new RedisSaver(redisson, stateSerializer, ttl, ttlUnit);
		}
	}

}
