package com.alibaba.cloud.ai.studio.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.metadata.Usage;

/**
 * @author zhuoguang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageMetrics {
    
    
    private Usage usage;
    
    private String traceId;
    
    
}
