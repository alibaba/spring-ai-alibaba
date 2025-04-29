package com.alibaba.cloud.ai.dashscope.common;

import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author logic.wu
 */
public class StreamInterruptManager {
    private static final ConcurrentHashMap<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();

    public static void register(String requestId) {
        stopFlags.put(requestId, new AtomicBoolean(false));
    }

    public static void stop(String requestId) {
        AtomicBoolean flag = stopFlags.get(requestId);
        if (flag != null) {
            flag.set(true);
        }
    }

    public static <T> Flux<T> wrapFluxWithInterrupt(Flux<T> flux, String requestId) {
        AtomicBoolean flag = stopFlags.getOrDefault(requestId, new AtomicBoolean(false));
        return flux.takeUntilOther(Flux.create(sink -> {
            new Thread(() -> {
                while (!flag.get()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
                sink.complete();
            }).start();
        }));
    }
}
