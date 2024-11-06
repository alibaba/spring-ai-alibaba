package dev.ai.alibaba.samples.executor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.NonNull;

public record AgentAction(
    @NonNull
    ToolExecutionRequest toolExecutionRequest,
    String log ) {

}