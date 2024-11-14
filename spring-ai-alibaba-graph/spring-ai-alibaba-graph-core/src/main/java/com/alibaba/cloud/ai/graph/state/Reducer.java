package com.alibaba.cloud.ai.graph.state;

import java.util.function.BiFunction;

public interface Reducer<T> extends BiFunction<T, T, T> {

}
