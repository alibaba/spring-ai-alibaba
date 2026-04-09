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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;

/**
 * 可中断的 ModelHook 基类。
 *
 * <p>同时实现 {@link AsyncNodeActionWithConfig} 和 {@link InterruptableAction}，
 * ReactAgent 的 {@code initGraph()} 会识别此类型，将其作为完整节点注册到图中，
 * 而非像普通 ModelHook 那样包装为方法引用。</p>
 *
 * @see AsyncNodeActionWithConfig
 * @see InterruptableAction
 */
public abstract class AbstractInterruptableModelHook extends ModelHook
		implements AsyncNodeActionWithConfig, InterruptableAction {

}
