package dev.ai.alibaba.samples.executor;

import dev.ai.alibaba.samples.executor.AgentAction;

public record IntermediateStep(
    AgentAction action,
    String observation
) {}

