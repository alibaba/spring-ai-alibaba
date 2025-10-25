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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 工具接口的抽象实现类
 *
 * @param <T> 输入参数类型
 * @param <R> 输出参数类型
 * @author vlsmb
 * @since 2025/10/25
 */
public abstract class AbstractTool<T, R> implements BaseTool<T, R> {

    private static final Logger log = LoggerFactory.getLogger(AbstractTool.class);

    private final ExecutorService executor;

    /**
     * 存储工具调用的Entry
     */
    private final ConcurrentHashMap<String, Entry<R>> entryMap = new ConcurrentHashMap<>();

    /**
     * 记录当前工具调用的状态
     */
    private static class Entry<R> {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<Future<? extends R>> futureRef = new AtomicReference<>(null);
    }

    public AbstractTool(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * 流式处理元素的Consumer（如果工具支持则需要重写本方法）
     * @return Consumer
     */
    protected Consumer<? super R> streamConsumer() {
        return r -> {};
    }

    /**
     * 实现工具执行方法（用于生成ToolCallback）
     * @param t 工具输入参数
     * @param toolContext 工具上下文
     * @return 输出参数
     */
    @Override
    public R apply(T t, ToolContext toolContext) {
        String agentName = getAgentName(toolContext);
        Entry<R> entry = entryMap.compute(agentName, (k, v) -> new Entry<>());
        try {
            // 检测是否取消
            if(isCancelled(toolContext)) {
                log.info("AgentName: {} , Tool cancelled", agentName);
                return fallback(t, toolContext, new CancellationException("Tool cancelled"));
            }
            // 提交任务到线程池
            Future<R> future = executor.submit(() -> {
                try {
                    if(isCancelled(toolContext)) {
                        log.info("AgentName: {} , Tool cancelled", agentName);
                        throw new CancellationException("Tool cancelled");
                    }
                    // 执行工具，使用子类提供的Consumer
                    return applyInterruptible(t, toolContext, streamConsumer());
                } catch (InterruptedException e) {
                    log.info("AgentName: {} , Tool interrupted", agentName);
                    Thread.currentThread().interrupt();
                    return fallback(t, toolContext, e);
                } catch (Exception e) {
                    // 遇见异常，返回fallback结果
                    log.info("AgentName: {} , Tool Exception", agentName);
                    return fallback(t, toolContext, e);
                }
            });
            // 保存future到entry中，如果失败，取消当前任务
            // （应该不会有同一个Agent同一时间调用同一个工具）
            if(!entry.futureRef.compareAndSet(null, future)) {
                future.cancel(true);
                log.info("AgentName: {} , Tool already applied", agentName);
                throw new IllegalStateException("Tool already applied");
            }
            // 检测是否取消
            if (isCancelled(toolContext)) {
                return fallback(t, toolContext, new CancellationException("Tool cancelled"));
            }
            // 返回任务结果
            return future.get();
        } catch (Exception e) {
            // 遇见异常，返回fallback结果
            log.info("AgentName: {} , Tool Exception", agentName);
            return fallback(t, toolContext, e);
        } finally {
            // 移除当前entry
            entryMap.remove(agentName, entry);
        }
    }

    @Override
    public void cancel(ToolContext toolContext) {
        String agentName = getAgentName(toolContext);
        Entry<R> entry = entryMap.get(agentName);
        if (entry == null) {
            log.warn("AgentName: {} , Tool not applied but cancel method is called", agentName);
            return;
        }
        if (entry.cancelled.compareAndSet(false, true)) {
            // 如果entry中有future，则取消该future
            Future<? extends R> future = entry.futureRef.get();
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
            log.info("AgentName: {} , Tool cancelled successfully", agentName);
        }
    }

    @Override
    public boolean isCancelled(ToolContext toolContext) {
        String agentName = getAgentName(toolContext);
        Entry<R> entry = entryMap.get(agentName);
        if (entry == null) {
            return false;
        }
        return entry.cancelled.get();
    }

}
