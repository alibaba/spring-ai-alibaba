package org.bsc.langgraph4j.agentexecutor;

public record AgentOutcome(
    AgentAction action,
    AgentFinish finish
) {}
