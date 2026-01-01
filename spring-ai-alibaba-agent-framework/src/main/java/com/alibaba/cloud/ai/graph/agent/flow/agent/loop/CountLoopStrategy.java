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

package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

/**
 * Fixed count loop strategy
 *
 * @author vlsmb
 * @since 2025/11/1
 */
public class CountLoopStrategy implements LoopStrategy {

    private final int maxCount;

    public CountLoopStrategy(int maxCount) {
        this.maxCount = Math.min(maxCount, maxLoopCount());
    }

    @Override
    public Map<String, Object> loopInit(OverAllState state) {
        return Map.of(loopCountKey(), 0, loopFlagKey(), maxCount > 0);
    }

    @Override
    public Map<String, Object> loopDispatch(OverAllState state) {
        int count = state.value(loopCountKey(), maxCount);
        if (count < maxCount) {
            return Map.of(loopCountKey(), count + 1, loopFlagKey(), true);
        } else {
            return Map.of(loopFlagKey(), false);
        }
    }
}
