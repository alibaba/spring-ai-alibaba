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

import com.alibaba.cloud.ai.graph.store.NamespaceListRequest;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.alibaba.cloud.ai.graph.store.StoreSearchResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class StorePaginationOverflowTest {

	@TempDir
	Path tempDir;

	static Stream<Arguments> pages() {
		return Stream.of("memory", "filesystem", "redis", "mongo", "database")
			.flatMap(type -> Stream.of(Arguments.of(type, 1, Integer.MAX_VALUE, List.of("b", "c")),
					Arguments.of(type, 0, Integer.MAX_VALUE, List.of("a", "b", "c")),
					Arguments.of(type, 1, 1, List.of("b")),
					Arguments.of(type, Integer.MAX_VALUE, Integer.MAX_VALUE, List.of())));
	}

	@ParameterizedTest
	@MethodSource("pages")
	void searchItemsReturnsTheRequestedPage(String type, int offset, int limit, List<String> expected) {
		try (StoreFixture fixture = createStore(type)) {
			Store store = fixture.store();
			putItems(store);
			StoreSearchRequest request = StoreSearchRequest.builder()
				.sortFields(List.of("key"))
				.offset(offset)
				.limit(limit)
				.build();

			assertThatCode(() -> {
				StoreSearchResult result = store.searchItems(request);
				assertThat(result.getItems()).extracting(StoreItem::getKey).containsExactlyElementsOf(expected);
				assertThat(result.getTotalCount()).isEqualTo(3);
				assertThat(result.getOffset()).isEqualTo(offset);
				assertThat(result.getLimit()).isEqualTo(limit);
			}).doesNotThrowAnyException();
		}
	}

	@ParameterizedTest
	@MethodSource("pages")
	void listNamespacesReturnsTheRequestedPage(String type, int offset, int limit, List<String> expected) {
		try (StoreFixture fixture = createStore(type)) {
			Store store = fixture.store();
			putItems(store);
			NamespaceListRequest request = NamespaceListRequest.builder().offset(offset).limit(limit).build();

			assertThatCode(() -> assertThat(store.listNamespaces(request)).containsExactlyElementsOf(expected))
				.doesNotThrowAnyException();
		}
	}

	private void putItems(Store store) {
		for (String key : List.of("a", "b", "c")) {
			store.putItem(StoreItem.of(List.of(key), key, Map.of("value", key)));
		}
	}

	private StoreFixture createStore(String type) {
		return switch (type) {
			case "memory" -> new StoreFixture(new MemoryStore(), null);
			case "filesystem" -> new StoreFixture(new FileSystemStore(tempDir), null);
			case "redis" -> new StoreFixture(new RedisStore(), null);
			case "mongo" -> new StoreFixture(new MongoStore(), null);
			case "database" -> {
				HikariConfig config = new HikariConfig();
				config.setJdbcUrl("jdbc:h2:mem:pagination" + System.nanoTime());
				config.setUsername("sa");
				config.setPassword("");
				config.setMaximumPoolSize(1);
				HikariDataSource dataSource = new HikariDataSource(config);
				yield new StoreFixture(new DatabaseStore(dataSource, "pagination_store"), dataSource);
			}
			default -> throw new IllegalArgumentException("Unknown store: " + type);
		};
	}

	private record StoreFixture(Store store, HikariDataSource dataSource) implements AutoCloseable {

		@Override
		public void close() {
			if (dataSource != null) {
				dataSource.close();
			}
		}

	}

}
