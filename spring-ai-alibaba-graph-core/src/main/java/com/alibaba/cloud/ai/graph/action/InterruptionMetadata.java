package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.HasMetadata;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Represents the metadata associated with a graph execution interruption. This class is
 * immutable and captures the state of the graph at the point of interruption, the node
 * where the interruption occurred, and any additional custom metadata.
 *
 */
public final class InterruptionMetadata implements HasMetadata<InterruptionMetadata.Builder> {

	private final String nodeId;

	private final OverAllState state;

	private final Map<String, Object> metadata;

	private InterruptionMetadata(Builder builder) {
		this.metadata = builder.metadata();
		this.nodeId = requireNonNull(builder.nodeId, "nodeId cannot be null!");
		this.state = requireNonNull(builder.state, "state cannot be null!");
	}

	/**
	 * Gets the ID of the node where the interruption occurred.
	 * @return the node ID
	 */
	public String nodeId() {
		return nodeId;
	}

	/**
	 * Gets the state of the graph at the time of the interruption.
	 * @return the agent state
	 */
	public OverAllState state() {
		return state;
	}

	/**
	 * Retrieves a metadata value associated with the specified key.
	 * @param key the key whose associated value is to be returned
	 * @return an {@link Optional} containing the value to which the specified key is
	 * mapped, or an empty {@link Optional} if this metadata contains no mapping for the
	 * key.
	 */
	@Override
	public Optional<Object> metadata(String key) {
		return ofNullable(metadata).map(m -> m.get(key));
	}

	@Override
	public String toString() {
		return String.format("""
				InterruptionMetadata{
				\tnodeId='%s',
				\tstate=%s,
				\tmetadata=%s
				}""", nodeId, state, CollectionsUtils.toString(metadata));
	}

	/**
	 * Creates a new builder for {@link InterruptionMetadata}.
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder(String nodeId, OverAllState state) {
		return new Builder(nodeId, state);
	}

	/**
	 * A builder for creating instances of {@link InterruptionMetadata}.
	 *
	 */
	public static class Builder extends HasMetadata.Builder<Builder> {

		final String nodeId;

		final OverAllState state;

		/**
		 * Constructs a new builder.
		 *
		 */
		public Builder(String nodeId, OverAllState state) {
			this.nodeId = nodeId;
			this.state = state;
		}

		/**
		 * Builds the {@link InterruptionMetadata} instance.
		 * @return a new, immutable {@link InterruptionMetadata} instance
		 */
		public InterruptionMetadata build() {
			return new InterruptionMetadata(this);
		}

	}

}
