package com.alibaba.cloud.ai.model.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 北极星
 */
@Data
public class Agent {

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Tool> tool;

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
    public static class Tool {

        private boolean enabled;

        private String providerId;

        private String providerName;

        private String providerType;

        private String toolLabel;

        private String toolName;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private ToolParameters toolParameters;
    }

    @Data
    public static class ToolParameters {
        private String cookies;
        private String format;
        private String language;
        private String preserveFormatting;
        private String proxy;
        private String videoId;
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

        private List<String> allowedFileExtensions = new ArrayList<>(0);

        private List<String> allowedFileTypes = new ArrayList<>(0);

        private List<String> allowedFileUploadMethods = new ArrayList<>(0);

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
