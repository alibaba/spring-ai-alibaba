package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.BeanUtils;

import java.util.Map;

/**
 * @author Sunrisea
 */
public class OpenAiObservationMetadataChatOptions extends OpenAiChatOptions implements ObservationMetadataAwareOptions {
    
    private Map<String, String> observationMetadata;
    
    public static OpenAiObservationMetadataChatOptions fromOpenAiOptions(OpenAiChatOptions fromOptions) {
        OpenAiObservationMetadataChatOptions options = new OpenAiObservationMetadataChatOptions();
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
    public OpenAiObservationMetadataChatOptions copy() {
        OpenAiObservationMetadataChatOptions options;
        options = OpenAiObservationMetadataChatOptions.fromOpenAiOptions(this);
        options.setObservationMetadata(observationMetadata);
        return options;
    }
    
}
