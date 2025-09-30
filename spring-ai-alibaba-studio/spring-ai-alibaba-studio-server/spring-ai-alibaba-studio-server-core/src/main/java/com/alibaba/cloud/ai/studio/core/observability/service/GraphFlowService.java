package com.alibaba.cloud.ai.studio.core.observability.service;

import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;

public interface GraphFlowService {
    /**
     * Generates a MERMAID graph representation for the given flow.
     *
     * @param flow The {@link SAAGraphFlow} object to generate the MERMAID graph for.
     * @return The MERMAID graph as a string, or an error message if generation fails.
     */
    String generateMermaidGraph(SAAGraphFlow flow);

    /**
     * Converts a {@link SAAGraphFlow} domain object to a {@link SAAGraphFlowInfoDTO}.
     *
     * @param flow The {@link SAAGraphFlow} object to convert.
     * @return The corresponding {@link SAAGraphFlowInfoDTO}.
     */
    SAAGraphFlowInfoDTO convertToDTO(SAAGraphFlow flow);

    /**
     * Finds a graph flow by its unique identifier.
     *
     * @param flowId The unique identifier of the flow to find.
     * @return The corresponding {@link SAAGraphFlowInfoDTO}, or null if not found.
     */
    SAAGraphFlowInfoDTO findFlowById(String flowId);

}
