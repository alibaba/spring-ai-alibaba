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

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.List;
import java.util.Map;

/**
 * LoopAgent的循环策略，用来控制LoopAgent的行为。
 * 这部分相当于定义了LoopAgent对应StateGraph的loopInitNode和loopDispatchNode。
 * 在使用的时候可以直接使用LoopMode提供的内置策略，如果需要自定义循环逻辑，可以实现本接口。
 * @author vlsmb
 * @since 2025/11/1
 */
public interface LoopStrategy {

    int ITERABLE_ELEMENT_COUNT = 1000;

    String LOOP_FLAG_PREFIX = "__loop_flag__";

    String LOOP_LIST_PREFIX = "__loop_list__";

    String LOOP_COUNT_PREFIX = "__loop_count__";

    String INIT_NODE_NAME = "_loop_init__";

    String DISPATCH_NODE_NAME = "_loop_dispatch__";

    String MESSAGE_KEY = "messages";

    Map<String, Object> loopInit(OverAllState state);

    Map<String, Object> loopDispatch(OverAllState state);
    
    default String uniqueKey() {
        return String.valueOf(System.identityHashCode(this));
    }

    default List<String> tempKeys() {
        return List.of(
                loopFlagKey(),
                loopListKey(),
                loopCountKey()
        );
    }

    default int maxLoopCount() {
        return ITERABLE_ELEMENT_COUNT;
    }

    default String loopFlagKey() {
        return LOOP_FLAG_PREFIX + uniqueKey();
    }

    default String loopListKey() {
        return LOOP_LIST_PREFIX + uniqueKey();
    }

    default String loopCountKey() {
        return LOOP_COUNT_PREFIX + uniqueKey();
    }

    default String loopInitNodeName() {
        return INIT_NODE_NAME + uniqueKey();
    }

    default String loopDispatchNodeName() {
        return DISPATCH_NODE_NAME + uniqueKey();
    }

}
