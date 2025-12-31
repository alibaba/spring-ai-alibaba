package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * @author Sunrisea
 */
@Component
public class DeepSeekChatClientFactory implements ChatClientFactory{
    
    private static final String DEEPSEEK_PROVIDER = "deepseek";
    
    private final ObservationRegistry observationRegistry;
    
    private final ToolCallingManager toolCallingManager;
    
    private final ChatModelObservationConvention customChatModelObservationConvention;
    
    public DeepSeekChatClientFactory(ObservationRegistry observationRegistry,
            ToolCallingManager toolCallingManager,
            ChatModelObservationConvention customChatModelObservationConvention) {
        this.observationRegistry = observationRegistry;
        this.toolCallingManager = toolCallingManager;
        this.customChatModelObservationConvention = customChatModelObservationConvention;
    }
    
    @Override
    public String getSupportProvider() {
        return DEEPSEEK_PROVIDER;
    }
    
    @Override
    public ChatModel buildChatModel(ModelConfigDO modelConfig) {
        DeepSeekApi api = DeepSeekApi.builder()
                .apiKey(modelConfig.getApiKey())
                .build();
        
        return DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry)
                .build();
    }
    
    @Override
    public ChatOptions buildChatOptions(ModelConfigDO modelConfig, Map<String, Object> userParameters,
            Map<String, String> observationMetadata) {
        DeepSeekChatOptions.Builder builder = DeepSeekChatOptions.builder();
        builder.model(modelConfig.getModelName());
        for (Map.Entry<String, Object> entry : userParameters.entrySet()) {
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
                case "topLogprobs":
                case "top_logprobs":
                    builder.topLogprobs(((Number) value).intValue());
                    break;
                case "logprobs":
                    builder.logprobs((Boolean) value);
                    break;
                default:
                    break;
            }
        }
        DeepSeekChatOptions chatOptions = builder.build();
        DeepSeekObservationMetadataChatOptions options = DeepSeekObservationMetadataChatOptions.fromDeepSeekOptions(chatOptions);
        if (observationMetadata != null) {
            options.setObservationMetadata(observationMetadata);
        }
        return options;
    }
}
