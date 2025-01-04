package com.alibaba.cloud.ai.graph;

import lombok.ToString;

import java.util.Objects;
import java.util.Optional;

@ToString
public final class RunnableConfig {

	private String userId;

	private String graphId;

	private String threadId;

	private String checkPointId;

	private Boolean isResume = Boolean.FALSE;

	private String nextNode;

	private CompiledGraph.StreamMode streamMode = CompiledGraph.StreamMode.VALUES;

	public CompiledGraph.StreamMode streamMode() {
		return streamMode;
	}

	public Optional<String> threadId() {
		return Optional.ofNullable(threadId);
	}

	public Optional<Boolean> isResume() {
		return Optional.ofNullable(isResume);
	}

	public Optional<String> checkPointId() {
		return Optional.ofNullable(checkPointId);
	}

	public Optional<String> nextNode() {
		return Optional.ofNullable(nextNode);
	}

	/**
	 * Create a new RunnableConfig with the same attributes as this one but with a
	 * different {@link CompiledGraph.StreamMode}.
	 * @param streamMode the new stream mode
	 * @return a new RunnableConfig with the updated stream mode
	 */
	public RunnableConfig withStreamMode(CompiledGraph.StreamMode streamMode) {
		if (this.streamMode == streamMode) {
			return this;
		}
		RunnableConfig newConfig = new RunnableConfig(this);
		newConfig.streamMode = streamMode;
		return newConfig;
	}

	public RunnableConfig withCheckPointId(String checkPointId) {
		if (Objects.equals(this.checkPointId, checkPointId)) {
			return this;
		}
		RunnableConfig newConfig = new RunnableConfig(this);
		newConfig.checkPointId = checkPointId;
		return newConfig;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(RunnableConfig config) {
		return new Builder(config);
	}

	public static class Builder {

		private final RunnableConfig config;

		Builder() {
			this.config = new RunnableConfig();
		}

		Builder(RunnableConfig config) {
			this.config = new RunnableConfig(config);
		}

		public Builder threadId(String threadId) {
			this.config.threadId = threadId;
			return this;
		}

		public Builder graphId(String graphId) {
			this.config.graphId = graphId;
			return this;
		}

		public Builder resume(){
			this.config.isResume = true;
			return this;
		}

		public Builder userId(String userId) {
			this.config.userId = userId;
			return this;
		}

		public Builder checkPointId(String checkPointId) {
			this.config.checkPointId = checkPointId;
			return this;
		}

		public Builder nextNode(String nextNode) {
			this.config.nextNode = nextNode;
			return this;
		}

		public Builder streamMode(CompiledGraph.StreamMode streamMode) {
			this.config.streamMode = streamMode;
			return this;
		}

		public RunnableConfig build() {
			return config;
		}

	}

	private RunnableConfig(RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		this.threadId = config.threadId;
		this.checkPointId = config.checkPointId;
		this.nextNode = config.nextNode;
		this.streamMode = config.streamMode;
	}

	private RunnableConfig() {
	}

}
