package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.NonNull;

public record AgentAction(
    @NonNull
    ToolExecutionRequest toolExecutionRequest,
    String log ) {

}