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
package com.alibaba.cloud.ai.graph.agent.context;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.RunnableConfig.STATE_UPDATE_METADATA_KEY;

/**
 * Selects the data crossing a delegated agent's execution boundary. State values
 * are provided as reference data in a user message and in the child's graph
 * state under {@link #STATE_KEY}. Metadata is available only to the child's
 * runtime unless the child explicitly places it into a prompt.
 */
public final class AgentToolContext {

	public static final String STATE_KEY = "_DELEGATED_AGENT_CONTEXT_";

	private final List<String> stateKeys;

	private final List<String> metadataKeys;

	private final List<String> outputKeys;

	private final int maxContextCharacters;

	private AgentToolContext(Builder builder) {
		this.stateKeys = List.copyOf(builder.stateKeys);
		this.metadataKeys = List.copyOf(builder.metadataKeys);
		this.outputKeys = List.copyOf(builder.outputKeys);
		this.maxContextCharacters = builder.maxContextCharacters;
	}

	public static Builder builder() {
		return new Builder();
	}

	public List<String> stateKeys() {
		return this.stateKeys;
	}

	public List<String> metadataKeys() {
		return this.metadataKeys;
	}

	public List<String> outputKeys() {
		return this.outputKeys;
	}

	public int maxContextCharacters() {
		return this.maxContextCharacters;
	}

	public static final class Builder {

		private final Set<String> stateKeys = new LinkedHashSet<>();

		private final Set<String> metadataKeys = new LinkedHashSet<>();

		private final Set<String> outputKeys = new LinkedHashSet<>();

		private int maxContextCharacters = 8192;

		/** Includes the named parent state values as JSON-compatible reference data. */
		public Builder stateKeys(String... keys) {
			addKeys(this.stateKeys, keys);
			return this;
		}

		/** Forwards the named parent runtime metadata keys. */
		public Builder metadataKeys(String... keys) {
			addKeys(this.metadataKeys, keys);
			return this;
		}

		/** Returns the final answer and these child state values in a JSON tool result. */
		public Builder outputKeys(String... keys) {
			addKeys(this.outputKeys, keys);
			return this;
		}

		/** Limits the JSON size of selected parent state and runtime metadata, in characters. */
		public Builder maxContextCharacters(int maxContextCharacters) {
			if (maxContextCharacters < 1) {
				throw new IllegalArgumentException("maxContextCharacters must be positive");
			}
			this.maxContextCharacters = maxContextCharacters;
			return this;
		}

		public AgentToolContext build() {
			return new AgentToolContext(this);
		}

		private static void addKeys(Set<String> destination, String[] keys) {
			for (String key : keys) {
				if (!StringUtils.hasText(key) || key.startsWith("_") || "messages".equals(key)
						|| HUMAN_FEEDBACK_METADATA_KEY.equals(key) || STATE_UPDATE_METADATA_KEY.equals(key)) {
					throw new IllegalArgumentException("Agent tool context keys must be non-reserved names");
				}
			}
			destination.addAll(List.of(keys));
		}

	}

}
