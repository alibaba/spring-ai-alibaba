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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;
import java.util.UUID;

import com.alibaba.cloud.ai.graph.utils.SerializationUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class Checkpoint {

	private static final String STORAGE_FORMAT_KEY = "__spring_ai_alibaba_checkpoint_format__";

	private static final String STORAGE_FORMAT_V1 = "runtime-v1";

	private final String id;

	private final Map<String, Object> state;

	private final String nodeId;

	private final String nextNodeId;

	private final Map<String, Object> runtimeData;

	@JsonCreator
	private Checkpoint(@JsonProperty("id") String id, @JsonProperty("state") Map<String, Object> state,
			@JsonProperty("nodeId") String nodeId, @JsonProperty("nextNodeId") String nextNodeId,
			@JsonProperty("runtimeData") Map<String, Object> runtimeData) {

		this.id = requireNonNull(id, "id cannot be null");
		this.state = requireNonNull(state, "state cannot be null");
		this.nodeId = requireNonNull(nodeId, "nodeId cannot be null");
		this.nextNodeId = requireNonNull(nextNodeId, "Checkpoint.nextNodeId cannot be null");
		this.runtimeData = runtimeData == null ? Map.of() : Map.copyOf(runtimeData);

	}

	/**
	 * create a copy of given checkpoint with a new id
	 * @param checkpoint value from which copy is created
	 * @return new copy with different id
	 */
	public static Checkpoint copyOf(Checkpoint checkpoint) {
		requireNonNull(checkpoint, "checkpoint cannot be null");
		return new Checkpoint(UUID.randomUUID().toString(), SerializationUtils.deepCopyMap(checkpoint.state),
				checkpoint.nodeId, checkpoint.nextNodeId, Map.of());
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getId() {
		return id;
	}

	public Map<String, Object> getState() {
		return state;
	}

	public String getNodeId() {
		return nodeId;
	}

	public String getNextNodeId() {
		return nextNodeId;
	}

	@JsonProperty("runtimeData")
	public Map<String, Object> runtimeData() {
		return runtimeData;
	}

	public Checkpoint withRuntimeData(Map<String, Object> data) {
		return new Checkpoint(id, state, nodeId, nextNodeId, data);
	}

	/**
	 * Encodes checkpoint data for savers that store a single state payload.
	 * Checkpoints without runtime data continue to use the original state payload.
	 */
	public Map<String, Object> stateForStorage() {
		if (runtimeData.isEmpty() && !state.containsKey(STORAGE_FORMAT_KEY)) {
			return state;
		}
		return Map.of(STORAGE_FORMAT_KEY, STORAGE_FORMAT_V1, "state", state, "runtimeData", runtimeData);
	}

	public Checkpoint updateState(Map<String, Object> values, Map<String, KeyStrategy> channels) {

		return new Checkpoint(this.id, OverAllState.updateState(this.state, values, channels), this.nodeId,
				this.nextNodeId, Map.of());
	}

	@Override
	public String toString() {
		return format("Checkpoint{ id=%s, nodeId=%s, nextNodeId=%s, state=%s }", id, nodeId, nextNodeId, state);
	}

	public static class Builder {

		private String id = UUID.randomUUID().toString();

		private Map<String, Object> state = null;

		private String nodeId = null;

		private String nextNodeId = null;

		private Map<String, Object> runtimeData = Map.of();

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder state(OverAllState state) {
			this.state = state.data();
			return this;
		}

		public Builder state(Map<String, Object> state) {
			this.state = state;
			return this;
		}

		@SuppressWarnings("unchecked")
		public Builder stateFromStorage(Map<String, Object> payload) {
			if (STORAGE_FORMAT_V1.equals(payload.get(STORAGE_FORMAT_KEY))
					&& payload.get("state") instanceof Map<?, ?> data
					&& payload.get("runtimeData") instanceof Map<?, ?> runtime) {
				this.state = (Map<String, Object>) data;
				this.runtimeData = (Map<String, Object>) runtime;
			}
			else {
				this.state = payload;
				this.runtimeData = Map.of();
			}
			return this;
		}

		public Builder nodeId(String nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder nextNodeId(String nextNodeId) {
			this.nextNodeId = nextNodeId;
			return this;
		}

		public Checkpoint build() {
			return new Checkpoint(id, state, nodeId, nextNodeId, runtimeData);
		}

	}

}
