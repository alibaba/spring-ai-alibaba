package com.alibaba.cloud.ai.studio.core.observability.service;

import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;

public interface GraphFlowService {
    /**
     * Generates MERMAID graph representation for the given flow.
     *
     * @param flow The {@link SAAGraphFlow} object to generate MERMAID for.
     * @return The MERMAID graph representation as a string, or an error message if generation fails.
     */
    String generateMermaidGraph(SAAGraphFlow flow);

    /**
     * Converts a {@link SAAGraphFlow} domain object into a {@link SAAGraphFlowInfoDTO}.
     *
     * @param flow The {@link SAAGraphFlow} object to convert.
     * @return The corresponding {@link SAAGraphFlowInfoDTO}.
     */
    SAAGraphFlowInfoDTO convertToDTO(SAAGraphFlow flow);

    /**
     * Finds a single graph flow by its unique identifier.
     * 根据 flowId 查找单个图形流程。
     *
     * @param flowId The unique identifier of the flow to search for
     *               流程的唯一标识符
     * @return The corresponding {@link SAAGraphFlowInfoDTO}, or null if not found
     *         对应的 {@link SAAGraphFlowInfoDTO}，如果不存在则返回 null
     */
    SAAGraphFlowInfoDTO findFlowById(String flowId);

}
