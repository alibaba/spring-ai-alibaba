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
package com.alibaba.cloud.ai.graph.streaming;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Map;

import static java.lang.String.format;

public class StreamingOutput<T> extends NodeOutput {

    private final String chunk;

    private final Message message;

    @JsonIgnore
    private final T originData;

    private OutputType outputType;

    private Map<String, Serializable> metadata;

    private StreamingOutput(Builder<T> builder) {
        super(builder.node, builder.agentName, builder.tokenUsage, builder.state);
        this.chunk = builder.chunk;
        this.message = builder.message;
        this.originData = builder.originData;
        this.metadata = builder.metadata;
        this.outputType = builder.outputType;
        trySetTokenUsage();
    }

    private void trySetTokenUsage() {
        if (originData != null) {
            if (originData instanceof ChatResponse chatResponse) {
                setTokenUsage(chatResponse.getMetadata().getUsage());
            } else if (originData instanceof Usage usage) {
                setTokenUsage(usage);
            }
        } else if (metadata != null && metadata.containsKey("usage")) {
            Object usageObj = metadata.get("usage");
            if (usageObj instanceof Usage usage) {
                setTokenUsage(usage);
            }
        }
    }


    public String chunk() {
        return chunk;
    }

    @JsonIgnore
    public T getOriginData() {
        return originData;
    }

    public Map<String, Serializable> getMetadata() {
        return metadata;
    }

    public Message message() {
        return message;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    @Override
    public String toString() {
        if (node() == null) {
            return format("StreamingOutput{message=%s, chunk=%s}", message(), chunk());
        }
        return format("StreamingOutput{node=%s, agent=%s, message=%s, chunk=%s, tokenUsage=%s, state=%s, subGraph=%s}",
                node(), agent(), message(), chunk(), tokenUsage(), state(), isSubGraph());
    }

    public static <T> StreamingOutput.Builder<T> builder() {
        return new StreamingOutput.Builder<>();
    }

    public static class Builder<T> {
        public String chunk;
        private Message message;
        private T originData;
        private Map<String, Serializable> metadata;
        private OutputType outputType;

        private String node;
        private OverAllState state;
        private String agentName;
        private Usage tokenUsage;


        public Builder() {
        }

        public Builder<T> message(Message message) {
            this.message = message;
            return this;
        }

        public Builder<T> originData(T originData) {
            this.originData = originData;
            return this;
        }

        public Builder<T> metadata(Map<String, Serializable> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder<T> outputType(OutputType outputType) {
            this.outputType = outputType;
            return this;
        }

        public Builder<T> node(String node) {
            this.node = node;
            return this;
        }

        public Builder<T> state(OverAllState state) {
            this.state = state;
            return this;
        }

        public Builder<T> agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder<T> tokenUsage(Usage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return this;
        }

        public Builder<T> chunk(String chunk) {
            this.chunk = chunk;
            return this;
        }

        public StreamingOutput<T> build() {
            StreamingOutput<T> output = new StreamingOutput<T>(this);
            output.trySetTokenUsage();
            return output;
        }
    }

}
