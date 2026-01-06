package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.beans.BeanUtils;

import java.util.Map;

public class DeepSeekObservationMetadataChatOptions extends DeepSeekChatOptions implements ObservationMetadataAwareOptions {
    
    private Map<String, String> observationMetadata;
    
    public static DeepSeekObservationMetadataChatOptions fromDeepSeekOptions(DeepSeekChatOptions fromOptions) {
        DeepSeekObservationMetadataChatOptions options = new DeepSeekObservationMetadataChatOptions();
        BeanUtils.copyProperties(fromOptions, options);
        return options;
    }
    
    @Override
    public Map<String, String> getObservationMetadata() {
        return observationMetadata;
    }
    
    @Override
    public void setObservationMetadata(Map<String, String> observationMetadata) {
        this.observationMetadata = observationMetadata;
    }
    
    @Override
    public DeepSeekObservationMetadataChatOptions copy() {
        DeepSeekObservationMetadataChatOptions options;
        options = DeepSeekObservationMetadataChatOptions.fromDeepSeekOptions( this);
        options.setObservationMetadata(observationMetadata);
        return options;
    }
}
