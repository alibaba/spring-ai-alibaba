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

public abstract class AbstractTool<T, R> implements BaseTool<T, R> {

    private static final Logger log = LoggerFactory.getLogger(AbstractTool.class);

    private final ExecutorService executor;

    private final ConcurrentHashMap<String, Entry<R>> entryMap = new ConcurrentHashMap<>();

    private static class Entry<R> {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<Future<? extends R>> futureRef = new AtomicReference<>(null);
    }

    public AbstractTool(ExecutorService executor) {
        this.executor = executor;
    }

    protected Consumer<? super R> streamConsumer() {
        return null;
    }

    @Override
    public R apply(T t, ToolContext toolContext) {
        String agentName = getAgentName(toolContext);
        Entry<R> entry = entryMap.computeIfAbsent(agentName, k -> new Entry<>());
        try {
            if(isCancelled(toolContext)) {
                log.info("AgentName: {} , Tool cancelled", agentName);
                return fallback(t, toolContext, new CancellationException("Tool cancelled"));
            }
            Future<R> future = executor.submit(() -> {
                try {
                    if(isCancelled(toolContext)) {
                        log.info("AgentName: {} , Tool cancelled", agentName);
                        throw new CancellationException("Tool cancelled");
                    }
                    return applyInterruptible(t, toolContext, streamConsumer());
                } catch (InterruptedException e) {
                    log.info("AgentName: {} , Tool interrupted", agentName);
                    Thread.currentThread().interrupt();
                    return fallback(t, toolContext, e);
                } catch (Exception e) {
                    log.info("AgentName: {} , Tool Exception", agentName);
                    return fallback(t, toolContext, e);
                }
            });
            if(!entry.futureRef.compareAndSet(null, future)) {
                future.cancel(true);
                log.info("AgentName: {} , Tool already applied", agentName);
                throw new IllegalStateException("Tool already applied");
            }
            if (isCancelled(toolContext)) {
                return fallback(t, toolContext, new CancellationException("Tool cancelled"));
            }
            return future.get();
        } catch (Exception e) {
            return fallback(t, toolContext, e);
        } finally {
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
