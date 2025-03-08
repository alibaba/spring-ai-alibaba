/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.graph.checkpoint;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.Channel;
import lombok.Data;
import lombok.ToString;

/**
 * Represents a checkpoint of an agent state.
 *
 * The checkpoint is an immutable object that holds an {@link AgentState} and a
 * {@code String} that represents the next state.
 *
 * The checkpoint is serializable and can be persisted and restored.
 *
 * @see AgentState
 */
@Data
@ToString
public class Checkpoint implements Serializable {

	private String id = UUID.randomUUID().toString();

	private Map<String, Object> state = null;

	private String nodeId = null;

	private String nextNodeId = null;

	private Checkpoint() {
	}

	public Checkpoint(Checkpoint checkpoint) {
		this.id = checkpoint.id;
		this.state = checkpoint.state;
		this.nodeId = checkpoint.nodeId;
		this.nextNodeId = checkpoint.nextNodeId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final Checkpoint result = new Checkpoint();

		public Builder id(String id) {
			result.id = id;
			return this;
		}

		public Builder state(OverAllState state) {
			result.state = state.data();
			return this;
		}

		public Builder state(Map<String, Object> state) {
			result.state = state;
			return this;
		}

		public Builder nodeId(String nodeId) {
			result.nodeId = nodeId;
			return this;
		}

		public Builder nextNodeId(String nextNodeId) {
			result.nextNodeId = nextNodeId;
			return this;
		}

		public Checkpoint build() {
			Objects.requireNonNull(result.id, "Checkpoint.id cannot be null");
			Objects.requireNonNull(result.state, "Checkpoint.state cannot be null");
			Objects.requireNonNull(result.nodeId, "Checkpoint.nodeId cannot be null");
			Objects.requireNonNull(result.nextNodeId, "Checkpoint.nextNodeId cannot be null");

			return result;

		}

	}

	public Checkpoint updateState(Map<String, Object> values, Map<String, KeyStrategy> channels) {

		Checkpoint result = new Checkpoint(this);
		result.state = OverAllState.updateState(state, values, channels);
		return result;
	}

}
