package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:54
 */
public class RenderContext {
    private final AtomicInteger seq = new AtomicInteger(0);

    public String nextVar(String base) { return base + seq.incrementAndGet(); }
}
