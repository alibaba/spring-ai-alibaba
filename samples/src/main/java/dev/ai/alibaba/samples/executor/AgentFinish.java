package dev.ai.alibaba.samples.executor;

import java.util.Map;

public record AgentFinish(
    Map<String, Object> returnValues,
    String log
) {}
