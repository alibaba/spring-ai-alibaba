package com.alibaba.cloud.ai.graph.action.llm;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Node action that uses LLM with chat history messages.
 * @author Robocanic
 * @author robocanic@gmail.com
 */
public class LLMNodeAction<State extends AgentState> implements NodeAction<State> {

    public static final String DEFAULT_SYSTEM_MESSAGE = "You're a helpful assistant";
    public static final String MESSAGE_KEY = "message";
    private final ChatClient chatClient;

    public LLMNodeAction(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(State state) throws Exception {
        List<Message> messages = state.value(MESSAGE_KEY, new ArrayList<>());
        List<Generation> generations = chatClient.prompt()
                .system(s->s.params(state.data()))
                .messages(messages)
                .call()
                .chatResponse().getResults();
        List<Message> output = generations.stream().map(Generation::getOutput).collect(Collectors.toList());
        return Map.of(MESSAGE_KEY, output);
    }

    public static Builder builder(ChatModel chatModel){
        return new Builder(chatModel);
    }

    public static class Builder{
        private ChatModel chatModel;

        private String systemMessage;

        private String[] functionNames;

        public Builder(ChatModel chatModel){
            this.chatModel = chatModel;
        }


        public Builder systemMessage(String systemMessage){
            this.systemMessage = systemMessage;
            return this;
        }

        public Builder systemMessage(Resource resource){
            try {
                this.systemMessage = resource.getContentAsString(Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder functionNames(String... functionNames){
            this.functionNames = functionNames;
            return this;
        }

        public <State extends AgentState> LLMNodeAction<State> build(){
            String defaultSystemMessage = systemMessage == null ? DEFAULT_SYSTEM_MESSAGE : systemMessage;
            ChatClient.Builder clientBuilder = ChatClient.builder(this.chatModel)
                    .defaultSystem(defaultSystemMessage);
            if (functionNames != null && functionNames.length > 0){
                clientBuilder = clientBuilder.defaultFunctions(functionNames);
            }
            return new LLMNodeAction<>(clientBuilder.build());
        }


    }

}
