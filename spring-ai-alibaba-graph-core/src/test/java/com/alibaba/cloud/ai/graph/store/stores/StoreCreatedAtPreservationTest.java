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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-implementation regression test for {@link Store#putItem(StoreItem)} update
 * semantics: when an item with the same (namespace, key) already exists, the original
 * {@code createdAt} must be preserved while {@code updatedAt} is refreshed.
 * <p>
 * Covers {@code MemoryStore}, {@code FileSystemStore} and the H2-backed
 * {@code DatabaseStore} so all put-on-existing-key paths honor the same update contract.
 * <p>
 * Deterministic by construction: the original item is created with an explicit, clearly
 * old {@code createdAt}; the update goes through {@code StoreItem.of(...)} which sets
 * {@code createdAt = now}. No {@code Thread.sleep} is used, so there is no timing flakiness.
 *
 * @author Spring AI Alibaba
 */
class StoreCreatedAtPreservationTest {

	@TempDir
	Path tempDir;

	private static final List<String> NS = List.of("users", "user123");

	private static final String KEY = "preferences";

	private static final long OLD_CREATED_AT = 1_000_000_000_000L; // fixed, clearly in the past

	private static final long OLD_UPDATED_AT = 1_000_000_000_000L;

	private Store create(String name) {
		switch (name) {
			case "FileSystemStore":
				return new FileSystemStore(tempDir);
			case "DatabaseStore(H2)":
				return newH2DatabaseStore();
			default:
				return new MemoryStore();
		}
	}

	private static DatabaseStore newH2DatabaseStore() {
		String dbUrl = "jdbc:h2:mem:createdat" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(dbUrl);
		config.setUsername("sa");
		config.setPassword("");
		config.setDriverClassName("org.h2.Driver");
		DataSource dataSource = new HikariDataSource(config);
		return new DatabaseStore(dataSource, "created_at_store");
	}

	@ParameterizedTest(name = "{0} preserves createdAt on update")
	@MethodSource("storeNames")
	void putItemUpdateShouldPreserveCreatedAt(String storeName) {
		Store store = create(storeName);

		// Given an item created in the past
		StoreItem original = new StoreItem(NS, KEY, Map.of("version", 1), OLD_CREATED_AT, OLD_UPDATED_AT);
		store.putItem(original);

		// When the same (namespace, key) is updated via StoreItem.of(...) (createdAt = now)
		StoreItem updated = StoreItem.of(NS, KEY, Map.of("version", 2, "updated", true));
		store.putItem(updated);

		// Then value is updated, createdAt is preserved, updatedAt is refreshed
		Optional<StoreItem> retrieved = store.getItem(NS, KEY);
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getValue()).isEqualTo(Map.of("version", 2, "updated", true));
		assertThat(retrieved.get().getCreatedAt())
			.as("%s must preserve the original createdAt on update", storeName)
			.isEqualTo(OLD_CREATED_AT);
		assertThat(retrieved.get().getUpdatedAt())
			.as("%s must refresh updatedAt on update", storeName)
			.isGreaterThan(OLD_UPDATED_AT);
	}

	static Stream<String> storeNames() {
		return Stream.of("MemoryStore", "FileSystemStore", "DatabaseStore(H2)");
	}

}
