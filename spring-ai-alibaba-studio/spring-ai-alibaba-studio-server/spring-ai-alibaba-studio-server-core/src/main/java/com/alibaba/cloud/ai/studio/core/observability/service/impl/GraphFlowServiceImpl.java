/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.studio.core.observability.service.impl;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import com.alibaba.cloud.ai.studio.core.observability.service.GraphFlowService;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.config.SAAGraphFlowRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of GraphFlowService providing business logic for graph flow operations.
 * 
 * <p>This service handles the conversion between domain models and DTOs, generation of
 * visual representations (Mermaid diagrams), and coordination with the graph flow registry.</p>
 * 
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Converting SAAGraphFlow entities to DTOs</li>
 *   <li>Generating Mermaid diagram representations</li>
 *   <li>Managing graph flow lookup operations</li>
 *   <li>Error handling and logging</li>
 * </ul>
 * 
 */
@Service
public class GraphFlowServiceImpl implements GraphFlowService {

    private static final Logger logger = LoggerFactory.getLogger(GraphFlowServiceImpl.class);

    private final SAAGraphFlowRegistry graphFlowRegistry;

    public GraphFlowServiceImpl(SAAGraphFlowRegistry graphFlowRegistry) {
        this.graphFlowRegistry = graphFlowRegistry;
    }


    @Override
    public String generateMermaidGraph(SAAGraphFlow flow) {
        if (flow == null) {
            logger.warn("Cannot generate Mermaid graph: flow is null");
            return "Error: Graph flow is null";
        }

        try {
            if (flow.stateGraph() != null) {
                String title = flow.title() != null ? flow.title() : flow.graphId();
                logger.debug("Generating Mermaid graph for flow: {}", title);
                
                GraphRepresentation representation = flow.stateGraph().getGraph(
                        GraphRepresentation.Type.MERMAID,
                        title,
                        false
                );
                
                String content = representation.content();
                logger.debug("Successfully generated Mermaid graph for flow: {}", flow.graphId());
                return content;
            } else {
                logger.warn("StateGraph is null for flow: {}", flow.graphId());
                return "Graph not available";
            }
        } catch (Exception e) {
            logger.error("Error generating Mermaid graph for flow: {}", flow.graphId(), e);
            return "Error generating MERMAID graph: " + e.getMessage();
        }
    }

    @Override
    public SAAGraphFlowInfoDTO convertToDTO(SAAGraphFlow flow) {
        if (flow == null) {
            logger.warn("Cannot convert to DTO: flow is null");
            return null;
        }

        try {
            // Generate MERMAID graph representation
            String mermaidGraph = generateMermaidGraph(flow);

            logger.debug("Converting flow to DTO: {}", flow.graphId());
            return new SAAGraphFlowInfoDTO(
                    flow.graphId(),
                    flow.title(),
                    flow.description(),
                    flow.stateGraph(),
                    flow.tags(),
                    mermaidGraph
            );
        } catch (Exception e) {
            logger.error("Error converting flow to DTO: {}", flow.graphId(), e);
            throw new RuntimeException("Failed to convert flow to DTO", e);
        }
    }

    @Override
    public SAAGraphFlowInfoDTO findFlowById(String flowId) {
        if (flowId == null || flowId.isBlank()) {
            logger.debug("Flow ID is null or empty");
            return null;
        }
        
        logger.debug("Finding flow by ID: {}", flowId);
        SAAGraphFlow flow = graphFlowRegistry.findById(flowId);
        if (flow == null) {
            logger.debug("Flow not found with ID: {}", flowId);
            return null;
        }
        
        return convertToDTO(flow);
    }
}
