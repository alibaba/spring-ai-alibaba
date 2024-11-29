package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.Channel;
import lombok.*;

import java.io.Serializable;
import java.util.*;

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

		public Builder state(AgentState state) {
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

	public Checkpoint updateState(Map<String, Object> values, Map<String, Channel<?>> channels) {

		Checkpoint result = new Checkpoint(this);
		result.state = AgentState.updateState(state, values, channels);
		return result;
	}

}
