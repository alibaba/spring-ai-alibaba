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
package com.alibaba.cloud.ai.graph.state.strategy;

import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link AppendStrategy}.
 */
class AppendStrategyTest {

    private final AppendStrategy strategy = new AppendStrategy();

    @Test
    void nullNewValueReturnsOldValueUnchanged() {
        Object oldValue = new ArrayList<>(List.of(1, 2));
        assertSame(oldValue, strategy.apply(oldValue, null));
    }

    @Test
    void nullOldValueWrapsScalarIntoNewList() {
        assertEquals(List.of("x"), strategy.apply(null, "x"));
    }

    @Test
    void nullOldValueExpandsListNewValue() {
        assertEquals(List.of("a", "b"), strategy.apply(null, List.of("a", "b")));
    }

    @Test
    void appendsScalarToExistingList() {
        Object result = strategy.apply(new ArrayList<>(List.of(1, 2)), 3);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void concatenatesTwoListsKeepingDuplicatesByDefault() {
        Object result = strategy.apply(new ArrayList<>(List.of(1, 2)), List.of(2, 3));
        assertEquals(List.of(1, 2, 2, 3), result);
    }

    @Test
    void dropsDuplicatesWhenNotAllowed() {
        AppendStrategy noDuplicates = new AppendStrategy(false);
        Object result = noDuplicates.apply(new ArrayList<>(List.of(1, 2)), List.of(2, 3));
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void emptyNewListLeavesOldValueUnchanged() {
        Object oldValue = new ArrayList<>(List.of(1, 2));
        assertSame(oldValue, strategy.apply(oldValue, List.of()));
    }

    @Test
    void replaceAllWithReplacesEntireValue() {
        Object result = strategy.apply(new ArrayList<>(List.of(1, 2)), ReplaceAllWith.of(List.of("x", "y")));
        assertEquals(List.of("x", "y"), result);
    }

    @Test
    void unwrapsOptionalOldValueBeforeAppending() {
        Object result = strategy.apply(Optional.of(new ArrayList<>(List.of(1, 2))), List.of(3));
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void acceptsArrayAsNewValue() {
        Object result = strategy.apply(null, new String[] {"a", "b"});
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void acceptsCollectionAsNewValue() {
        Object result = strategy.apply(null, Set.of("a"));
        assertEquals(List.of("a"), result);
    }

    @Test
    void removeIdentifierRemovesMatchingElementFromList() {
        AppenderChannel.RemoveIdentifier<Object> removeB = (element, atIndex) -> "b".equals(element) ? 0 : 1;
        Object result = strategy.apply(new ArrayList<>(List.of("a", "b", "c")), removeB);
        assertEquals(List.of("a", "c"), result);
    }
}
