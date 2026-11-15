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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that store text search does not depend on the JVM default locale.
 */
class StoreTextSearchLocaleTest {

	@TempDir
	Path tempDir;

	@Test
	void textSearchIsIndependentOfTheDefaultLocale() {
		Locale original = Locale.getDefault();
		try {
			for (String languageTag : List.of("en-US", "tr-TR", "az-AZ")) {
				Locale.setDefault(Locale.forLanguageTag(languageTag));
				assertSearchMatchesKeyAndValue(new MemoryStore(), languageTag);
				assertSearchMatchesKeyAndValue(new FileSystemStore(tempDir.resolve(languageTag)), languageTag);
			}
		}
		finally {
			Locale.setDefault(original);
		}
	}

	private void assertSearchMatchesKeyAndValue(Store store, String languageTag) {
		store.putItem(StoreItem.of(List.of("documents"), "TITLE", Map.of("text", "other")));
		store.putItem(StoreItem.of(List.of("documents"), "note", Map.of("text", "TITLE")));

		var items = store.searchItems(StoreSearchRequest.builder().query("title").build()).getItems();

		assertThat(items).as(languageTag)
			.extracting(StoreItem::getKey)
			.containsExactlyInAnyOrder("TITLE", "note");
	}

}
