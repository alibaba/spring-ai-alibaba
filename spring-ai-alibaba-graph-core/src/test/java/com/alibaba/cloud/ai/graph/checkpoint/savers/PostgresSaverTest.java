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

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql.PostgresSaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;

import java.io.IOException;
import java.sql.SQLException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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


	@Test
	public void testPostgresSaverMultipleRoundTrips() throws Exception {

		var saver = buildPostgresSaver()
			.dropTablesFirst(true)
			.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("counter", (o, o2) -> o2);
			keyStrategyMap.put("graphResponse", (o, o2) -> o2);
			return keyStrategyMap;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "node1")
			.addNode("node1", node_async(state -> {
				int counter = (int) state.value("counter").orElse(0);
				counter++;

				Map<String, Object> metadata = Map.of(
					"iteration", counter,
					"nodeId", "node1",
					"timestamp", System.currentTimeMillis()
				);
				GraphResponse<?> response =
					GraphResponse.of("Result " + counter, metadata);

				return Map.of(
					"counter", counter,
					"graphResponse", response
				);
			}))
			.addEdge("node1", END);

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);

		String threadId = "bug3895-postgres-thread";
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		try {
			for (int i = 0; i < 5; i++) {

				var stateOpt = app.invoke(Map.of(), config);


				assertTrue(stateOpt.isPresent(), "State should be present");
				var result = stateOpt.get();
				assertEquals(i + 1, result.data().get("counter"), "Counter should be " + (i + 1));

				Object graphResponseObj = result.data().get("graphResponse");
				assertNotNull(graphResponseObj, "GraphResponse should not be null");
				assertTrue(graphResponseObj instanceof GraphResponse,
					"Should be GraphResponse instance");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> graphResponse =
					(GraphResponse<Object>) graphResponseObj;

				Map<String, Object> metadata = graphResponse.getAllMetadata();
				assertFalse(metadata.containsKey("@class"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @class field (Bug #3895)");
				assertFalse(metadata.containsKey("@type"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @type field");
				assertFalse(metadata.containsKey("@typeHint"),
					"Iteration " + (i + 1) + ": metadata should NOT contain @typeHint field");

				assertEquals(3, metadata.size(),
					"Iteration " + (i + 1) + ": metadata should have exactly 3 fields (no accumulated type markers)");

				assertEquals(i + 1, metadata.get("iteration"),
					"Iteration metadata should match");
				assertEquals("node1", metadata.get("nodeId"),
					"NodeId should be preserved");
				assertNotNull(metadata.get("timestamp"),
					"Timestamp should be preserved");


				var snapshot = app.getState(config);
				assertNotNull(snapshot, "Snapshot should not be null");

				var snapshotState = snapshot.state();
				assertNotNull(snapshotState, "Snapshot state should not be null");
				assertEquals(i + 1, snapshotState.data().get("counter"),
					"Snapshot counter should match");

			}


		} finally {
			saver.release(config);
		}
	}

	@Test
	public void testUserScenarioWithoutExplicitMetadata() throws Exception {

		var saver = buildPostgresSaver()
			.dropTablesFirst(true)
			.build();

		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("messages", (o, o2) -> o2);
			keyStrategyMap.put("lastResponse", (o, o2) -> o2);
			return keyStrategyMap;
		};

		StateGraph workflow = new StateGraph(keyStrategyFactory)
			.addEdge(START, "processNode")
			.addNode("processNode", node_async(state -> {
				@SuppressWarnings("unchecked")
				var messages = (java.util.List<String>) state.value("messages").orElse(new java.util.ArrayList<String>());
				var newMessages = new java.util.ArrayList<>(messages);
				newMessages.add("Message " + (messages.size() + 1));

				GraphResponse<?> response =
					GraphResponse.of(
						"Processed message " + newMessages.size()
					);


				return Map.of(
					"messages", newMessages,
					"lastResponse", response
				);
			}))
			.addEdge("processNode", END);

		CompileConfig compileConfig = CompileConfig.builder()
			.saverConfig(SaverConfig.builder()
				.register(saver)
				.build())
			.build();

		CompiledGraph app = workflow.compile(compileConfig);
		String threadId = "bug3895-user-scenario-postgres";
		RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

		try {
			for (int round = 0; round < 5; round++) {

				var stateOpt = app.invoke(Map.of(), config);
				assertTrue(stateOpt.isPresent(), "State should be present");

				var result = stateOpt.get();
				var messages = (java.util.List<?>) result.data().get("messages");
				assertNotNull(messages, "Messages should not be null");
				assertEquals(round + 1, messages.size(),
					"Round " + (round + 1) + ": Should have " + (round + 1) + " messages");

				Object lastResponseObj = result.data().get("lastResponse");
				assertNotNull(lastResponseObj, "LastResponse should not be null");
				assertTrue(lastResponseObj instanceof GraphResponse,
					"LastResponse should be GraphResponse instance");

				@SuppressWarnings("unchecked")
				GraphResponse<Object> lastResponse =
					(GraphResponse<Object>) lastResponseObj;

				Map<String, Object> metadata = lastResponse.getAllMetadata();

				assertFalse(metadata.containsKey("@class"),
					"Round " + (round + 1) + ": metadata should NOT contain @class field");
				assertFalse(metadata.containsKey("@type"),
					"Round " + (round + 1) + ": metadata should NOT contain @type field");
				assertFalse(metadata.containsKey("@typeHint"),
					"Round " + (round + 1) + ": metadata should NOT contain @typeHint field");


				var snapshot = app.getState(config);
				assertNotNull(snapshot, "Snapshot should not be null");

				var snapshotState = snapshot.state();
				assertNotNull(snapshotState, "Snapshot state should not be null");

				Object restoredResponseObj = snapshotState.data().get("lastResponse");
				if (restoredResponseObj instanceof GraphResponse) {
					@SuppressWarnings("unchecked")
					GraphResponse<Object> restoredResponse =
						(GraphResponse<Object>) restoredResponseObj;
					Map<String, Object> restoredMetadata = restoredResponse.getAllMetadata();

					assertFalse(restoredMetadata.containsKey("@class"),
						"Round " + (round + 1) + ": Restored metadata should NOT contain @class");

				}
			}


		} finally {
			saver.release(config);
		}
	}

}
