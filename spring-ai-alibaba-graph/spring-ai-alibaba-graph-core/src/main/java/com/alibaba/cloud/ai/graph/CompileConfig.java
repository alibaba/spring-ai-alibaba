package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import lombok.Getter;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class CompileConfig {

    private SaverConfig saverConfig;

    @Getter
    private String[] interruptBefore = {};

    @Getter
    private String[] interruptAfter = {};

    public Optional<BaseCheckpointSaver> checkpointSaver(String type) {
        return ofNullable(saverConfig.get(type));
    }

    public Optional<BaseCheckpointSaver> checkpointSaver() {
        return ofNullable(saverConfig.get());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final CompileConfig config = new CompileConfig();


        public Builder saverConfig(SaverConfig saverConfig) {
            this.config.saverConfig = saverConfig;
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
            if (config.saverConfig == null) {
                throw new NullPointerException("saverConfig isn't allow null");
            }
            return config;
        }

    }

    private CompileConfig() {
    }

}
