package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public class Agent {

    private final  ChatLanguageModel chatLanguageModel;
    @Singular  private final List<ToolSpecification> tools;


    public Response<AiMessage> execute( String input, List<IntermediateStep> intermediateSteps ) {
        var userMessageTemplate = PromptTemplate.from( "{{input}}" )
                                                    .apply(Map.of("input", input));

        var messages = new ArrayList<ChatMessage>();

        messages.add(new SystemMessage("You are a helpful assistant"));
        messages.add(new UserMessage(userMessageTemplate.text()));

        if (!intermediateSteps.isEmpty()) {

            var toolRequests = intermediateSteps.stream()
                    .map(IntermediateStep::action)
                    .map(AgentAction::toolExecutionRequest)
                    .collect(Collectors.toList());

            messages.add(new AiMessage(toolRequests)); // reply with tool requests

            for (IntermediateStep step : intermediateSteps) {
                var toolRequest = step.action().toolExecutionRequest();

                messages.add(new ToolExecutionResultMessage(toolRequest.id(), toolRequest.name(), step.observation()));
            }
        }
        return chatLanguageModel.generate( messages, tools );
    }
}
