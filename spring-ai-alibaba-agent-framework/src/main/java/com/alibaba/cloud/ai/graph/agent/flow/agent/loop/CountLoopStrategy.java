package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

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
