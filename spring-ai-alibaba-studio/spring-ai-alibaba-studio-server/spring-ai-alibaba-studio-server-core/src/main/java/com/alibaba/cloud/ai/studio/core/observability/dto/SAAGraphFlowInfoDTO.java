package com.alibaba.cloud.ai.studio.core.observability.dto;

import com.alibaba.cloud.ai.graph.StateGraph;
import java.util.List;

public record SAAGraphFlowInfoDTO(
        String id,
        String title,
        String description,
        StateGraph stateGraph,
        List<String> tags,
        String mermaidGraph
) {}
