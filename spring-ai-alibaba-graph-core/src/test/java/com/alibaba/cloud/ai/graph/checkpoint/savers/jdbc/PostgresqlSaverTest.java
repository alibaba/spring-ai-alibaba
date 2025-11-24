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
package com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for PostgresqlSaver using Testcontainers.
 *
 * @author yuluo-yx
 * @since 1.1.0.0-M4
 */
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
class PostgresqlSaverTest {

	private static boolean isCI() {
		return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
	}

	@Container
	private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
		.withDatabaseName("checkpoint_test")
		.withUsername("test")
		.withPassword("test");

	private static DataSource dataSource;

	private static PostgresqlSaver postgresqlSaver;

	@BeforeAll
	static void setup() {
		postgresContainer.start();

		// Create DataSource
		PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
		pgDataSource.setUrl(postgresContainer.getJdbcUrl());
		pgDataSource.setUser(postgresContainer.getUsername());
		pgDataSource.setPassword(postgresContainer.getPassword());

		dataSource = pgDataSource;
		postgresqlSaver = new PostgresqlSaver(dataSource);
	}

	@AfterAll
	static void tearDown() {
		postgresContainer.stop();
	}

	@Test
	void testPutAndGetAndList() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Construct checkpoints
		Checkpoint cp1 = Checkpoint.builder()
			.id("cp1")
			.state(Map.of("data", "data1"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		Checkpoint cp2 = Checkpoint.builder()
			.id("cp2")
			.state(Map.of("data", "data2"))
			.nodeId("node2")
			.nextNodeId("node3")
			.build();

		// Put first checkpoint
		RunnableConfig config1 = postgresqlSaver.put(config, cp1);
		assertNotNull(config1);
		assertEquals("cp1", config1.checkPointId().orElse(null));

		// Put second checkpoint
		RunnableConfig config2 = postgresqlSaver.put(config, cp2);
		assertNotNull(config2);
		assertEquals("cp2", config2.checkPointId().orElse(null));

		// List check - should have 2 checkpoints
		List<Checkpoint> list = (List<Checkpoint>) postgresqlSaver.list(config);
		assertEquals(2, list.size());
		assertEquals("cp2", list.get(0).getId()); // Latest is first (pushed to head)

		// Get latest checkpoint
		Optional<Checkpoint> latest = postgresqlSaver.get(config);
		assertTrue(latest.isPresent());
		assertEquals("cp2", latest.get().getId());
		assertEquals("data2", latest.get().getState().get("data"));

		// Get by specific checkpoint ID
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		Optional<Checkpoint> byId = postgresqlSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("cp1", byId.get().getId());
		assertEquals("data1", byId.get().getState().get("data"));
	}

	@Test
	void testReplaceCheckpoint() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Insert initial checkpoint
		Checkpoint cp1 = Checkpoint.builder()
			.id("cp1")
			.state(Map.of("data", "data1"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		postgresqlSaver.put(config, cp1);

		// Replace cp1 with new data
		Checkpoint cp1New = Checkpoint.builder()
			.id("cp1")
			.state(Map.of("data", "data1-updated"))
			.nodeId("node1")
			.nextNodeId("node3")
			.build();
		RunnableConfig configWithId = RunnableConfig.builder(config).checkPointId("cp1").build();
		RunnableConfig resultConfig = postgresqlSaver.put(configWithId, cp1New);

		// Verify the config returned is the same (not a new checkpoint ID)
		assertEquals(configWithId.checkPointId().orElse(null), resultConfig.checkPointId().orElse(null));

		// Verify the checkpoint was updated
		Optional<Checkpoint> byId = postgresqlSaver.get(configWithId);
		assertTrue(byId.isPresent());
		assertEquals("data1-updated", byId.get().getState().get("data"));
		assertEquals("node3", byId.get().getNextNodeId());
	}

	@Test
	void testClear() throws Exception {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Add some checkpoints
		postgresqlSaver.put(config,
				Checkpoint.builder()
					.id("cp1")
					.state(Map.of("data", "data1"))
					.nodeId("node1")
					.nextNodeId("node2")
					.build());
		postgresqlSaver.put(config,
				Checkpoint.builder()
					.id("cp2")
					.state(Map.of("data", "data2"))
					.nodeId("node2")
					.nextNodeId("node3")
					.build());

		// Verify checkpoints exist
		List<Checkpoint> listBefore = (List<Checkpoint>) postgresqlSaver.list(config);
		assertEquals(2, listBefore.size());

		// Clear checkpoints
		boolean cleared = postgresqlSaver.clear(config);
		assertTrue(cleared);

		// Verify checkpoints are cleared
		List<Checkpoint> listAfter = (List<Checkpoint>) postgresqlSaver.list(config);
		assertEquals(0, listAfter.size());
	}

	@Test
	void testGetWithNoData() {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Get from non-existent thread
		Optional<Checkpoint> result = postgresqlSaver.get(config);
		assertTrue(result.isEmpty());
	}

	@Test
	void testListWithNoData() {
		String threadId = "test-thread-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// List from non-existent thread
		List<Checkpoint> list = (List<Checkpoint>) postgresqlSaver.list(config);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	void testMultipleThreads() throws Exception {
		String threadId1 = "thread-1-" + UUID.randomUUID();
		String threadId2 = "thread-2-" + UUID.randomUUID();

		RunnableConfig config1 = RunnableConfig.builder().threadId(threadId1).build();
		RunnableConfig config2 = RunnableConfig.builder().threadId(threadId2).build();

		// Add checkpoint to thread 1
		Checkpoint cp1 = Checkpoint.builder()
			.id("cp1")
			.state(Map.of("thread", "1"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		postgresqlSaver.put(config1, cp1);

		// Add checkpoint to thread 2
		Checkpoint cp2 = Checkpoint.builder()
			.id("cp2")
			.state(Map.of("thread", "2"))
			.nodeId("node1")
			.nextNodeId("node2")
			.build();
		postgresqlSaver.put(config2, cp2);

		// Verify thread isolation
		Optional<Checkpoint> result1 = postgresqlSaver.get(config1);
		assertTrue(result1.isPresent());
		assertEquals("1", result1.get().getState().get("thread"));

		Optional<Checkpoint> result2 = postgresqlSaver.get(config2);
		assertTrue(result2.isPresent());
		assertEquals("2", result2.get().getState().get("thread"));
	}

	@Test
	void testClearNonExistentThread() {
		String threadId = "non-existent-" + UUID.randomUUID();
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		// Clear non-existent thread should return false
		boolean cleared = postgresqlSaver.clear(config);
		assertFalse(cleared);
	}

}
