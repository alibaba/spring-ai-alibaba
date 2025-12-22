package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class TraceSpanDTO {

    private String traceId;
    
    private String spanId;
    
    private String parentSpanId;
    
    private Long durationNs;
    
    private String spanKind;
    
    private String service;
    
    private String spanName;
    
    private String startTime;
    
    private String endTime;
    
    private String status;
    
    private Integer errorCount;
    
    private Map<String, Object> attributes;
    
    private Map<String, Object> resources;
    
    private List<SpanLinkDTO> spanLinks;
    
    private List<SpanEventDTO> spanEvents;
}