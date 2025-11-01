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

package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 工具接口，定义工具方法，支持中断的方法，流式输出方法以及默认值返回方法。
 * ToolContext中必须要有agent_name的值，否则会报错
 *
 * @param <T> 输入参数类型
 * @param <R> 输出结果类型
 * @author vlsmb
 * @since 2025/10/25
 */
public interface BaseTool<T, R> extends BiFunction<T, ToolContext, R> {

    String AGENT_NAME = "agent_name";

    /**
     * 工具方法，来自BiFunction，已经在AbstractTool中实现（用于定义toolCallback）
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @return 输出结果
     */
    R apply(T t, ToolContext toolContext);

    /**
     * 允许中断的apply方法，实现类需要需要实现此方法
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @param streamConsumer 每一个元素的Consumer（如果工具不支持流式可以置空）
     * @return 输出结果
     */
    R applyInterruptible(T t, ToolContext toolContext, Consumer<? super R> streamConsumer) throws Exception;

    default R applyInterruptible(T t, ToolContext toolContext) throws Exception {
        return applyInterruptible(t, toolContext, r -> {});
    }

    /**
     * 返回包装后的ToolCallback
     */
    ToolCallback toolCallback();

    /**
     * 流式方法，如果工具支持则需要重写
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @return 最终结果
     */
    default Flux<R> applyStream(T t, ToolContext toolContext) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * 失败后返回的默认值
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @param throwable 异常，如果为CancellationException则表示工具被取消
     * @return 默认值
     */
    R fallback(T t, ToolContext toolContext, Throwable throwable);

    /**
     * 取消正在调用的工具，立即返回fallback的值
     * @param toolContext 工具上下文，需要有agent_name
     */
    void cancel(ToolContext toolContext);

    /**
     * 判断工具是否被取消
     * @param toolContext 工具上下文，需要有agent_name
     * @return 是否被取消
     */
    boolean isCancelled(ToolContext toolContext);

    /**
     * 获取工具名称，从工具上下文中获取agent_name的值
     * @param toolContext 工具上下文
     * @return 工具名称
     */
    default String getAgentName(ToolContext toolContext) {
        Object obj = toolContext.getContext().get(AGENT_NAME);
        if(!(obj instanceof String agentName)) {
            throw new IllegalArgumentException("agent_name must be a string");
        }
        return agentName;
    }
}
