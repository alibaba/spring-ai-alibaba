/*
 * Copyright 2025-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class FileSystemStoreResourceTest {

	@TempDir
	Path tempDir;

	@ParameterizedTest
	@ValueSource(strings = { "size", "search", "namespaces", "clear" })
	void closesDirectoryTraversalStreams(String operation) throws Exception {
		FileSystemStore store = new FileSystemStore(tempDir);
		store.putItem(StoreItem.of(List.of("users", "alice"), "profile", Map.of("name", "Alice")));
		List<Stream<Path>> opened = new ArrayList<>();
		AtomicInteger closed = new AtomicInteger();

		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.walk(any(Path.class))).thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				Stream<Path> stream = (Stream<Path>) invocation.callRealMethod();
				opened.add(stream);
				return stream.onClose(closed::incrementAndGet);
			});
			files.when(() -> Files.list(any(Path.class))).thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				Stream<Path> stream = (Stream<Path>) invocation.callRealMethod();
				opened.add(stream);
				return stream.onClose(closed::incrementAndGet);
			});

			switch (operation) {
				case "size" -> assertThat(store.size()).isEqualTo(1);
				case "search" -> assertThat(store.searchItems(StoreSearchRequest.builder().build()).getItems()).hasSize(1);
				case "namespaces" -> assertThat(store.listNamespaces(NamespaceListRequest.builder().build()))
					.contains("users", "users/alice");
				case "clear" -> store.clear();
				default -> throw new AssertionError(operation);
			}
			assertThat(opened).isNotEmpty();
			assertThat(closed.get()).isEqualTo(opened.size());
		}
		finally {
			opened.forEach(Stream::close);
		}
	}

}
