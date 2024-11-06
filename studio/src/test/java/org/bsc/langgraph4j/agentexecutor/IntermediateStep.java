package org.bsc.langgraph4j.agentexecutor;

public record IntermediateStep(
    AgentAction action,
    String observation
) {}

