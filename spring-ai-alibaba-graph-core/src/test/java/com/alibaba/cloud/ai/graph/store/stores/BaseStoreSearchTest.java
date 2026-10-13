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
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Changes the JVM default locale")
class BaseStoreSearchTest {

	@TempDir
	Path tempDir;

	@ParameterizedTest
	@CsvSource({ "false, en-US", "false, tr-TR", "false, az-AZ", "true, en-US", "true, tr-TR", "true, az-AZ" })
	void textSearchDoesNotDependOnDefaultLocale(boolean fileSystem, String languageTag) {
		Locale originalLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag(languageTag));
			Store store = fileSystem ? new FileSystemStore(tempDir) : new MemoryStore();
			store.putItem(StoreItem.of(List.of("documents"), "TITLE", Map.of("text", "other")));
			store.putItem(StoreItem.of(List.of("documents"), "note", Map.of("text", "TITLE")));
			store.putItem(StoreItem.of(List.of("documents"), "unrelated", Map.of("text", "other")));

			for (String query : List.of("title", "TITLE", "Title")) {
				assertThat(store.searchItems(StoreSearchRequest.builder().query(query).build()).getItems())
					.as("Results for query '%s' with default locale %s", query, languageTag)
					.extracting(StoreItem::getKey)
					.containsExactlyInAnyOrder("TITLE", "note");
			}
		}
		finally {
			Locale.setDefault(originalLocale);
		}
	}

}
