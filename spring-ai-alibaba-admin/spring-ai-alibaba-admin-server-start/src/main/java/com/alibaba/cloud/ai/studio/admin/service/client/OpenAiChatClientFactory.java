package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OpenAI聊天客户端工厂
 *
 * @author zhuoguang
 */
@Component
public class OpenAiChatClientFactory implements ChatClientFactory {
    
    public static final String OPEN_AI_PROVIDER = "openai";
    
    private final ObservationRegistry observationRegistry;
    
    private final ToolCallingManager toolCallingManager;
    
    private final ChatModelObservationConvention customChatModelObservationConvention;
    
    public OpenAiChatClientFactory(ObservationRegistry observationRegistry,
            ToolCallingManager toolCallingManager,
            ChatModelObservationConvention customChatModelObservationConvention) {
        this.observationRegistry = observationRegistry;
        this.toolCallingManager = toolCallingManager;
        this.customChatModelObservationConvention = customChatModelObservationConvention;
    }
    
    @Override
    public String getSupportProvider() {
        return OPEN_AI_PROVIDER;
    }
    
    @Override
    public ChatModel buildChatModel(ModelConfigDO modelConfig) {
        OpenAiApi api = OpenAiApi.builder().apiKey(modelConfig.getApiKey()).baseUrl(modelConfig.getBaseUrl()).build();
        // 创建ChatModel
        OpenAiChatModel model =  OpenAiChatModel.builder().openAiApi(api)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry)
                .build();
        model.setObservationConvention(customChatModelObservationConvention);
        return model;
    }
    
    @Override
    public ChatOptions buildChatOptions(ModelConfigDO modelConfig, Map<String, Object> parameters, Map<String, String> observationMetadata) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(modelConfig.getModelName());
        builder.streamUsage(true);
        // 应用支持的参数
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            switch (key) {
                case "temperature":
                    builder.temperature(((Number) value).doubleValue());
                    break;
                case "maxTokens":
                case "max_tokens":
                    builder.maxTokens(((Number) value).intValue());
                    break;
                case "maxCompletionToken":
                case "max_completion_token":
                    builder.maxCompletionTokens(((Number) value).intValue());
                    break;
                case "topLogprobs":
                case "top_logprobs":
                    builder.topLogprobs(((Number) value).intValue());
                    break;
                case "logprobs":
                    builder.logprobs((Boolean) value);
                    break;
                case "N":
                case "n":
                    builder.N(((Number) value).intValue());
                    break;
                case "reasoningEffort":
                case "reasoning_effort":
                    builder.reasoningEffort(value.toString());
                    break;
                case "topP":
                case "top_p":
                    builder.topP(((Number) value).doubleValue());
                    break;
                case "frequencyPenalty":
                case "frequency_penalty":
                    builder.frequencyPenalty(((Number) value).doubleValue());
                    break;
                case "presencePenalty":
                case "presence_penalty":
                    builder.presencePenalty(((Number) value).doubleValue());
                    break;
                default:
                    break;
            }
        }
        OpenAiChatOptions chatOptions = builder.build();
        OpenAiObservationMetadataChatOptions options = OpenAiObservationMetadataChatOptions.fromOpenAiOptions(chatOptions);
        if (observationMetadata != null) {
            options.setObservationMetadata(observationMetadata);
        }
        return options;
    }
    
    
}
