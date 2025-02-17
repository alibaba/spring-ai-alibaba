package com.alibaba.cloud.ai.graph;


import com.alibaba.cloud.ai.graph.state.KeyStrategy;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;


@Data
public final class OverAllState {
    private final Map<String, Object> data;
    private final Map<String, KeyStrategy> keyStrategies;
    private final Boolean isResume;
    public static final String DEFAULT_INPUT_KEY = "inputs";


    public OverAllState(boolean isResume) {
        this.data = new HashMap<>();
        this.keyStrategies = new HashMap<>();
        this.isResume = isResume;
    }

    public OverAllState() {
        this.data = new HashMap<>();
        this.keyStrategies = new HashMap<>();
        this.isResume = false;
    }

    private OverAllState(Map<String, Object> data, Map<String, KeyStrategy> keyStrategies, Boolean isResume) {
        this.data = data;
        this.keyStrategies = keyStrategies;
        this.isResume = isResume;
    }


    public OverAllState copyWithResume() {
        return new OverAllState(this.data, this.keyStrategies, true);
    }


    public boolean isResume() {
        return this.isResume;
    }


    public OverAllState inputs(Map<String, Object> input) {
        if (CollectionUtils.isEmpty(input)) return this;
        this.data.putAll(input);
        addKeyAndStrategy(DEFAULT_INPUT_KEY, (oldValue, newValue) -> newValue);
        return this;
    }


    public OverAllState inputs(Object value) {
        if (value == null) return this;
        this.data.put(DEFAULT_INPUT_KEY, value);
        addKeyAndStrategy(DEFAULT_INPUT_KEY, (oldValue, newValue) -> newValue);
        return this;
    }

    public OverAllState addKeyAndStrategy(String key, KeyStrategy strategy) {
        this.keyStrategies.put(key, strategy);
        return this;
    }


    protected Map<String, Object> updateState(Map<String, Object> partialState) {
        Map<String, KeyStrategy> keyStrategies = keyStrategies();
        partialState.keySet()
                .stream()
                .filter(key -> keyStrategies.containsKey(key))
                .forEach(key -> {
                    this.data.put(
                            key,
                            keyStrategies.get(key).apply(value(key, null), partialState.get(key))
                    );
                });
        return data();
    }

    protected boolean keyVerify() {
        return hasCommonKey(this.data, getKeyStrategies());
    }

    private boolean hasCommonKey(Map<?, ?> map1, Map<?, ?> map2) {
        Set<?> keys1 = map1.keySet();
        for (Object key : map2.keySet()) {
            if (keys1.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates a state with the provided partial state. The merge function is used to
     * merge the current state value with the new value.
     *
     * @param state        the current state
     * @param partialState the partial state to update from
     * @return the updated state
     * @throws NullPointerException if state is null
     */
    public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState) {
        Objects.requireNonNull(state, "state cannot be null");
        if (partialState == null || partialState.isEmpty()) {
            return state;
        }

        return Stream.concat(state.entrySet().stream(), partialState.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, OverAllState::mergeFunction));
    }

    /**
     * Merges the current value with the new value using the appropriate merge function.
     *
     * @param currentValue the current value
     * @param newValue     the new value
     * @return the merged value
     */
    private static Object mergeFunction(Object currentValue, Object newValue) {
        return newValue;
    }


    public final Map<String, KeyStrategy> keyStrategies() {
        return unmodifiableMap(keyStrategies);
    }

    public final Map<String, Object> data() {
        return unmodifiableMap(data);
    }

    public final <T> Optional<T> value(String key) {
        return ofNullable((T) data().get(key));
    }

    public final <T> T value(String key, T defaultValue) {
        return (T) value(key).orElse(defaultValue);
    }


}
