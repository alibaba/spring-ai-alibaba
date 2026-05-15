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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;


import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
public class MysqlSaverTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    protected static final String MYSQL_IMAGE_NAME = "mysql:8.0";
    protected static MysqlDataSource DATA_SOURCE;

    protected static MySQLContainer<?> mysqlContainer;

    static KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("agent_1:prop1", (o, o2) -> o2);
        return keyStrategyMap;
    };

    @BeforeAll
    public static void setup() {
        try {
            DATA_SOURCE = new MysqlDataSource();
            String urlFromEnv = System.getenv("MYSQL_JDBC_URL");

            if (urlFromEnv == null) {
                @SuppressWarnings("resource")
                MySQLContainer<?> container = new MySQLContainer<>(MYSQL_IMAGE_NAME)
                        .withDatabaseName("testdb")
                        .withUsername("testuser")
                        .withPassword("testpwd");
                container.start();
                mysqlContainer = container;

                initDataSource(
                        DATA_SOURCE,
                        mysqlContainer.getJdbcUrl(),
                        mysqlContainer.getUsername(),
                        mysqlContainer.getPassword());

            } else {
                initDataSource(
                        DATA_SOURCE,
                        urlFromEnv,
                        System.getenv("MYSQL_JDBC_USER"),
                        System.getenv("MYSQL_JDBC_PASSWORD"));
            }

        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

    }

    @AfterAll
    public static void tearDown() {
        if (mysqlContainer != null) {
            mysqlContainer.close();
        }
    }

    static void initDataSource(MysqlDataSource dataSource, String url, String username, String password) {
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
    }

    private static RunnableConfig config(String threadId) {
        return RunnableConfig.builder().threadId(threadId).build();
    }

    private static RunnableConfig config(String threadId, String checkpointId) {
        return RunnableConfig.builder().threadId(threadId).checkPointId(checkpointId).build();
    }

    private static Checkpoint checkpoint(String value) {
        return checkpoint(null, value);
    }

    private static Checkpoint checkpoint(String id, String value) {
        Checkpoint.Builder builder = Checkpoint.builder()
                .nodeId("agent_1")
                .nextNodeId(END)
                .state(Map.of("value", value));
        if (id != null) {
            builder.id(id);
        }
        return builder.build();
    }

    private static void forceSameSavedAt(String threadId) throws SQLException {
        try (Connection connection = DATA_SOURCE.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE GRAPH_CHECKPOINT c
                        INNER JOIN GRAPH_THREAD t ON c.thread_id = t.thread_id
                        SET c.saved_at = TIMESTAMP('2026-01-01 00:00:00')
                        WHERE t.thread_name = ? AND t.is_released != TRUE
                        """)) {
            statement.setString(1, threadId);
            assertEquals(2, statement.executeUpdate());
        }
    }

    private static String firstCheckpointId() {
        return "00000000-0000-0000-0000-000000000001";
    }

    private static String secondCheckpointId() {
        return "00000000-0000-0000-0000-000000000002";
    }

    @Test
    public void testMysqlSaverOrdersCheckpointsByInsertSequenceWhenSavedAtTies() throws Exception {
        var saver = MysqlSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(DATA_SOURCE)
                .maxCachedThreads(0)
                .build();

        String threadId = "mysql-checkpoint-sequence-thread";
        var firstCheckpoint = checkpoint(firstCheckpointId(), "first");
        var secondCheckpoint = checkpoint(secondCheckpointId(), "second");

        saver.put(config(threadId), firstCheckpoint);
        saver.put(config(threadId), secondCheckpoint);
        forceSameSavedAt(threadId);

        Collection<Checkpoint> history = saver.list(config(threadId));
        assertEquals(2, history.size());
        assertEquals(secondCheckpoint.getId(), history.iterator().next().getId());

        var latest = saver.get(config(threadId));
        assertTrue(latest.isPresent());
        assertEquals(secondCheckpoint.getId(), latest.get().getId());
    }

    @Test
    public void testCheckpointWithReleasedThread() throws Exception {

        var saver = MysqlSaver.builder()
                .dataSource(DATA_SOURCE)
                .build();

        NodeAction agent_1 = state ->
             Map.of("agent_1:prop1", "agent_1:test");


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
        var saver = MysqlSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(DATA_SOURCE)
                .build();

        NodeAction agent_1 = state ->
            Map.of("agent_1:prop1", "agent_1:test");


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

        // UPDATE STATE
        final var updatedConfig = workflow.updateState(lastSnapshot.get().config(), Map.of("update", "update test"));

        var updatedSnapshot = workflow.stateOf(updatedConfig);
        assertTrue(updatedSnapshot.isPresent());
        assertEquals("agent_1", updatedSnapshot.get().node());
        assertTrue(updatedSnapshot.get().state().value("update").isPresent());
        assertEquals("update test", updatedSnapshot.get().state().value("update").get());
        assertEquals(END, lastSnapshot.get().next());

        // test checkpoints reloading from database
        saver = MysqlSaver.builder()
                .dataSource(DATA_SOURCE)
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
        // lastSnapshot = workflow.lastStateOf( runnableConfig );

        assertTrue(lastSnapshot.isPresent());
        assertEquals("agent_1", lastSnapshot.get().node());
        assertEquals(END, lastSnapshot.get().next());
        assertTrue(lastSnapshot.get().state().value("update").isPresent());
        assertEquals("update test", lastSnapshot.get().state().value("update").get());
        assertEquals(END, lastSnapshot.get().next());

        saver.release(runnableConfig);

    }

    @Test
    public void testLatestCheckpointCacheIsBoundedByThreadCount() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = MysqlSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(2)
                .build();

        var firstCheckpoint = checkpoint("first");
        var firstConfig = config("mysql-cache-thread-1");
        saver.put(firstConfig, firstCheckpoint);
        saver.put(config("mysql-cache-thread-2"), checkpoint("second"));
        saver.put(config("mysql-cache-thread-3"), checkpoint("third"));

        countingDataSource.reset();
        var reloaded = saver.get(firstConfig);
        assertTrue(reloaded.isPresent());
        assertEquals(firstCheckpoint.getId(), reloaded.get().getId());
        assertEquals(1, countingDataSource.latestCheckpointSelects());
    }

    @Test
    public void testMysqlSaverKeepsOnlyLatestCheckpointInMemory() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = MysqlSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(16)
                .build();

        String threadId = "mysql-cache-single-thread";
        var firstCheckpoint = checkpoint("first");
        var secondCheckpoint = checkpoint("second");
        var thirdCheckpoint = checkpoint("third");

        saver.put(config(threadId), firstCheckpoint);
        saver.put(config(threadId), secondCheckpoint);
        saver.put(config(threadId), thirdCheckpoint);

        countingDataSource.reset();
        var latest = saver.get(config(threadId));
        assertTrue(latest.isPresent());
        assertEquals(thirdCheckpoint.getId(), latest.get().getId());
        assertEquals(0, countingDataSource.latestCheckpointSelects());

        Collection<Checkpoint> history = saver.list(config(threadId));
        assertEquals(3, history.size());

        countingDataSource.reset();
        var firstFromDatabase = saver.get(config(threadId, firstCheckpoint.getId()));
        assertTrue(firstFromDatabase.isPresent());
        assertEquals(firstCheckpoint.getId(), firstFromDatabase.get().getId());
        assertEquals(1, countingDataSource.checkpointByIdSelects());
    }

    private static final class CountingDataSource implements DataSource {

        private final DataSource delegate;

        private final AtomicInteger latestCheckpointSelects = new AtomicInteger();

        private final AtomicInteger checkpointByIdSelects = new AtomicInteger();

        private CountingDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        void reset() {
            latestCheckpointSelects.set(0);
            checkpointByIdSelects.set(0);
        }

        int latestCheckpointSelects() {
            return latestCheckpointSelects.get();
        }

        int checkpointByIdSelects() {
            return checkpointByIdSelects.get();
        }

        @Override
        public Connection getConnection() throws SQLException {
            return countPreparedStatements(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return countPreparedStatements(delegate.getConnection(username, password));
        }

        private Connection countPreparedStatements(Connection connection) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
                    (proxy, method, args) -> {
                        countQuery(method, args);
                        try {
                            return method.invoke(connection, args);
                        }
                        catch (InvocationTargetException ex) {
                            throw ex.getCause();
                        }
                    });
        }

        private void countQuery(java.lang.reflect.Method method, Object[] args) {
            if (!"prepareStatement".equals(method.getName()) || args == null || args.length == 0
                    || !(args[0] instanceof String sql)) {
                return;
            }
            if (sql.contains("LIMIT 1")) {
                latestCheckpointSelects.incrementAndGet();
            }
            if (sql.contains("AND c.checkpoint_id = ?")) {
                checkpointByIdSelects.incrementAndGet();
            }
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }

}
