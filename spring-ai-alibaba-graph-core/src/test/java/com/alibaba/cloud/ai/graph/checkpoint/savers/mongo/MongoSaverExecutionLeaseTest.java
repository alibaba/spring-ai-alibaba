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
package com.alibaba.cloud.ai.graph.checkpoint.savers.mongo;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfDockerAvailable
@EnabledIf("isCI")
class MongoSaverExecutionLeaseTest {

	static boolean isCI() {
		return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
	}

	@Test
	void aSuccessorCanTakeOverWithoutAcceptingStaleCheckpointWrites() throws Exception {
		try (var mongo = new GenericContainer<>(DockerImageName.parse("mongo:7.0")).withExposedPorts(27017)
				.withCommand("mongod", "--replSet", "rs0", "--bind_ip_all")) {
			mongo.start();
			var init = mongo.execInContainer("mongosh", "--quiet", "--eval",
					"rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'localhost:27017'}]})");
			assertEquals(0, init.getExitCode(), init.getStderr());
			var url = "mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017)
					+ "/?directConnection=true&replicaSet=rs0";
			try (var clientA = MongoClients.create(url); var clientB = MongoClients.create(url)) {
				var first = MongoSaver.builder().client(clientA).build();
				var second = MongoSaver.builder().client(clientB).build();
				var config = RunnableConfig.builder().threadId("mongo-lease-test").build();
				try (var original = first.acquireExecutionLease(config)) {
					var stale = original.configure(config);
					first.put(stale, checkpoint("original"));
					assertThrows(IllegalStateException.class, () -> second.acquireExecutionLease(config));
					var leaseCollection = clientB.getDatabase("check_point_db").getCollection("execution_lease");
					leaseCollection.updateOne(Filters.eq("_id", "mongo-lease-test"), Updates.set("expiresAt", new Date(0)));
					try (var successor = second.acquireExecutionLease(config)) {
						second.put(successor.configure(config), checkpoint("successor"));
						var failure = assertThrows(RuntimeException.class, () -> first.put(stale, checkpoint("stale")));
						assertInstanceOf(IllegalStateException.class, failure.getCause());
						original.close();
						second.put(successor.configure(config), checkpoint("still active"));
					}
				}
				assertEquals("still active", first.get(config).orElseThrow().getState().get("value"));
				assertEquals(3, first.list(config).size());
			}
		}
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder().nodeId("work").nextNodeId(END).state(Map.of("value", value)).build();
	}

}
