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
