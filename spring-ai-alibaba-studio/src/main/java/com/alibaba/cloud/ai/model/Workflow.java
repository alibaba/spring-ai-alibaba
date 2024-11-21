package com.alibaba.cloud.ai.model;

import lombok.Data;

import java.util.Map;

// TODO
@Data
public class Workflow {

    private String id;

    private String type;

    private Map<String, Object> graph;

    private String createdBy;

    private Long createdAt;

    private String updatedBy;

    private Long updatedAt;
}
