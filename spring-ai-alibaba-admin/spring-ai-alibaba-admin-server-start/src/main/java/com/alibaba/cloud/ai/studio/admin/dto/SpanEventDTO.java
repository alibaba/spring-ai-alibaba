package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SpanEventDTO {

    private String time;
    
    private String name;
    
    private Map<String, Object> attributes;
}