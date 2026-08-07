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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AppendStrategyMutationRegressionTest {

    // https://github.com/alibaba/spring-ai-alibaba/issues/4757
    @Test
    void issue4757_singleValueAppend_doesNotMutateExistingList() {
        AppendStrategy strategy = new AppendStrategy();
        List<Object> oldValues = new ArrayList<>(List.of("question"));

        Object result = strategy.apply(oldValues, "user-answer");

        assertEquals(List.of("question"), oldValues);
        assertEquals(List.of("question", "user-answer"), result);
        assertNotSame(oldValues, result);
    }
}
