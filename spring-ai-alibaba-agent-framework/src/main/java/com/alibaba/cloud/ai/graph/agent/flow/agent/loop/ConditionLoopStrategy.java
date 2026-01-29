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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Conditional loop strategy that retries until the Predicate is satisfied or the maximum count is reached.
 *
 * @author vlsmb
 * @since 2025/11/1
 */
public class ConditionLoopStrategy implements LoopStrategy {

    private final Predicate<List<Message>> messagePredicate;

    private final int maxCount = maxLoopCount();

    public ConditionLoopStrategy(Predicate<List<Message>> messagePredicate) {
        this.messagePredicate = messagePredicate;
    }

    @Override
    public Map<String, Object> loopInit(OverAllState state) {
        return Map.of(loopCountKey(), 0, loopFlagKey(), true);
    }

    @Override
    public Map<String, Object> loopDispatch(OverAllState state) {
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) state.value(LoopStrategy.MESSAGE_KEY).orElse(List.of());
        if(messagePredicate.test(messages)) {
            return Map.of(loopFlagKey(), false);
        } else {
            int count = state.value(loopCountKey(), maxCount);
            if(count < maxCount) {
                return Map.of(loopCountKey(), count + 1, loopFlagKey(), true);
            } else {
                return Map.of(LoopStrategy.MESSAGE_KEY, new SystemMessage("Max loop count reached"), loopFlagKey(), false);
            }
        }
    }
}
