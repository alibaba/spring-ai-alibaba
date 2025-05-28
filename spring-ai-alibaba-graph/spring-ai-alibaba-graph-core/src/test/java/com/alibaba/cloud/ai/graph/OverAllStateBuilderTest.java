package com.alibaba.cloud.ai.graph;


import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OverAllStateBuilderTest {

    @Test
    void testBuildWithMinimalConfiguration_shouldApplyDefaultInputKey() {
        // Arrange & Act
        OverAllState state = OverAllStateBuilder.builder().build();

        // Assert
        assertThat(state).isNotNull();
        assertThat(state.data()).isEmpty(); // No data added
        assertThat(state.containStrategy(OverAllState.DEFAULT_INPUT_KEY)).isTrue();
        assertThat(state.value("input", String.class)).isEmpty();
    }

    @Test
    void testBuildWithDataAndStrategy_shouldPreserveValues() {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("input", "Hello World");
        data.put("count", 1);

        // Act
        OverAllState state = OverAllStateBuilder.builder()
                .withData(data)
                .withKeyStrategy("input", new ReplaceStrategy())
                .setResume(true)
                .build();

        // Assert
        assertThat(state).isNotNull();
        assertThat(state.data()).containsEntry("input", "Hello World").containsEntry("count", 1);
        assertThat(state.isResume()).isTrue();
        assertThat(state.containStrategy("input")).isTrue();
        assertThat(state.value("input", String.class)).hasValue("Hello World");
    }

    @Test
    void testBuildWithCustomStrategyMap_shouldApplyAllStrategies() {
        // Arrange
        Map<String, KeyStrategy> strategies = new HashMap<>();
        strategies.put("input", new ReplaceStrategy());
        strategies.put("metadata", (oldVal, newVal) -> newVal + "-merged");

        Map<String, Object> data = Map.of("input", "initial", "metadata", "meta");

        // Act
        OverAllState state = OverAllStateBuilder.builder()
                .withData(data)
                .withKeyStrategies(strategies)
                .build();

        // Apply update
        Map<String, Object> update = Map.of("input", "new input", "metadata", "new meta");
        state.updateState(update);

        // Assert
        assertThat(state.value("input", String.class)).hasValue("new input");
        assertThat(state.value("metadata", String.class)).hasValue("new meta-merged");
    }

    @Test
    void testBuildWithoutExplicitInputStrategy_shouldAutoRegisterReplaceStrategy() {
        // Act
        OverAllState state = OverAllStateBuilder.builder()
                .putData("otherKey", "value")
                .build();

        // Assert
        assertThat(state.containStrategy(OverAllState.DEFAULT_INPUT_KEY)).isTrue();
        assertThat(state.value("otherKey", String.class)).hasValue("value");
    }

    @Test
    void testBuildWithResumeFlag_shouldSetResumeCorrectly() {
        // Act
        OverAllState state = OverAllStateBuilder.builder()
                .setResume(true)
                .build();

        // Assert
        assertThat(state.isResume()).isTrue();
    }
}

