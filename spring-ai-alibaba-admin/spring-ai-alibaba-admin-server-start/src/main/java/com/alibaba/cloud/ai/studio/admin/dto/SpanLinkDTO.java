package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SpanLinkDTO {

    private String traceId;
    
    private String spanId;
    
    private Map<String, Object> attributes;
}