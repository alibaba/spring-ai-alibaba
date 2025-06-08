/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VariableAggregatorNode implements NodeAction {

    private final List<String> inputKeys;
    private final String outputKey;

    private VariableAggregatorNode(List<String> inputKeys, String outputKey) {
        this.inputKeys = inputKeys;
        this.outputKey = outputKey;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> aggregated = new HashMap<>();
        for (String key : this.inputKeys) {
            Object value = state.value(key).orElse(null);
            aggregated.put(key, value);
        }
        return Map.of(outputKey, aggregated);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> inputKeys;
        private String outputKey;

        public Builder inputKeys(List<String> inputKeys) {
            this.inputKeys = inputKeys;
            return this;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public VariableAggregatorNode build() {
            Objects.requireNonNull(this.inputKeys, "inputKeys cannot be null");
            Objects.requireNonNull(this.outputKey, "outputKey cannot be null");
            return new VariableAggregatorNode(this.inputKeys, this.outputKey);
        }
    }
}
