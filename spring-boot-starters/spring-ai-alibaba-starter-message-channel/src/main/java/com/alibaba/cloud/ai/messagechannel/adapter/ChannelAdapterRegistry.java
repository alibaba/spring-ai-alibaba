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

package com.alibaba.cloud.ai.messagechannel.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lookup table from channel name (URL path variable, config key) to the
 * {@link MessageChannelAdapter} that implements it.
 */
public class ChannelAdapterRegistry {

	private final Map<String, MessageChannelAdapter> adapters;

	public ChannelAdapterRegistry(List<MessageChannelAdapter> discovered) {
		Map<String, MessageChannelAdapter> map = new HashMap<>();
		for (MessageChannelAdapter adapter : discovered) {
			MessageChannelAdapter prev = map.put(adapter.name(), adapter);
			if (prev != null) {
				throw new IllegalStateException("Duplicate MessageChannelAdapter for channel '"
						+ adapter.name() + "': " + prev.getClass().getName()
						+ " vs " + adapter.getClass().getName());
			}
		}
		this.adapters = Map.copyOf(map);
	}

	public MessageChannelAdapter require(String name) {
		MessageChannelAdapter adapter = adapters.get(name);
		if (adapter == null) {
			throw new IllegalArgumentException("No MessageChannelAdapter registered for channel '" + name + "'");
		}
		return adapter;
	}

	public boolean contains(String name) {
		return adapters.containsKey(name);
	}

}
