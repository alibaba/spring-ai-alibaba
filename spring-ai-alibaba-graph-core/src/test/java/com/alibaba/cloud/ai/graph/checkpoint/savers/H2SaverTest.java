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
package com.alibaba.cloud.ai.graph.checkpoint.savers;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.h2.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.h2.H2Saver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2SaverTest {

	@Test
	void shouldPersistAndReloadCheckpointsFromH2() throws Exception {
		DataSource dataSource = dataSource();
		var saver = saver(dataSource, CreateOption.CREATE_OR_REPLACE);
		String threadId = "h2-persist-thread";
		var firstCheckpoint = checkpoint("first");
		var secondCheckpoint = checkpoint("second");
		var thirdCheckpoint = checkpoint("third");

		saver.put(config(threadId), firstCheckpoint);
		saver.put(config(threadId), secondCheckpoint);
		saver.put(config(threadId), thirdCheckpoint);

		Collection<Checkpoint> history = saver.list(config(threadId));
		assertEquals(3, history.size());
		assertEquals(thirdCheckpoint.getId(), history.iterator().next().getId());
		assertEquals(firstCheckpoint.getId(), saver.get(config(threadId, firstCheckpoint.getId())).orElseThrow().getId());

		var reloadedSaver = saver(dataSource, CreateOption.CREATE_IF_NOT_EXISTS);
		assertEquals(thirdCheckpoint.getId(), reloadedSaver.get(config(threadId)).orElseThrow().getId());
	}

	@Test
	void shouldReleaseThreadAndAllowNewActiveThreadWithSameName() throws Exception {
		DataSource dataSource = dataSource();
		var saver = saver(dataSource, CreateOption.CREATE_OR_REPLACE);
		String threadId = "h2-release-thread";
		var releasedCheckpoint = checkpoint("released");
		var newCheckpoint = checkpoint("new");

		saver.put(config(threadId), releasedCheckpoint);
		var released = saver.release(config(threadId));

		assertEquals(threadId, released.threadId());
		assertEquals(1, released.checkpoints().size());
		assertTrue(saver.get(config(threadId)).isEmpty());

		saver.put(config(threadId), newCheckpoint);

		assertEquals(newCheckpoint.getId(), saver.get(config(threadId)).orElseThrow().getId());
		assertEquals(1, saver.list(config(threadId)).size());
		assertEquals(2, rowCount(dataSource, "GRAPH_THREAD"));
	}

	@Test
	void shouldRetainOnlyConfiguredNumberOfCheckpoints() throws Exception {
		DataSource dataSource = dataSource();
		var saver = saver(dataSource, CreateOption.CREATE_OR_REPLACE);
		String threadId = "h2-retention-thread";
		var config = RunnableConfig.builder()
				.threadId(threadId)
				.checkpointsNumRetained(2)
				.build();
		var firstCheckpoint = checkpoint("first");
		var secondCheckpoint = checkpoint("second");
		var thirdCheckpoint = checkpoint("third");

		saver.put(config, firstCheckpoint);
		saver.put(config, secondCheckpoint);
		saver.put(config, thirdCheckpoint);

		Collection<Checkpoint> history = saver.list(config);
		assertEquals(2, history.size());
		assertEquals(thirdCheckpoint.getId(), history.iterator().next().getId());
		assertTrue(saver.get(config(threadId, firstCheckpoint.getId())).isEmpty());
		assertEquals(2, rowCount(dataSource, "GRAPH_CHECKPOINT"));
	}

	@Test
	void shouldRejectNegativeMaxCachedThreads() {
		var builder = H2Saver.builder()
				.dataSource(dataSource())
				.stateSerializer(StateGraph.DEFAULT_JACKSON_SERIALIZER);

		var exception = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> builder.maxCachedThreads(-1));
		assertEquals("maxCachedThreads must be greater than or equal to 0", exception.getMessage());
	}

	private static H2Saver saver(DataSource dataSource, CreateOption createOption) {
		return H2Saver.builder()
				.dataSource(dataSource)
				.stateSerializer(StateGraph.DEFAULT_JACKSON_SERIALIZER)
				.createOption(createOption)
				.build();
	}

	private static DataSource dataSource() {
		var dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:h2_saver_" + UUID.randomUUID()
				+ ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
		dataSource.setUser("sa");
		dataSource.setPassword("");
		return dataSource;
	}

	private static RunnableConfig config(String threadId) {
		return RunnableConfig.builder().threadId(threadId).build();
	}

	private static RunnableConfig config(String threadId, String checkpointId) {
		return RunnableConfig.builder().threadId(threadId).checkPointId(checkpointId).build();
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder()
				.nodeId("agent_1")
				.nextNodeId(END)
				.state(Map.of("value", value))
				.build();
	}

	private static int rowCount(DataSource dataSource, String tableName) throws SQLException {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
				var resultSet = statement.executeQuery()) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}

}
