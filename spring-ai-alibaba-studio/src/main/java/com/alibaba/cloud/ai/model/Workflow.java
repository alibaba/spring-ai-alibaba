package com.alibaba.cloud.ai.model;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class Workflow {

    private String id;

    private Graph graph;

    private String createdBy;

    private Long createdAt;

    private String updatedBy;

    private Long updatedAt;
}

@Data
class Graph{
    private List<WorkflowEdge> edges;
    private List<WorkflowNode> nodes;
    private Map<String, Float> viewport;
}