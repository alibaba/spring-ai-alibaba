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
package com.alibaba.cloud.ai.graph.store;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.store.stores.DatabaseStore;
import com.alibaba.cloud.ai.graph.store.stores.FileSystemStore;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.stores.MongoStore;
import com.alibaba.cloud.ai.graph.store.stores.RedisStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Store functionality with actual StateGraph execution.
 * <p>
 * This test demonstrates real-world usage patterns of Store in graph workflows, including
 * cross-session memory, multi-step workflows, and different storage backends.
 * </p>
 *
 * @author Spring AI Alibaba
 */
public class GraphStoreIntegrationTest {

	@TempDir
	Path tempDir;

	@Test
	void testUserSessionWorkflowWithMemoryStore() throws Exception {
		// Test user session workflow with memory store
		testUserSessionWorkflow(new MemoryStore(), "Memory");
	}

	@Test
	void testUserSessionWorkflowWithFileSystemStore() throws Exception {
		// Test user session workflow with file system store
		testUserSessionWorkflow(new FileSystemStore(tempDir.resolve("user_sessions")), "FileSystem");
	}

	@Test
	void testUserSessionWorkflowWithDatabaseStore() throws Exception {
		// Test user session workflow with database store
		testUserSessionWorkflow(createDatabaseStore("user_workflow"), "Database");
	}

	@Test
	void testCrossSessionPersistenceWorkflow() throws Exception {
		// Test cross-session persistence using FileSystem store
		FileSystemStore store = new FileSystemStore(tempDir.resolve("cross_session"));

		// First session - user creates profile and preferences
		String userId = "user123";
		Map<String, Object> session1Input = Map.of("userId", userId, "userName", "Alice Smith", "preferredLanguage",
				"zh-CN", "theme", "dark");

		Optional<OverAllState> session1Result = runUserProfileCreationGraph(store, session1Input);
		assertThat(session1Result).isPresent();
		assertThat(session1Result.get().<String>value("status").orElse("")).isEqualTo("profile_created");

		// Verify data was stored
		Optional<StoreItem> profileItem = store.getItem(List.of("users", userId), "profile");
		assertThat(profileItem).isPresent();
		assertThat(profileItem.get().getValue().get("name")).isEqualTo("Alice Smith");

		// Second session - user updates preferences (new graph instance)
		Map<String, Object> session2Input = Map.of("userId", userId, "theme", "light", "notifications", true);

		Optional<OverAllState> session2Result = runUserPreferencesUpdateGraph(store, session2Input);
		assertThat(session2Result).isPresent();
		assertThat(session2Result.get().<String>value("status").orElse("")).isEqualTo("preferences_updated");

		// Verify updated data
		Optional<StoreItem> prefsItem = store.getItem(List.of("users", userId), "preferences");
		assertThat(prefsItem).isPresent();
		assertThat(prefsItem.get().getValue().get("theme")).isEqualTo("light");
		assertThat(prefsItem.get().getValue().get("language")).isEqualTo("zh-CN"); // Should
		// preserve
		// existing
		assertThat(prefsItem.get().getValue().get("notifications")).isEqualTo(true);
	}

	@Test
	void testMultiStepDataProcessingWithStore() throws Exception {
		// Test multi-step data processing workflow with store persistence
		MemoryStore store = new MemoryStore();
		String taskId = "task456";

		Map<String, Object> input = Map.of("taskId", taskId, "data", "raw input data");

		Optional<OverAllState> result = runDataProcessingGraph(store, input);
		assertThat(result).isPresent();
		assertThat(result.get().<String>value("status").orElse("")).isEqualTo("processing_complete");

		// Verify all steps saved their results to store
		Optional<StoreItem> rawDataItem = store.getItem(List.of("tasks", taskId), "raw_data");
		Optional<StoreItem> processedDataItem = store.getItem(List.of("tasks", taskId), "processed_data");
		Optional<StoreItem> resultItem = store.getItem(List.of("tasks", taskId), "final_result");

		assertThat(rawDataItem).isPresent();
		assertThat(processedDataItem).isPresent();
		assertThat(resultItem).isPresent();

		// Verify data flow consistency
		assertThat(rawDataItem.get().getValue().get("data")).isEqualTo("raw input data");
		assertThat(processedDataItem.get().getValue().get("data")).isNotNull();
		assertThat(resultItem.get().getValue().get("result")).isNotNull();
	}

	@Test
	void testStoreSearchInGraphWorkflow() throws Exception {
		// Test using store search functionality in graph workflow
		MemoryStore store = new MemoryStore();

		// Setup some historical data
		setupHistoricalUserData(store);

		Map<String, Object> input = Map.of("query", "theme", "operation", "search_preferences");

		Optional<OverAllState> result = runDataSearchGraph(store, input);
		assertThat(result).isPresent();

		@SuppressWarnings("unchecked")
		List<Object> rawResults = (List<Object>) result.get().value("searchResults", Collections.emptyList());
		List<StoreItem> searchResults = rawResults.stream()
			.filter(StoreItem.class::isInstance)
			.map(StoreItem.class::cast)
			.toList();
		assertThat(searchResults).isNotEmpty();
		assertThat(searchResults)
			.anyMatch(item -> item.getValue().containsKey("theme") || item.getKey().toLowerCase().contains("theme"));
	}

	private void testUserSessionWorkflow(Store store, String storeType) throws Exception {
		String userId = "testUser" + System.nanoTime();
		Map<String, Object> input = Map.of("userId", userId, "action", "login", "userName",
				"Test User for " + storeType);

		Optional<OverAllState> result = runUserSessionGraph(store, input);

		assertThat(result).isPresent();
		assertThat(result.get().<String>value("status").orElse("")).isEqualTo("session_complete");
		assertThat(result.get().<String>value("storeType").orElse("")).isEqualTo(storeType);

		// Verify data was stored correctly
		Optional<StoreItem> sessionItem = store.getItem(List.of("sessions", userId), "current");
		assertThat(sessionItem).isPresent();
		assertThat(sessionItem.get().getValue().get("userName")).isEqualTo("Test User for " + storeType);
	}

	private Optional<OverAllState> runUserSessionGraph(Store store, Map<String, Object> input) throws Exception {
		CompileConfig config = CompileConfig.builder().store(store).build();

		StateGraph workflow = new StateGraph(
				() -> Map.of("userId", new ReplaceStrategy(), "userName", new ReplaceStrategy(), "action",
						new ReplaceStrategy(), "status", new ReplaceStrategy(), "storeType", new ReplaceStrategy()))
			.addNode("authenticate", node_async(state -> {
				String userId = state.value("userId", "");
				String userName = state.value("userName", "");

				// Log authentication in store
				Store sessionStore = state.getStore();
				if (sessionStore != null) {
					sessionStore.putItem(StoreItem.of(List.of("auth", "sessions"), userId, Map.of("userName", userName,
							"loginTime", System.currentTimeMillis(), "status", "authenticated")));
				}

				return Map.of("authenticated", true, "currentUser", userName);
			}))
			.addNode("processRequest", node_async(state -> {
				String userId = state.value("userId", "");
				String action = state.value("action", "");

				return Map.of("requestProcessed", true);
			}))
			.addNode("saveSession", node_async(state -> {
				String userId = state.value("userId", "");
				String userName = state.value("userName", "");
				Store sessionStore = state.getStore();

				if (sessionStore != null) {
					// Save current session
					sessionStore.putItem(StoreItem.of(List.of("sessions", userId), "current",
							Map.of("userName", userName, "sessionId", "session_" + System.nanoTime(), "completedAt",
									System.currentTimeMillis(), "status", "active")));
				}

				// Determine store type for verification
				String storeType = "Unknown";
				if (sessionStore instanceof MemoryStore)
					storeType = "Memory";
				else if (sessionStore instanceof FileSystemStore)
					storeType = "FileSystem";
				else if (sessionStore instanceof DatabaseStore)
					storeType = "Database";
				else if (sessionStore instanceof RedisStore)
					storeType = "Redis";
				else if (sessionStore instanceof MongoStore)
					storeType = "MongoDB";

				return Map.of("status", "session_complete", "storeType", storeType);
			}))
			.addEdge(START, "authenticate")
			.addEdge("authenticate", "processRequest")
			.addEdge("processRequest", "saveSession")
			.addEdge("saveSession", END);

		return workflow.compile(config).call(input);
	}

	private Optional<OverAllState> runUserProfileCreationGraph(Store store, Map<String, Object> input)
			throws Exception {
		CompileConfig config = CompileConfig.builder().store(store).build();

		StateGraph workflow = new StateGraph(
				() -> Map.of("userId", new ReplaceStrategy(), "userName", new ReplaceStrategy(), "preferredLanguage",
						new ReplaceStrategy(), "theme", new ReplaceStrategy(), "status", new ReplaceStrategy()))
			.addNode("createProfile", node_async(state -> {
				String userId = state.value("userId", "");
				String userName = state.value("userName", "");
				Store profileStore = state.getStore();

				if (profileStore != null) {
					profileStore.putItem(StoreItem.of(List.of("users", userId), "profile", Map.of("name", userName,
							"userId", userId, "createdAt", System.currentTimeMillis(), "status", "active")));
				}

				return Map.of("profileCreated", true);
			}))
			.addNode("setPreferences", node_async(state -> {
				String userId = state.value("userId", "");
				String language = state.value("preferredLanguage", "en");
				String theme = state.value("theme", "light");
				Store prefStore = state.getStore();

				if (prefStore != null) {
					prefStore.putItem(StoreItem.of(List.of("users", userId), "preferences",
							Map.of("language", language, "theme", theme, "updatedAt", System.currentTimeMillis())));
				}

				return Map.of("status", "profile_created");
			}))
			.addEdge(START, "createProfile")
			.addEdge("createProfile", "setPreferences")
			.addEdge("setPreferences", END);

		return workflow.compile(config).call(input);
	}

	private Optional<OverAllState> runUserPreferencesUpdateGraph(Store store, Map<String, Object> input)
			throws Exception {
		CompileConfig config = CompileConfig.builder().store(store).build();

		StateGraph workflow = new StateGraph(() -> Map.of("userId", new ReplaceStrategy(), "theme",
				new ReplaceStrategy(), "notifications", new ReplaceStrategy(), "currentPreferences",
				new ReplaceStrategy(), "status", new ReplaceStrategy()))
			.addNode("loadExistingPrefs", node_async(state -> {
				String userId = state.value("userId", "");
				Store prefStore = state.getStore();

				Map<String, Object> currentPrefs = new HashMap<>();
				if (prefStore != null) {
					Optional<StoreItem> existing = prefStore.getItem(List.of("users", userId), "preferences");
					if (existing.isPresent()) {
						currentPrefs = new HashMap<>(existing.get().getValue());
					}
				}

				return Map.of("currentPreferences", currentPrefs);
			}))
			.addNode("updatePrefs", node_async(state -> {
				String userId = state.value("userId", "");
				@SuppressWarnings("unchecked")
				Map<String, Object> currentPrefs = (Map<String, Object>) state.value("currentPreferences",
						new HashMap<String, Object>());
				Store prefStore = state.getStore();

				// Merge new preferences with existing ones
				Map<String, Object> updatedPrefs = new HashMap<>(currentPrefs);
				if (state.data().containsKey("theme")) {
					updatedPrefs.put("theme", state.value("theme").orElse(null));
				}
				if (state.data().containsKey("notifications")) {
					updatedPrefs.put("notifications", state.value("notifications").orElse(null));
				}
				updatedPrefs.put("updatedAt", System.currentTimeMillis());

				if (prefStore != null) {
					prefStore.putItem(StoreItem.of(List.of("users", userId), "preferences", updatedPrefs));
				}

				return Map.of("status", "preferences_updated");
			}))
			.addEdge(START, "loadExistingPrefs")
			.addEdge("loadExistingPrefs", "updatePrefs")
			.addEdge("updatePrefs", END);

		return workflow.compile(config).call(input);
	}

	private Optional<OverAllState> runDataProcessingGraph(Store store, Map<String, Object> input) throws Exception {
		CompileConfig config = CompileConfig.builder().store(store).build();

		StateGraph workflow = new StateGraph(() -> Map.of("taskId", new ReplaceStrategy(), "data",
				new ReplaceStrategy(), "status", new ReplaceStrategy()))
			.addNode("storeRawData", node_async(state -> {
				String taskId = state.value("taskId", "");
				String data = state.value("data", "");
				Store dataStore = state.getStore();

				if (dataStore != null) {
					dataStore.putItem(StoreItem.of(List.of("tasks", taskId), "raw_data",
							Map.of("data", data, "timestamp", System.currentTimeMillis())));
				}

				return Map.of("rawDataStored", true);
			}))
			.addNode("processData", node_async(state -> {
				String taskId = state.value("taskId", "");
				String data = state.value("data", "");
				Store dataStore = state.getStore();

				// Simulate data processing
				String processedData = "processed: " + data;

				if (dataStore != null) {
					dataStore.putItem(StoreItem.of(List.of("tasks", taskId), "processed_data",
							Map.of("data", processedData, "timestamp", System.currentTimeMillis())));
				}

				return Map.of("processedData", processedData);
			}))
			.addNode("generateResult", node_async(state -> {
				String taskId = state.value("taskId", "");
				String processedData = state.value("processedData", "");
				Store dataStore = state.getStore();

				// Generate final result
				String result = "result for: " + processedData;

				if (dataStore != null) {
					dataStore.putItem(StoreItem.of(List.of("tasks", taskId), "final_result",
							Map.of("result", result, "timestamp", System.currentTimeMillis())));
				}

				return Map.of("status", "processing_complete", "result", result);
			}))
			.addEdge(START, "storeRawData")
			.addEdge("storeRawData", "processData")
			.addEdge("processData", "generateResult")
			.addEdge("generateResult", END);

		return workflow.compile(config).call(input);
	}

	private Optional<OverAllState> runDataSearchGraph(Store store, Map<String, Object> input) throws Exception {
		CompileConfig config = CompileConfig.builder().store(store).build();

		StateGraph workflow = new StateGraph(() -> Map.of("query", new ReplaceStrategy(), "operation",
				new ReplaceStrategy(), "searchResults", new ReplaceStrategy()), new SpringAIStateSerializer())
			.addNode("searchData", node_async(state -> {
				String query = state.value("query", "");
				Store searchStore = state.getStore();

				List<StoreItem> searchResults = new ArrayList<>();
				if (searchStore != null) {
					StoreSearchRequest searchRequest = StoreSearchRequest.builder().query(query).limit(10).build();

					StoreSearchResult result = searchStore.searchItems(searchRequest);
					searchResults = result.getItems();
				}

				return Map.of("searchResults", searchResults, "searchQuery", query);
			}))
			.addNode("processResults", node_async(state -> {
				@SuppressWarnings("unchecked")
				List<StoreItem> searchResults = state.value("searchResults", List.class)
					.map(list -> (List<StoreItem>) list)
					.orElse(Collections.emptyList());
				String query = state.value("searchQuery", "");

				Map<String, Object> processedResults = Map
					.of("totalResults", searchResults.size(), "query", query, "resultSummary", searchResults.stream()
						.map(item -> Map.of("namespace", String.join("/", item.getNamespace()), "key", item.getKey()))
						.toList());

				return Map.of("processedResults", processedResults);
			}))
			.addEdge(START, "searchData")
			.addEdge("searchData", "processResults")
			.addEdge("processResults", END);

		return workflow.compile(config).call(input);
	}

	private void setupHistoricalUserData(Store store) {
		// Setup various user preferences for search testing
		store.putItem(StoreItem.of(List.of("users", "alice"), "preferences",
				Map.of("theme", "dark", "language", "en", "notifications", true)));
		store.putItem(StoreItem.of(List.of("users", "bob"), "preferences",
				Map.of("theme", "light", "language", "zh", "notifications", false)));
		store.putItem(StoreItem.of(List.of("users", "charlie"), "settings",
				Map.of("theme", "auto", "language", "ja", "timezone", "Asia/Tokyo")));
		store.putItem(StoreItem.of(List.of("system"), "theme_defaults",
				Map.of("default_theme", "light", "available_themes", List.of("light", "dark", "auto"))));
	}

	private DatabaseStore createDatabaseStore(String suffix) {
		String dbUrl = "jdbc:h2:mem:graph_integration_" + suffix + "_" + System.nanoTime()
				+ ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setUsername("sa");
		config.setPassword("");
		config.setDriverClassName("org.h2.Driver");

		return new DatabaseStore(new HikariDataSource(config), "graph_store");
	}

}
