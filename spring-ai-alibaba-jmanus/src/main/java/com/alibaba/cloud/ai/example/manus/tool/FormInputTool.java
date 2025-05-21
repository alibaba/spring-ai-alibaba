package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM表单输入工具：支持带标签的多输入项和描述说明。
 */
public class FormInputTool implements ToolCallBiFunctionDef {

    private static final Logger log = LoggerFactory.getLogger(FormInputTool.class);

    private static final String PARAMETERS = """
        {
          "type": "object",
          "properties": {
            "inputs": {
              "type": "array",
              "description": "输入项列表，每项包含 label 和 value 字段",
              "items": {
                "type": "object",
                "properties": {
                  "label": { "type": "string", "description": "输入项标签" },
                  "value": { "type": "string", "description": "输入内容" }
                },
                "required": ["label", "value"]
              }
            },
            "description": {
              "type": "string",
              "description": "如何填写这些输入项的说明"
            }
          },
          "required": ["inputs", "description"]
        }
        """;

    public static final String name = "form_input";

    private static final String description = """
        提供一个带标签的多输入项表单工具。LLM可通过本工具提交多个输入项（每项有label和内容），并附带填写说明。
        适用于需要结构化输入的场景。
        """;

    public static OpenAiApi.FunctionTool getToolDefinition() {
        OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
        return new OpenAiApi.FunctionTool(function);
    }

    public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback() {
        return FunctionToolCallback.builder(name, new FormInputTool())
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                .build();
    }

    // 数据结构：
    /**
     * 表单输入项，包含标签和对应的值。 */
    public static class InputItem {
        private String label;
        private String value;
        public InputItem() {}
        public InputItem(String label, String value) { this.label = label; this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    /**
     * 用户提交的表单数据，包含输入项列表和说明。 */
    public static class UserFormInput {
        private List<InputItem> inputs;
        private String description;
        public UserFormInput() {}
        public UserFormInput(List<InputItem> inputs, String description) {
            this.inputs = inputs;
            this.description = description;
        }
        public List<InputItem> getInputs() { return inputs; }
        public void setInputs(List<InputItem> inputs) { this.inputs = inputs; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Replace boolean states with a single enum to represent the input state.
    public enum InputState {
        AWAITING_USER_INPUT,
        INPUT_RECEIVED,
        INPUT_TIMEOUT
    }

    private InputState inputState = InputState.INPUT_RECEIVED;

    public InputState getInputState() {
        return inputState;
    }

    public void setInputState(InputState inputState) {
        this.inputState = inputState;
    }

    @Override
    public ToolExecuteResult apply(String s, ToolContext toolContext) {
        log.info("FormInputTool input: {}", s);
        // Mark that the system is now awaiting user input
        setInputState(InputState.AWAITING_USER_INPUT);
        return new ToolExecuteResult(s);
    }

    public void markUserInputReceived() {
        // Mark that user input has been received and the system can proceed
        setInputState(InputState.INPUT_RECEIVED);
    }

    public void handleInputTimeout() {
        log.warn("Input timeout occurred. No input received from the user.");
        setInputState(InputState.INPUT_TIMEOUT);
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
        return Map.class;
    }

    @Override
    public boolean isReturnDirect() {
        return true;
    }

    @Override
    public void setPlanId(String planId) {
        // 可选实现
    }

    @Override
    public void cleanup(String planId) {
        // 可选实现
    }

    @Override
    public String getServiceGroup() {
        return "default-service-group";
    }

    // 存储用户提交的表单输入
    private List<UserFormInput> userFormInputs = new ArrayList<>();

    /**
     * 获取当前工具状态，包括表单说明和输入项
     */
    @Override
    public String getCurrentToolStateString() {
        if (userFormInputs.isEmpty()) {
            return "FormInputTool 状态：未接收到用户输入";
        }
        try {
            StringBuilder stateBuilder = new StringBuilder("FormInputTool 状态：\n");
            for (int i = 0; i < userFormInputs.size(); i++) {
                UserFormInput input = userFormInputs.get(i);
                stateBuilder.append(String.format("输入轮次 %d:\n说明：%s\n输入项：%s\n",
                        i + 1,
                        input.getDescription(),
                        objectMapper.writeValueAsString(input.getInputs())));
            }
            return stateBuilder.toString();
        } catch (JsonProcessingException e) {
            log.error("Error serializing userFormInputs", e);
            return "FormInputTool 状态：序列化输入项时出错";
        }
    }
}
