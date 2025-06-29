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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class RedisSaverTest {

	// 使用较为稳定的版本

	static RedissonClient redisson;
	static RedisSaver redisSaver;

	@Container
	private static final GenericContainer<?> redisContainer = new GenericContainer<>(
			DockerImageName.parse("valkey/valkey:8.1.2"))
		.withExposedPorts(6379); // #gitleaks:allow

	@BeforeAll
	static void setup() {
		redisContainer.start();
		// 本地单机 Redis，测试环境需保证 6379 端口可用
		Config config = new Config();
		config.useSingleServer()
			.setAddress("redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379));
		redisson = Redisson.create(config);
		redisSaver = new RedisSaver(redisson);
	}

	@AfterAll
	static void tearDown() {
		if (redisson != null) {
			redisson.shutdown();
		}
	}

	@Test
	void testPutAndGetAndList() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// 构造 checkpoint
		Checkpoint cp1 = Checkpoint.builder()
			.id("cp1")
			.state(java.util.Map.of("data", "data1"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		Checkpoint cp2 = Checkpoint.builder()
			.id("cp2")
			.state(java.util.Map.of("data", "data2"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();

		// put 第一个
		redisSaver.put(config, cp1);
		// put 第二个
		redisSaver.put(config, cp2);

		// list 检查
		List<Checkpoint> list = (List<Checkpoint>) redisSaver.list(config);
		assertEquals(2, list.size());
		assertEquals("cp2", list.get(0).getId()); // push 到头部

		// get 最新
		Optional<Checkpoint> latest = redisSaver.get(config);
		assertTrue(latest.isPresent());
		assertEquals("cp2", latest.get().getId());

		// get by id
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		Optional<Checkpoint> byId = redisSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("cp1", byId.get().getId());
	}

	@Test
	void testReplaceCheckpoint() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Checkpoint cp1 = Checkpoint.builder()
			.id("cp1")
			.state(java.util.Map.of("data", "data1"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		redisSaver.put(config, cp1);

		// 替换 cp1
		Checkpoint cp1New = Checkpoint.builder()
			.id("cp1")
			.state(java.util.Map.of("data", "data1-new"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		redisSaver.put(configWithId, cp1New);

		Optional<Checkpoint> byId = redisSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("data1-new", byId.get().getState().get("data"));
	}

	@Test
	void testClear() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		redisSaver.put(config,
				Checkpoint.builder()
					.id("cp1")
					.state(java.util.Map.of("data", "data1"))
					.nodeId("node1")
					.nextNodeId("node2")
					.build());
		redisSaver.put(config,
				Checkpoint.builder()
					.id("cp2")
					.state(java.util.Map.of("data", "data2"))
					.nodeId("node1")
					.nextNodeId("node2")
					.build());

		boolean cleared = redisSaver.clear(config);
		assertTrue(cleared);

		List<Checkpoint> list = (List<Checkpoint>) redisSaver.list(config);
		assertEquals(0, list.size());
	}

	@Test
	void testGetWithNoData() {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		Optional<Checkpoint> result = redisSaver.get(config);
		assertTrue(result.isEmpty());
	}

	@Test
	public void concurrentExceptionTest() throws Exception {
		ExecutorService executorService = Executors.newCachedThreadPool();
		int count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		var index = new AtomicInteger(0);
		var futures = new ArrayList<Future<?>>();

		for (int i = 0; i < count; i++) {

			var future = executorService.submit(() -> {
				try {

					var threadName = format("thread-%d", index.incrementAndGet());
					System.out.println(threadName);
					redisSaver.list(RunnableConfig.builder().threadId(threadName).build());

				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					latch.countDown();
				}
			});

			futures.add(future);
		}

		latch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();

		for (var future : futures) {

			assertTrue(future.isDone());
			assertNull(future.get());
		}

		// int size = redisSaver.get_checkpointsByThread().size();
		// size must be equals to count

		// assertEquals(count, size, "Checkpoint Lost during concurrency");
	}

}
