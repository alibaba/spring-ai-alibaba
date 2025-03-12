/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.state;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;
import java.util.function.Function;

/**
 * A factory interface for creating instances of {@link AgentState}.
 *
 */
public interface AgentStateFactory<T> extends Function<Map<String, Object>, T> {

}
