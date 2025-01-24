package com.alibaba.cloud.ai.graph;


import com.alibaba.cloud.ai.graph.state.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.NodeState;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;


@Data
public class OverAllState {
    private final Map<String, Object> data;
    private final Map<String, KeyStrategy> keyStrategies;

    public OverAllState() {
        this.data = new HashMap<>();
        this.keyStrategies = new HashMap<>();
    }


    public boolean isResume(){
        return data.isEmpty();
    }


    public OverAllState initData(Map<String,Object> input){
        if (CollectionUtils.isEmpty(input)) return this;
        this.data.putAll(input);
        return this;
    }

    public OverAllState addKeyStrategy(String key, KeyStrategy strategy) {
        this.keyStrategies.put(key, strategy);
        return this;
    }


    protected Map<String, Object> updateState(Map<String, Object> partialState) {
        Map<String, KeyStrategy> keyStrategies = keyStrategies();
        partialState.keySet()
                .stream()
                .filter(key -> keyStrategies.containsKey(key))
                .forEach(key -> {
                    Object apply = keyStrategies.get(key).apply(value(key, null), partialState.get(key));
                    this.data.put(key, apply);
                });
        return data();
    }

    public KeyStrategy getKeyStrategy(String key) {
        return this.keyStrategies.get(key);
    }

    public final Map<String,KeyStrategy> keyStrategies(){
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
