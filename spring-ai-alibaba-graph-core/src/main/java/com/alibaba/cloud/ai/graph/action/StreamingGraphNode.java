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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface StreamingGraphNode extends NodeAction {

	/**
	 * 执行流式节点操作，返回响应式数据流。 这是流式节点的核心方法，用于生成连续的数据流。
	 * @param state 图的整体状态
	 * @return 包含图输出数据的响应式流
	 * @throws Exception 执行过程中可能出现的异常
	 */
	Flux<Map<String, Object>> executeStreaming(OverAllState state) throws Exception;

	/**
	 * 默认实现，通过流式方法的第一个元素来提供同步兼容性。 该方法确保现有系统的向后兼容性。
	 * @param state 图的整体状态
	 * @return 同步执行结果
	 * @throws Exception 执行过程中可能出现的异常
	 */
	@Override
	default Map<String, Object> apply(OverAllState state) throws Exception {
		return executeStreaming(state).blockFirst();
	}

	/**
	 * 判断是否为流式节点。 用于GraphEngine区分同步和流式节点的执行方式。
	 * @return 总是返回true，表示这是一个流式节点
	 */
	default boolean isStreaming() {
		return true;
	}

}
