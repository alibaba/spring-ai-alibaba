/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.memory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.util.StringUtils;

record ScopedMemory(Store store, List<String> namespace) {

	static Optional<ScopedMemory> resolve(ToolContext context, Store configured,
			Function<RunnableConfig, List<String>> namespaceResolver) {
		return ToolContextHelper.getConfig(context)
				.flatMap(config -> namespace(config, namespaceResolver).flatMap(namespace -> {
					Store store = configured != null ? configured : config.store();
					if (store == null) {
						store = ToolContextHelper.getState(context).map(OverAllState::getStore).orElse(null);
					}
					return store == null ? Optional.empty() : Optional.of(new ScopedMemory(store, namespace));
				}));
	}

	static Optional<List<String>> namespace(RunnableConfig config, Function<RunnableConfig, List<String>> resolver) {
		var namespace = resolver.apply(config);
		if (namespace == null || namespace.isEmpty() || namespace.stream().anyMatch(part -> !usableSegment(part))) {
			return Optional.empty();
		}
		return Optional.of(List.copyOf(namespace));
	}

	List<String> searchKeys(String query, int limit) {
		var request = StoreSearchRequest.builder().namespace(this.namespace).exactNamespace(true)
				.query(query).sortFields(List.of("key")).limit(limit).build();
		return this.store.searchItems(request).getItems().stream().filter(Objects::nonNull)
				.filter(item -> this.namespace.equals(item.getNamespace()))
				.map(StoreItem::getKey).filter(ScopedMemory::usableKey).distinct().limit(limit).toList();
	}

	Optional<StoreItem> get(String key) {
		return this.store.getItem(this.namespace, key)
				.filter(item -> this.namespace.equals(item.getNamespace()) && key.equals(item.getKey()));
	}

	static boolean usableKey(String key) {
		return usableSegment(key) && key.length() <= 256;
	}

	private static boolean usableSegment(String value) {
		return StringUtils.hasText(value) && !value.equals(".") && !value.equals("..")
				&& value.indexOf('/') == -1 && value.indexOf('\\') == -1
				&& value.chars().noneMatch(Character::isISOControl);
	}

}
