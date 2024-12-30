package com.alibaba.cloud.ai.model.chatbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;


/**
 * @author 北极星
 */
@Data
@NoArgsConstructor
public class ChatBot {

    private AgentMode agentMode;

    private Model model;

    private String openingStatement;

    private String prePrompt;

    private String promptType;

    private CompletionPromptConfig completionPromptConfig;

    private DataSetConfig datasetConfigs;

    private String datasetQueryVariable = "";

    private List<String> externalDataTools = new ArrayList<>(0);

    private FileUpLoad fileUpLoad;

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
    @AllArgsConstructor
    public static class DataSetConfig {

        private List<DataSet> dataSet;

        private Boolean rerankingEnable;

        private String rerankingMode;

        private Integer topK = 0;

        private Weights weights;
    }

    @Data
    public static class DataSet {

        private String id;

        private Boolean enabled;
    }

    @Data
    public static class Weights {

        private KeywordSetting keywordSetting;

        private VectorSetting vectorSetting;
    }

    @Data
    public static class KeywordSetting {
        private int keywordWeight;
    }

    @Data
    public static class VectorSetting {
        private String embeddingModelName;
        private String embeddingProviderName;
        private int vectorWeight;
    }

    @Data
    public static class FileUpLoad {

        private List<String> allowed_file_extensions = new ArrayList<>(0);

        private List<String> allowed_file_types = new ArrayList<>(0);

        private List<String> allowed_file_upload_methods = new ArrayList<>(0);

        private Boolean enabled = false;

        private Image image;

        @JsonProperty("number_limits")
        private int numberLimit;
    }

    @Data
    public static class Image {

        private String detail;

        private Boolean enabled = false;

        private int numberLimits;

        private List<String> transferMethods;
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
