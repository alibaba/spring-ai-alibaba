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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql.PostgresSaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfDockerAvailable
@EnabledIf(value = "isCI", disabledReason = "this test is designed to run only in the GitHub CI environment.")
@Testcontainers
public class PostgresSaverTest {
    private static boolean isCI() {
        return "true".equalsIgnoreCase(System.getProperty("CI", System.getenv("CI")));
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostgresSaverTest.class);

    private static String DATABASE_NAME = "lg4j-store";

    private static String[] IMAGES = {
            "postgres:16-alpine",
            "pgvector/pgvector:pg16"
    };

    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGES[1])
                            .withDatabaseName(DATABASE_NAME);

    static KeyStrategyFactory keyStrategyFactory = () -> {
        Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
        keyStrategyMap.put("agent_1:prop1", (o, o2) -> o2);
        return keyStrategyMap;
    };
    static StateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new, new ObjectMapper());

    @BeforeAll
    public static void init() throws IOException {
        // initialize log
        try( var is = PostgresSaverTest.class.getResourceAsStream("/logging.properties") ) {
            if( is!=null ) LogManager.getLogManager().readConfiguration(is);
        }

        // start postgres container
        postgres.start();

    }

    @AfterAll
    public static void shutdown() {
        postgres.stop();
    }

    PostgresSaver.Builder buildPostgresSaver() throws SQLException {
        return PostgresSaver.builder()
                //.host("localhost")
                .host(postgres.getHost())
                //.port(5432)
                .port(postgres.getFirstMappedPort())
                //.user("admin")
                .user(postgres.getUsername())
                //.password("bsorrentino")
                .password(postgres.getPassword())
                .database(DATABASE_NAME)
                .stateSerializer(serializer)
                ;
    }

    @Test
    public void testCheckpointWithReleasedThread() throws Exception {

        var saver = buildPostgresSaver()
                        .dropTablesFirst(true)
                        .build();

        NodeAction agent_1 = state -> {
            log.info( "agent_1");
            return Map.of("agent_1:prop1", "agent_1:test");
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async( agent_1 ))
                .addEdge( START,"agent_1")
                .addEdge( "agent_1",  END)
                ;

        var compileConfig = CompileConfig.builder()
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .releaseThread(true)
                                .build();

        var runnableConfig = RunnableConfig.builder()
                            .build();
        var workflow = graph.compile( compileConfig );

        Map<String, Object> inputs = Map.of( "input", "test1");

        var result = workflow.invoke( inputs, runnableConfig );

        assertTrue( result.isPresent() );

        var history = workflow.getStateHistory( runnableConfig );

        assertTrue( history.isEmpty() );

    }

    @Test
    public void testCheckpointWithNotReleasedThread() throws Exception {
        var saver = buildPostgresSaver()
                        .dropTablesFirst(true)
                        .build();


        NodeAction agent_1 = state -> {
            log.info( "agent_1");
            return Map.of("agent_1:prop1", "agent_1:test");
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async( agent_1 ))
                .addEdge( START,"agent_1")
                .addEdge( "agent_1",  END)
                ;

        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile( compileConfig );

        Map<String, Object> inputs = Map.of( "input", "test1");

        var result = workflow.invoke( inputs, runnableConfig );

        assertTrue( result.isPresent() );

        var history = workflow.getStateHistory( runnableConfig );

        assertFalse( history.isEmpty() );
        assertEquals( 2, history.size() );

        var lastSnapshot = workflow.lastStateOf( runnableConfig );

        assertTrue( lastSnapshot.isPresent() );
        assertEquals( "agent_1", lastSnapshot.get().node() );
        assertEquals( END, lastSnapshot.get().next() );

        // UPDATE STATE
        final var updatedConfig = workflow.updateState( lastSnapshot.get().config(), Map.of( "update", "update test") );

        var updatedSnapshot = workflow.stateOf( updatedConfig );
        assertTrue( updatedSnapshot.isPresent() );
        assertEquals( "agent_1", updatedSnapshot.get().node() );
        assertTrue( updatedSnapshot.get().state().value("update").isPresent() );
        assertEquals( "update test", updatedSnapshot.get().state().value("update").get() );
        assertEquals( END, lastSnapshot.get().next() );

        // test checkpoints reloading from database
        saver = buildPostgresSaver().build(); // create a new saver (reset cache)

        compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(saver).build())
                .releaseThread(false)
                .build();

        runnableConfig = RunnableConfig.builder().build();
        workflow = graph.compile( compileConfig );

        history = workflow.getStateHistory( runnableConfig );

        assertFalse( history.isEmpty() );
        assertEquals( 2, history.size() );

        lastSnapshot = workflow.stateOf(updatedConfig);
        // lastSnapshot = workflow.lastStateOf( runnableConfig );

        assertTrue( lastSnapshot.isPresent() );
        assertEquals( "agent_1", lastSnapshot.get().node() );
        assertEquals( END, lastSnapshot.get().next() );
        assertTrue( lastSnapshot.get().state().value("update").isPresent() );
        assertEquals( "update test", lastSnapshot.get().state().value("update").get() );
        assertEquals( END, lastSnapshot.get().next() );


        saver.release( runnableConfig );

    }


    private void addUniqueConstraint() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    WITH latest_checkpoints AS (
                        SELECT checkpoint_id,
                               ROW_NUMBER() OVER (
                                   PARTITION BY thread_id
                                   ORDER BY saved_at DESC, checkpoint_id DESC
                               ) AS rn
                        FROM GraphCheckpoint
                    )
                    DELETE FROM GraphCheckpoint
                    WHERE checkpoint_id IN (
                        SELECT checkpoint_id FROM latest_checkpoints WHERE rn > 1
                    )
                    """);

            try {
                stmt.execute("CREATE UNIQUE INDEX idx_unique_graphcheckpoint_thread ON GraphCheckpoint(thread_id)");
            } catch (Exception e) {
                // 忽略已存在的错误
                log.debug("Index already exists or error creating index: {}", e.getMessage());
            }
        }
    }


    @Test
    public void testInsertMode() throws Exception {
        var saver = buildPostgresSaver()
                        .dropTablesFirst(true)
                        .overwriteMode(false)
                        .build();

        NodeAction agent_1 = state -> {
            log.info( "agent_1");
            return Map.of("agent_1:prop1", "agent_1:test");
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async( agent_1 ))
                .addEdge( START,"agent_1")
                .addEdge( "agent_1",  END);

        var compileConfig = CompileConfig.builder()
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .releaseThread(false)
                                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile( compileConfig );

        Map<String, Object> inputs1 = Map.of( "input", "test1");
        var result1 = workflow.invoke( inputs1, runnableConfig );
        assertTrue( result1.isPresent() );

        Map<String, Object> inputs2 = Map.of( "input", "test2");
        var result2 = workflow.invoke( inputs2, runnableConfig );
        assertTrue( result2.isPresent() );

        var history = workflow.getStateHistory( runnableConfig );
        assertFalse( history.isEmpty() );
        assertEquals( 4, history.size(), "插入模式应该保留所有历史记录" );

        saver.release( runnableConfig );
    }


    @Test
    public void testOverwriteMode() throws Exception {
        var saver = buildPostgresSaver()
                        .dropTablesFirst(true)
                        .build();

        // addUniqueConstraint(); // 注释掉：唯一约束与overwriteMode冲突（单次运行产生多个checkpoints共享thread_id）

        saver = buildPostgresSaver()
                        .overwriteMode(true)
                        .build();

        NodeAction agent_1 = state -> {
            log.info( "agent_1");
            return Map.of("agent_1:prop1", "agent_1:test_overwrite");
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async( agent_1 ))
                .addEdge( START,"agent_1")
                .addEdge( "agent_1",  END);

        var compileConfig = CompileConfig.builder()
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .releaseThread(false)
                                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile( compileConfig );

        Map<String, Object> inputs1 = Map.of( "input", "test1");
        var result1 = workflow.invoke( inputs1, runnableConfig );
        assertTrue( result1.isPresent() );

        Map<String, Object> inputs2 = Map.of( "input", "test2");
        var result2 = workflow.invoke( inputs2, runnableConfig );
        assertTrue( result2.isPresent() );

        Map<String, Object> inputs3 = Map.of( "input", "test3");
        var result3 = workflow.invoke( inputs3, runnableConfig );
        assertTrue( result3.isPresent() );

        var history = workflow.getStateHistory( runnableConfig );
        assertFalse( history.isEmpty() );
        assertEquals( 2, history.size(), "覆盖模式应该只保留最新执行的记录" );

        var lastSnapshot = workflow.lastStateOf( runnableConfig );
        assertTrue( lastSnapshot.isPresent() );
        assertEquals( "agent_1", lastSnapshot.get().node() );
        assertEquals( "agent_1:test_overwrite", lastSnapshot.get().state().value("agent_1:prop1").orElse(null) );

        saver.release( runnableConfig );
    }

    @Test
    public void testOverwriteModeDataConsistency() throws Exception {
        var saver = buildPostgresSaver()
                        .dropTablesFirst(true)
                        .build();

        // addUniqueConstraint(); // 注释掉：唯一约束与overwriteMode冲突（单次运行产生多个checkpoints共享thread_id）

        saver = buildPostgresSaver()
                        .overwriteMode(true)
                        .build();

        NodeAction agent_1 = state -> {
            Object input = state.data().get("input");
            log.info( "agent_1 processing: {}", input);
            return Map.of("agent_1:prop1", "processed_" + input);
        };

        var graph = new StateGraph(keyStrategyFactory)
                .addNode("agent_1", node_async( agent_1 ))
                .addEdge( START,"agent_1")
                .addEdge( "agent_1",  END);

        var compileConfig = CompileConfig.builder()
                                .saverConfig(SaverConfig.builder().register(saver).build())
                                .releaseThread(false)
                                .build();

        var runnableConfig = RunnableConfig.builder().build();
        var workflow = graph.compile( compileConfig );

        String[] inputs = {"data1", "data2", "data3", "data4", "data5"};
        for (String input : inputs) {
            var result = workflow.invoke( Map.of("input", input), runnableConfig );
            assertTrue( result.isPresent() );

            var lastSnapshot = workflow.lastStateOf( runnableConfig );
            assertTrue( lastSnapshot.isPresent() );
            assertEquals( "processed_" + input,
                    lastSnapshot.get().state().value("agent_1:prop1").orElse(null),
                    "应该能读取到最新覆盖的数据" );
        }

        var history = workflow.getStateHistory( runnableConfig );
        assertEquals( 2, history.size(), "覆盖模式应该只保留最新执行的记录" );

        saver.release( runnableConfig );
    }

}
