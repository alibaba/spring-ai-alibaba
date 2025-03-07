package com.alibaba.cloud.ai.graph.checkpoint.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import jodd.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

import static com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant.MEMORY;

public class SaverConfig {

	private Map<String, BaseCheckpointSaver> savers = new ConcurrentHashMap<>();

	@Setter
	@Getter
	private String type = MEMORY;

	public static Builder builder() {
		return new Builder();
	}

	public SaverConfig register(String type, BaseCheckpointSaver saver) {
		// or computeIfPresent?
		savers.computeIfAbsent(type, s -> saver);
		return this;
	}

	public BaseCheckpointSaver get(String type) {
		if (StringUtil.isEmpty(type))
			throw new IllegalArgumentException("type isn't allow null");
		return savers.get(type);
	}

	public BaseCheckpointSaver get() {
		if (savers.size() == 1) {
			return savers.values().iterator().next();
		}
		return savers.get(this.type);
	}

	public static class Builder {

		private final SaverConfig config;

		Builder() {
			this.config = new SaverConfig();
		}

		public Builder type(String type) {
			this.config.type = type;
			return this;
		}

		public Builder register(String type, BaseCheckpointSaver saver) {
			this.config.register(type, saver);
			return this;
		}

		public SaverConfig build() {
			return this.config;
		}

	}

}
