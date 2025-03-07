package com.alibaba.cloud.ai.graph.checkpoint.savers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.fastjson.JSON;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static java.lang.String.format;

/**
 * The type Redis saver.
 *
 * @author disaster
 * @since 1.0.0-M2
 */
public class RedisSaver implements BaseCheckpointSaver {

	private RedissonClient redisson;

	private static final String PREFIX = "graph:checkpoint:content:";

	private static final String LOCK_PREFIX = "graph:checkpoint:lock:";

	/**
	 * Instantiates a new Redis saver.
	 * @param redisson the redisson
	 */
	public RedisSaver(RedissonClient redisson) {
		this.redisson = redisson;
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			RLock lock = redisson.getLock(LOCK_PREFIX + configOption.get());
			boolean tryLock = false;
			try {
				tryLock = lock.tryLock(2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					RBucket<String> bucket = redisson.getBucket(PREFIX + configOption.get());
					// or CheckPointSerializer?
					return JSON.parseArray(bucket.get(), Checkpoint.class);
				}
				else {
					return List.of();
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {
				if (tryLock) {
					lock.unlock();
				}
			}
		}
		else {
			throw new IllegalArgumentException("threadId is not allow null");
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			RLock lock = redisson.getLock(LOCK_PREFIX + configOption.get());
			boolean tryLock = false;
			try {
				tryLock = lock.tryLock(2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					RBucket<String> bucket = redisson.getBucket(PREFIX + configOption.get());
					// or CheckPointSerializer?
					List<Checkpoint> checkpoints = JSON.parseArray(bucket.get(), Checkpoint.class);
					if (config.checkPointId().isPresent()) {
						return config.checkPointId()
							.flatMap(id -> checkpoints.stream()
								.filter(checkpoint -> checkpoint.getId().equals(id))
								.findFirst());
					}
					return getLast(getLinkedList(checkpoints), config);
				}
				else {
					return Optional.empty();
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {
				if (tryLock) {
					lock.unlock();
				}
			}
		}
		else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			RLock lock = redisson.getLock(LOCK_PREFIX + configOption.get());
			boolean tryLock = false;
			try {
				tryLock = lock.tryLock(2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					RBucket<String> bucket = redisson.getBucket(PREFIX + configOption.get());
					List<Checkpoint> checkpoints = JSON.parseArray(bucket.get(), Checkpoint.class);
					LinkedList<Checkpoint> linkedList = getLinkedList(checkpoints);
					if (config.checkPointId().isPresent()) { // Replace Checkpoint
						String checkPointId = config.checkPointId().get();
						int index = IntStream.range(0, checkpoints.size())
							.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
							.findFirst()
							.orElseThrow(() -> (new NoSuchElementException(
									format("Checkpoint with id %s not found!", checkPointId))));
						linkedList.set(index, checkpoint);
						bucket.set(JSON.toJSONString(linkedList));
						return config;
					}
					linkedList.push(checkpoint); // Add Checkpoint
					bucket.set(JSON.toJSONString(linkedList));
				}
				return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {
				if (tryLock) {
					lock.unlock();
				}
			}
		}
		else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

	@Override
	public boolean clear(RunnableConfig config) {
		Optional<String> configOption = config.threadId();
		if (configOption.isPresent()) {
			RLock lock = redisson.getLock(LOCK_PREFIX + configOption.get());
			boolean tryLock = false;
			try {
				tryLock = lock.tryLock(2, TimeUnit.MILLISECONDS);
				if (tryLock) {
					RBucket<String> bucket = redisson.getBucket(PREFIX + configOption.get());
					bucket.getAndSet(JSON.toJSONString(List.of()));
					return tryLock;
				}
				return false;
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {
				if (tryLock) {
					lock.unlock();
				}
			}
		}
		else {
			throw new IllegalArgumentException("threadId isn't allow null");
		}
	}

}
