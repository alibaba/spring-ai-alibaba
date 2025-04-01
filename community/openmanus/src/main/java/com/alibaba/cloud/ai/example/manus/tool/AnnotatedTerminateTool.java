package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.agent.AgentState;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

@DynamicAgentDefinition(
    agentName = "terminateAgent",
    agentDescription = "An agent that can terminate the current execution with a message",
    systemPrompt = "You are a termination agent that can stop the current execution when needed. Use the terminate tool to stop with an appropriate message.",
    nextStepPrompt = "Would you like to terminate the current execution? If yes, provide a termination message.",
    availableToolKeys = {"terminate"}
)
public class AnnotatedTerminateTool implements ToolCallBiFunctionDef {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedTerminateTool.class);

    private static final String PARAMETERS = """
            {
              "type" : "object",
              "properties" : {
                "message" : {
                  "type" : "string",
                  "description" : "The termination message"
                }
              },
              "required" : [ "message" ]
            }
            """;

    private static final String name = "terminate";

    private static final String description = "Terminate the current step with a message.";

    private BaseAgent agent;

    public AnnotatedTerminateTool() {
    }

    public ToolExecuteResult run(String toolInput) {
        log.info("Terminate toolInput: " + toolInput);
        agent.setState(AgentState.FINISHED);
        return new ToolExecuteResult(toolInput);
    }

    @Override
    public ToolExecuteResult apply(String s, ToolContext toolContext) {
        return run(s);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getParameters() {
        return PARAMETERS;
    }

    @Override
    public Class<?> getInputType() {
        return String.class;
    }

    @Override
    public boolean isReturnDirect() {
        return true;
    }

    @Override
    public void setAgent(BaseAgent agent) {
        this.agent = agent;
    }
}
