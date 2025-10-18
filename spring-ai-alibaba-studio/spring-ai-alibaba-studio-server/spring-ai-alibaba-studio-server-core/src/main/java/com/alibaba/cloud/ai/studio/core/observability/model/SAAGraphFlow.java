package com.alibaba.cloud.ai.studio.core.observability.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.StateGraph;

/**
 * Represents a graph flow, including its metadata and the underlying state graph.
 *
 * @param graphId The unique identifier of the graph flow.
 * @param title The human-readable title of the graph flow.
 * @param description A detailed description of the graph flow's purpose.
 * @param tags A list of tags for categorization.
 * @param ownerID The identifier of the owner of the graph flow.
 * @param compileConfig The configuration for compiling the graph.
 * @param stateGraph The actual state graph object containing the execution logic.
 *
 */
public record SAAGraphFlow(String graphId, String title, String description, List<String> tags, String ownerID,
		CompileConfig compileConfig, StateGraph stateGraph) {

	public SAAGraphFlow {
		Objects.requireNonNull(stateGraph, "stateGraph is NULL!");
	}

	public static SAAGraphFlow.Builder builder() {
		return new SAAGraphFlow.Builder();
	}
	public static class Builder {

		private String graphId;

		private String description;

		private List<String> tags = new ArrayList<>();

		private String ownerID;

		private String title = null;

		private CompileConfig compileConfig;

		private StateGraph stateGraph;

		public Builder id(String graphId) {
			this.graphId = graphId;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder tags(List<String> tags) {
			this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
			return this;
		}

		public Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public Builder addTag(String tag) {
			if (tag != null && !tag.isBlank()) {
				this.tags.add(tag);
			}
			return this;
		}

		public Builder ownerID(String ownerID) {
			this.ownerID = ownerID;
			return this;
		}

		public Builder stateGraph(StateGraph stateGraph) {
			this.stateGraph = stateGraph;
			return this;
		}

		/**
		 * Constructs the final, immutable SAAGraphFlow object. Performs validation to
		 * ensure required fields are set.
		 * @return A new instance of SAAGraphFlow.
		 * @throws NullPointerException if required fields (id, title, stateGraph) are
		 * null.
		 */
		public SAAGraphFlow build() {
			// Perform validation for required fields before creating the object.
			Objects.requireNonNull(graphId, "GraphID cannot be null");
			Objects.requireNonNull(title, "Title cannot be null");
			Objects.requireNonNull(stateGraph, "State cannot be null");
			// Ensure the list of tags is immutable in the final record.
			List<String> immutableTags = List.copyOf(this.tags);
			return new SAAGraphFlow(graphId, title, description, immutableTags, ownerID, compileConfig, stateGraph);
		}
	}

}
