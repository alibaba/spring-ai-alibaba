package com.alibaba.cloud.ai.graph;

import lombok.Getter;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class CompileConfig {

	private BaseCheckpointSaver checkpointSaver;

	@Getter
	private String[] interruptBefore = {};

	@Getter
	private String[] interruptAfter = {};

	public Optional<BaseCheckpointSaver> checkpointSaver() {
		return ofNullable(checkpointSaver);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final CompileConfig config = new CompileConfig();

		public Builder checkpointSaver(BaseCheckpointSaver checkpointSaver) {
			this.config.checkpointSaver = checkpointSaver;
			return this;
		}

		public Builder interruptBefore(String... interruptBefore) {
			this.config.interruptBefore = interruptBefore;
			return this;
		}

		public Builder interruptAfter(String... interruptAfter) {
			this.config.interruptAfter = interruptAfter;
			return this;
		}

		public CompileConfig build() {
			return config;
		}

	}

	private CompileConfig() {
	}

}
