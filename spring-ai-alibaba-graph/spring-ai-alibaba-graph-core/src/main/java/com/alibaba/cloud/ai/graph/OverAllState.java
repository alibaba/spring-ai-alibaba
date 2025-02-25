package com.alibaba.cloud.ai.graph;

import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;


/**
 * The type Over all state.
 */
@ToString
public final class OverAllState implements Serializable {
    private final Map<String, Object> data;
    private final Map<String, KeyStrategy> keyStrategies;
    private  Boolean isResume;
    /**
     * The constant DEFAULT_INPUT_KEY.
     */
    public static final String DEFAULT_INPUT_KEY = "input";

    public void reset(){
        this.data.clear();
    }

    public Optional<OverAllState> snapShot(){
        return Optional.of(new OverAllState(new HashMap<>(this.data), new HashMap<>(this.keyStrategies), this.isResume));
    }

    /**
     * Instantiates a new Over all state.
     *
     * @param isResume the is resume
     */
    public OverAllState(boolean isResume) {
        this.data = new HashMap<>();
        this.keyStrategies = new HashMap<>();
        this.isResume = isResume;
    }

    /**
     * Instantiates a new Over all state.
     *
     * @param data the data
     */
    public OverAllState(Map<String, Object> data) {
        this.data = data;
        this.keyStrategies = new HashMap<>();
        this.isResume = false;
    }

    /**
     * Instantiates a new Over all state.
     */
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


    /**
     * Copy with resume over all state.
     *
     * @return the over all state
     */
    public OverAllState copyWithResume() {
        return new OverAllState(this.data, this.keyStrategies, true);
    }


    public void withResume(){
        this.isResume = true;
    }


    public void withOutResume(){
        this.isResume = false;
    }


    /**
     * Is resume boolean.
     *
     * @return the boolean
     */
    public boolean isResume() {
        return this.isResume;
    }


    /**
     * Inputs over all state.
     *
     * @param input the input
     * @return the over all state
     */
    public OverAllState input(Map<String, Object> input) {
        if (CollectionUtils.isEmpty(input)) return this;
        this.data.putAll(input);
        addKeyAndStrategy(DEFAULT_INPUT_KEY, (oldValue, newValue) -> newValue);
        return this;
    }


    /**
     * Inputs over all state.
     *
     * @param value the value
     * @return the over all state
     */
    public OverAllState input(Object value) {
        if (value == null) return this;
        this.data.put(DEFAULT_INPUT_KEY, value);
        addKeyAndStrategy(DEFAULT_INPUT_KEY, (oldValue, newValue) -> newValue);
        return this;
    }

    /**
     * Add key and strategy over all state.
     *
     * @param key      the key
     * @param strategy the strategy
     * @return the over all state
     */
    public OverAllState addKeyAndStrategy(String key, KeyStrategy strategy) {
        this.keyStrategies.put(key, strategy);
        return this;
    }


    /**
     * Update state map.
     *
     * @param partialState the partial state
     * @return the map
     */
    public Map<String, Object> updateState(Map<String, Object> partialState) {
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

    /**
     * Key verify boolean.
     *
     * @return the boolean
     */
    protected boolean keyVerify() {
        return hasCommonKey(this.data, getKeyStrategies());
    }

    private Map<?,?> getKeyStrategies() {
        return this.keyStrategies;
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


    public static Map<String, Object> updateState(Map<String, Object> state, Map<String, Object> partialState, Map<String, KeyStrategy> keyStrategies) {
        Objects.requireNonNull(state, "state cannot be null");
        if (partialState == null || partialState.isEmpty()) {
            return state;
        }

        return Stream.concat(state.entrySet().stream(), partialState.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> {
                    String key = (Stream.of(state.entrySet(), partialState.entrySet())
                            .flatMap(Set::stream)
                            .filter(entry -> entry.getValue() == oldValue || entry.getValue() == newValue)
                            .findFirst()
                            .orElseThrow()).getKey();
                    KeyStrategy strategy = keyStrategies.getOrDefault(key, OverAllState::mergeFunction);
                    return strategy.apply(oldValue, newValue);
                }));
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


    /**
     * Key strategies map.
     *
     * @return the map
     */
    public final Map<String, KeyStrategy> keyStrategies() {
        return unmodifiableMap(keyStrategies);
    }

    /**
     * Data map.
     *
     * @return the map
     */
    public final Map<String, Object> data() {
        return unmodifiableMap(data);
    }

    /**
     * Value optional.
     *
     * @param <T> the type parameter
     * @param key the key
     * @return the optional
     */
    public final <T> Optional<T> value(String key) {
        return ofNullable((T) data().get(key));
    }

    /**
     * Value t.
     *
     * @param <T>          the type parameter
     * @param key          the key
     * @param defaultValue the default value
     * @return the t
     */
    public final <T> T value(String key, T defaultValue) {
        return (T) value(key).orElse(defaultValue);
    }


}

