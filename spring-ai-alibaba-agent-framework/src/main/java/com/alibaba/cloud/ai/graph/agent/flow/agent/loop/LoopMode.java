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

package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import org.springframework.ai.chat.messages.Message;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Built-in loop strategies for LoopAgent
 *
 * @author vlsmb
 * @since 2025/11/1
 */
public final class LoopMode {
    private LoopMode() {
        throw new UnsupportedOperationException();
    }

    public static CountLoopStrategy count(int maxCount) {
        return new CountLoopStrategy(maxCount);
    }

    public static ArrayLoopStrategy array() {
        return new ArrayLoopStrategy();
    }

    public static ArrayLoopStrategy array(Converter<List<Message>, List<?>> converter) {
        return new ArrayLoopStrategy(converter);
    }

    public static ConditionLoopStrategy condition(Predicate<List<Message>> messagePredicate) {
        return new ConditionLoopStrategy(messagePredicate);
    }
}
