package com.alibaba.cloud.ai.model.chatbot.node;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * @author 北极星
 */
@Data
@NoArgsConstructor
public class ChatBot extends NodeData {
    public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("text", VariableType.STRING.value());

    private AgentMode agentMode;

    private Model model;

    private String openingStatement;

    private String prePrompt;

    private String promptType;

    private CompletionPromptConfig completionPromptConfig;

    private UserInputForm userInputForm;

    @Data
    @Accessors
    public static class AgentMode {

        private Boolean enabled = false;

        private String strategy = "function_call";

        @JsonProperty("max_iteration")
        private Integer maxIteration = 0;
    }

    @Data
    public static class Model {

        public static final String MODE_COMPLETION = "completion";

        public static final String MODE_CHAT = "chat";

        private String mode;

        private String name;

        private String provider;

        private String jsonSchema;

        private CompletionParams completionParams;

    }

    @Data
    public static class CompletionParams {

        private List<String> stop;

        private Float frequencyPenalty;

        private Integer maxTokens;

        private Float presencePenalty;

        private String responseFormat;

        private Float temperature;

        private Float topP;
    }

    @Data
    @Accessors
    public static class CompletionPromptConfig {

        private int toK;
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
    public static class UserInputForm {

        private List<Paragraph> paragraph;

        @JsonProperty("text-input")
        private List<TextInput> textInput;

        private List<Select> select;

        private List<Number> number;
    }

    @Data
    public static class Paragraph {

        private String label;

        private int maxLength;

        private Boolean required = false;

        private String variable;

        private String defaultContent;
    }

    @Data
    public static class TextInput {

        private String label;

        private int maxLength;

        private Boolean required = false;

        private String variable;

        private String defaultContent;
    }

    @Data
    public static class Select {

        private String label;

        private List<String> options;

        private Boolean required = false;

        private String variable;

        private String defaultContent;
    }

    @Data
    public static class Number {

        private String label;

        private List<Object> options;

        private Boolean required = false;

        private String variable;

        private String defaultContent;
    }
}
