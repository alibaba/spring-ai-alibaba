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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mongo.MongoSaver;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
public class MongoSaverTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    protected static final String MONGODB_IMAGE_NAME = "mongo:7.0";
    protected static MongoClient MONGO_CLIENT;
    protected static GenericContainer<?> mongoDBContainer;

    static KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("agent_1:prop1", (o, o2) -> o2);
        return keyStrategyMap;
    };

    @BeforeAll
    public static void setup() {
        String urlFromEnv = System.getenv("MONGODB_CONNECTION_STRING");

        if (urlFromEnv == null) {
            mongoDBContainer = new GenericContainer<>(DockerImageName.parse(MONGODB_IMAGE_NAME))
                    .withExposedPorts(27017);
            mongoDBContainer.start();

            String connectionString = String.format("mongodb://%s:%d",
                    mongoDBContainer.getHost(),
                    mongoDBContainer.getMappedPort(27017));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .build();
            MONGO_CLIENT = MongoClients.create(settings);
        } else {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(urlFromEnv))
                    .build();
            MONGO_CLIENT = MongoClients.create(settings);
        }
    }

    @AfterAll
    public static void tearDown() {
        if (MONGO_CLIENT != null) {
            MONGO_CLIENT.close();
        }
        if (mongoDBContainer != null) {
            mongoDBContainer.close();
        }
    }

    @Test
    public void testCheckpointWithReleasedThread() throws Exception {
        var saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .build();

        NodeAction agent_1 = state -> Map.of("agent_1:prop1", "agent_1:test");

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async(agent_1))
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(true)
                .build();

        var runnableConfig = RunnableConfig.builder()
                .build();
        var workflow = graph.compile(compileConfig);

        Map<String, Object> inputs = Map.of("input", "test1");

        var result = workflow.invoke(inputs, runnableConfig);

        assertTrue(result.isPresent());

        var history = workflow.getStateHistory(runnableConfig);

        assertTrue(history.isEmpty());
    }

    @Test
    public void testCheckpointWithNotReleasedThread() throws Exception {
        var saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .build();

        NodeAction agent_1 = state -> Map.of("agent_1:prop1", "agent_1:test");

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async(agent_1))
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile(compileConfig);

        Map<String, Object> inputs = Map.of("input", "test1");

        var result = workflow.invoke(inputs, runnableConfig);

        assertTrue(result.isPresent());

        var history = workflow.getStateHistory(runnableConfig);

        assertFalse(history.isEmpty());
        assertEquals(2, history.size());

        var lastSnapshot = workflow.lastStateOf(runnableConfig);

        assertTrue(lastSnapshot.isPresent());
        assertEquals("agent_1", lastSnapshot.get().node());
        assertEquals(END, lastSnapshot.get().next());

        final var updatedConfig = workflow.updateState(lastSnapshot.get().config(), Map.of("update", "update test"));

        var updatedSnapshot = workflow.stateOf(updatedConfig);
        assertTrue(updatedSnapshot.isPresent());
        assertEquals("agent_1", updatedSnapshot.get().node());
        assertTrue(updatedSnapshot.get().state().value("update").isPresent());
        assertEquals("update test", updatedSnapshot.get().state().value("update").get());
        assertEquals(END, lastSnapshot.get().next());

        saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .build();

        compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        runnableConfig = RunnableConfig.builder().build();
        workflow = graph.compile(compileConfig);

        history = workflow.getStateHistory(runnableConfig);

        assertFalse(history.isEmpty());
        assertEquals(2, history.size());

        lastSnapshot = workflow.stateOf(updatedConfig);

        assertTrue(lastSnapshot.isPresent());
        assertEquals("agent_1", lastSnapshot.get().node());
        assertEquals(END, lastSnapshot.get().next());
        assertTrue(lastSnapshot.get().state().value("update").isPresent());
        assertEquals("update test", lastSnapshot.get().state().value("update").get());
        assertEquals(END, lastSnapshot.get().next());

        saver.release(runnableConfig);
    }

    @Test
    public void testInsertMode() throws Exception {
        var saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .overwriteMode(false)
                .build();

        NodeAction agent_1 = state -> Map.of("agent_1:prop1", "agent_1:test");

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async(agent_1))
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile(compileConfig);

        Map<String, Object> inputs1 = Map.of("input", "test1");
        var result1 = workflow.invoke(inputs1, runnableConfig);
        assertTrue(result1.isPresent());

        Map<String, Object> inputs2 = Map.of("input", "test2");
        var result2 = workflow.invoke(inputs2, runnableConfig);
        assertTrue(result2.isPresent());

        var history = workflow.getStateHistory(runnableConfig);
        assertFalse(history.isEmpty());
        assertEquals(4, history.size(), "插入模式应该保留所有历史记录");

        saver.release(runnableConfig);
    }

    @Test
    public void testOverwriteMode() throws Exception {
        var saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .overwriteMode(true)
                .build();

        NodeAction agent_1 = state -> Map.of("agent_1:prop1", "agent_1:test_overwrite");

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async(agent_1))
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile(compileConfig);

        Map<String, Object> inputs1 = Map.of("input", "test1");
        var result1 = workflow.invoke(inputs1, runnableConfig);
        assertTrue(result1.isPresent());

        Map<String, Object> inputs2 = Map.of("input", "test2");
        var result2 = workflow.invoke(inputs2, runnableConfig);
        assertTrue(result2.isPresent());

        Map<String, Object> inputs3 = Map.of("input", "test3");
        var result3 = workflow.invoke(inputs3, runnableConfig);
        assertTrue(result3.isPresent());

        var history = workflow.getStateHistory(runnableConfig);
        assertFalse(history.isEmpty());
        assertEquals(2, history.size(), "覆盖模式应该只保留最新执行的记录");

        var lastSnapshot = workflow.lastStateOf(runnableConfig);
        assertTrue(lastSnapshot.isPresent());
        assertEquals("agent_1", lastSnapshot.get().node());
        assertEquals("agent_1:test_overwrite", lastSnapshot.get().state().value("agent_1:prop1").orElse(null));

        saver.release(runnableConfig);
    }

    @Test
    public void testOverwriteModeDataConsistency() throws Exception {
        var saver = MongoSaver.builder()
                .client(MONGO_CLIENT)
                .overwriteMode(true)
                .build();

        NodeAction agent_1 = state -> {
            Object input = state.data().get("input");
            return Map.of("agent_1:prop1", "processed_" + input);
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async(agent_1))
                .addEdge(START, "agent_1")
                .addEdge("agent_1", END);

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile(compileConfig);

        String[] inputs = {"data1", "data2", "data3", "data4", "data5"};
        for (String input : inputs) {
            var result = workflow.invoke(Map.of("input", input), runnableConfig);
            assertTrue(result.isPresent());

            var lastSnapshot = workflow.lastStateOf(runnableConfig);
            assertTrue(lastSnapshot.isPresent());
            assertEquals("processed_" + input,
                    lastSnapshot.get().state().value("agent_1:prop1").orElse(null),
                    "应该能读取到最新覆盖的数据");
        }

        var history = workflow.getStateHistory(runnableConfig);
        assertEquals(2, history.size(), "覆盖模式应该只保留最新执行的记录");

        saver.release(runnableConfig);
    }
}
