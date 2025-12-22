package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import org.springframework.beans.BeanUtils;

import java.util.Map;

public class DashScopeObservationMetadataChatOptions extends DashScopeChatOptions implements ObservationMetadataAwareOptions {
    
    private Map<String, String> observationMetadata;
    
    public static DashScopeObservationMetadataChatOptions fromDashScopeOptions(DashScopeChatOptions fromOptions) {
        DashScopeObservationMetadataChatOptions options = new DashScopeObservationMetadataChatOptions();
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
    
    public DashScopeObservationMetadataChatOptions copy() {
        DashScopeObservationMetadataChatOptions options;
        options = DashScopeObservationMetadataChatOptions.fromDashScopeOptions(this);
        options.setObservationMetadata(observationMetadata);
        return options;
    }
}
