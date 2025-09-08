package com.alibaba.cloud.ai.studio.core.observability.service.impl;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import com.alibaba.cloud.ai.studio.core.observability.service.GraphFlowService;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlowRegistry;
import org.springframework.stereotype.Service;

@Service
public class GraphFlowServiceImpl implements GraphFlowService {

    private final SAAGraphFlowRegistry graphFlowRegistry;

    public GraphFlowServiceImpl(SAAGraphFlowRegistry graphFlowRegistry) {
        this.graphFlowRegistry = graphFlowRegistry;
    }


    @Override
    public String generateMermaidGraph(SAAGraphFlow flow) {
        try {
            if (flow.stateGraph() != null) {
                GraphRepresentation representation = flow.stateGraph().getGraph(
                        GraphRepresentation.Type.MERMAID,
                        flow.title() != null ? flow.title() : flow.graphId(),
                        false
                );
                return representation.content();
            } else {
                return "Graph not available";
            }
        } catch (Exception e) {
            return "Error generating MERMAID graph: " + e.getMessage();
        }
    }
    @Override
    public SAAGraphFlowInfoDTO convertToDTO(SAAGraphFlow flow) {
        // Generate MERMAID graph representation
        String mermaidGraph = generateMermaidGraph(flow);

        return new SAAGraphFlowInfoDTO(
                flow.graphId(),
                flow.title(),
                flow.description(),
                flow.stateGraph(),
                flow.tags(),
                mermaidGraph
        );
    }

    @Override
    public SAAGraphFlowInfoDTO findFlowById(String flowId) {
        if (flowId == null || flowId.isBlank()) {
            return null;
        }
        
        SAAGraphFlow flow = graphFlowRegistry.findById(flowId);
        if (flow == null) {
            return null;
        }
        
        return convertToDTO(flow);
    }
}
