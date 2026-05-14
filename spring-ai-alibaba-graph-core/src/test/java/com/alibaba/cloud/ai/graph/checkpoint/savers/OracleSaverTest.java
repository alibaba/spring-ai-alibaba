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
import com.alibaba.cloud.ai.graph.checkpoint.savers.oracle.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.oracle.OracleSaver;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.datasource.OracleDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
public class OracleSaverTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    protected static final String ORACLE_IMAGE_NAME = "gvenzl/oracle-free:23.7-slim-faststart";
    protected static OracleDataSource DATA_SOURCE;
    protected static OracleDataSource SYSDBA_DATA_SOURCE;

    protected static OracleContainer oracleContainer;

    static KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("agent_1:prop1", (o, o2) -> o2);
        return keyStrategyMap;
    };

    @BeforeAll
    public static void setup() throws IOException {
        try {
            DATA_SOURCE = new oracle.jdbc.datasource.impl.OracleDataSource();
            SYSDBA_DATA_SOURCE = new oracle.jdbc.datasource.impl.OracleDataSource();
            String urlFromEnv = System.getenv("ORACLE_JDBC_URL");

            if (urlFromEnv == null) {
                // The Ryuk component is relied upon to stop this container.
                oracleContainer = new OracleContainer(ORACLE_IMAGE_NAME)
                        .withStartupTimeout(Duration.ofSeconds(600))
                        .withConnectTimeoutSeconds(600)
                        .withDatabaseName("pdb1")
                        .withUsername("testuser")
                        .withPassword("testpwd");
                oracleContainer.start();

                initDataSource(
                        DATA_SOURCE,
                        oracleContainer.getJdbcUrl(),
                        oracleContainer.getUsername(),
                        oracleContainer.getPassword());
                initDataSource(SYSDBA_DATA_SOURCE, oracleContainer.getJdbcUrl(), "sys", oracleContainer.getPassword());

            } else {
                initDataSource(
                        DATA_SOURCE,
                        urlFromEnv,
                        System.getenv("ORACLE_JDBC_USER"),
                        System.getenv("ORACLE_JDBC_PASSWORD"));
                initDataSource(
                        SYSDBA_DATA_SOURCE,
                        urlFromEnv,
                        System.getenv("ORACLE_JDBC_USER"),
                        System.getenv("ORACLE_JDBC_PASSWORD"));
            }
            SYSDBA_DATA_SOURCE.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_INTERNAL_LOGON, "SYSDBA");

        } catch (SQLException sqlException) {
            throw new AssertionError(sqlException);
        }

    }

    @AfterAll
    public static void tearDown() {
        if (oracleContainer != null) {
            oracleContainer.close();
        }
    }

    static void initDataSource(OracleDataSource dataSource, String url, String username, String password)
            throws SQLException {
        dataSource.setURL(url + "?oracle.jdbc.provider.json=jackson-json-provider");
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
        return Checkpoint.builder()
                .nodeId("agent_1")
                .nextNodeId(END)
                .state(Map.of("value", value))
                .build();
    }

    @Test
    public void testCheckpointWithReleasedThread() throws Exception {

        var saver = OracleSaver.builder()
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
        var saver = OracleSaver.builder()
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
        saver = OracleSaver.builder()
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
        var saver = OracleSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(2)
                .build();

        var firstCheckpoint = checkpoint("first");
        var firstConfig = config("oracle-cache-thread-1");
        saver.put(firstConfig, firstCheckpoint);
        saver.put(config("oracle-cache-thread-2"), checkpoint("second"));
        saver.put(config("oracle-cache-thread-3"), checkpoint("third"));

        countingDataSource.reset();
        var reloaded = saver.get(firstConfig);
        assertTrue(reloaded.isPresent());
        assertEquals(firstCheckpoint.getId(), reloaded.get().getId());
        assertEquals(1, countingDataSource.latestCheckpointSelects());
    }

    @Test
    public void testOracleSaverKeepsOnlyLatestCheckpointInMemory() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = OracleSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(16)
                .build();

        String threadId = "oracle-cache-single-thread";
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

    @Test
    public void testOracleSaverRefreshesLatestCacheWhenLatestCheckpointIsUpdated() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = OracleSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(16)
                .build();

        String threadId = "oracle-cache-update-latest-thread";
        var originalCheckpoint = checkpoint("original");
        var updatedCheckpoint = checkpoint("updated");
        var checkpointConfig = saver.put(config(threadId), originalCheckpoint);

        saver.put(checkpointConfig, updatedCheckpoint);

        countingDataSource.reset();
        var latest = saver.get(config(threadId));
        assertTrue(latest.isPresent());
        assertEquals(updatedCheckpoint.getId(), latest.get().getId());
        assertEquals(0, countingDataSource.latestCheckpointSelects());
    }

    @Test
    public void testOracleSaverClearsLatestCacheWhenThreadIsReleased() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = OracleSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(16)
                .build();

        String threadId = "oracle-cache-release-thread";
        saver.put(config(threadId), checkpoint("released"));

        countingDataSource.reset();
        saver.release(config(threadId));
        var latest = saver.get(config(threadId));
        assertTrue(latest.isEmpty());
        assertEquals(1, countingDataSource.latestCheckpointSelects());
    }

    @Test
    public void testOracleSaverCanDisableLatestCheckpointCache() throws Exception {
        var countingDataSource = new CountingDataSource(DATA_SOURCE);
        var saver = OracleSaver.builder()
                .createOption(CreateOption.CREATE_OR_REPLACE)
                .dataSource(countingDataSource)
                .maxCachedThreads(0)
                .build();

        String threadId = "oracle-cache-disabled-thread";
        var latestCheckpoint = checkpoint("latest");
        saver.put(config(threadId), latestCheckpoint);

        countingDataSource.reset();
        var firstRead = saver.get(config(threadId));
        assertTrue(firstRead.isPresent());
        assertEquals(latestCheckpoint.getId(), firstRead.get().getId());
        assertEquals(1, countingDataSource.latestCheckpointSelects());

        countingDataSource.reset();
        var secondRead = saver.get(config(threadId));
        assertTrue(secondRead.isPresent());
        assertEquals(latestCheckpoint.getId(), secondRead.get().getId());
        assertEquals(1, countingDataSource.latestCheckpointSelects());
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
            if (sql.contains("FETCH FIRST 1 ROW ONLY")) {
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
