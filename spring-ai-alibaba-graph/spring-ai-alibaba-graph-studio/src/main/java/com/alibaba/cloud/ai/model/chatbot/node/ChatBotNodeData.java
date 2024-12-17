package com.alibaba.cloud.ai.model.chatbot.node;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * @author 北极星
 */
@Data
@NoArgsConstructor
public class ChatBotNodeData<T> extends NodeData {
    public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("text", VariableType.STRING.value());

    private ModelConfig model;

    private List<PromptTemplate> promptTemplate;

    private MemoryConfig memoryConfig;

    private List<VariableConfig> variableConfig;

    public ChatBotNodeData (List<VariableSelector> inputs, List<Variable> outputs) {
        super(inputs, outputs);
    }

    @Data
    @AllArgsConstructor
    public static class PromptTemplate {

        private String role;

        private String text;

    }

    @Data
    public static class ModelConfig {

        public static final String MODE_COMPLETION = "completion";

        public static final String MODE_CHAT = "chat";

        private String mode;

        private String name;

        private String provider;

        private String jsonSchema;

        private LLMNodeData.CompletionParams completionParams;

    }

    @Data
    public static class CompletionParams {

        private Integer maxTokens;

        private Float repetitionPenalty;

        private String responseFormat;

        private Integer seed;

        private List<String> stop;

        private Float temperature;

        private Float topP;

        private Integer topK;

    }

    @Data
    @Accessors(chain = true)
    public static class MemoryConfig {

        private Integer windowSize;

        private Boolean windowEnabled;

        private Boolean includeLastMessage;

        private String lastMessageTemplate;

    }

    @Data
    public static class VariableConfig {

        private int maxLength;

        private boolean required;

        private String variable;

        private String defaultContent;
    }
}
