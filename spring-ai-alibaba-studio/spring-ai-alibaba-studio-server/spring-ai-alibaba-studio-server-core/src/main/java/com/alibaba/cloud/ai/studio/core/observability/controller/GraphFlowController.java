package com.alibaba.cloud.ai.studio.core.observability.controller;

import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlowRegistry;
import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Observability", description = "APIs for system observability, including graph flows.")
@RequestMapping("/observability/v1/flows")
public class GraphFlowController {

    private final SAAGraphFlowRegistry graphFlowRegistry;

    public GraphFlowController(SAAGraphFlowRegistry graphFlowRegistry) {
        this.graphFlowRegistry = graphFlowRegistry;
    }

    @GetMapping
    @Operation(summary = "Get Flows by Owner", description = "Retrieves a list of all graph flows owned by a specific user.")
    public List<SAAGraphFlowInfoDTO> getFlowsByOwner(
        @Parameter(description = "The unique identifier of the owner.", required = true, example = "saa")
        @RequestParam("ownerID") String ownerID) {
        
        List<SAAGraphFlow> userFlows = graphFlowRegistry.findByOwnerID(ownerID);
        
        return userFlows.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    /**
     * Converts a {@link SAAGraphFlow} domain object into a {@link SAAGraphFlowInfoDTO}.
     *
     * @param flow The {@link SAAGraphFlow} object to convert.
     * @return The corresponding {@link SAAGraphFlowInfoDTO}.
     */
    private SAAGraphFlowInfoDTO convertToDTO(SAAGraphFlow flow) {
        // Generate MERMAID graph representation
        String mermaidGraph = generateMermaidGraph(flow);
        
        return new SAAGraphFlowInfoDTO(
                flow.id(),
                flow.title(),
                flow.description(),
                flow.stateGraph(),
                flow.tags(),
                mermaidGraph
        );
    }

    /**
     * Generates MERMAID graph representation for the given flow.
     * 
     * @param flow The {@link SAAGraphFlow} object to generate MERMAID for.
     * @return The MERMAID graph representation as a string, or an error message if generation fails.
     */
    private String generateMermaidGraph(SAAGraphFlow flow) {
        try {
            if (flow.stateGraph() != null) {
                GraphRepresentation representation = flow.stateGraph().getGraph(
                    GraphRepresentation.Type.MERMAID, 
                    flow.title() != null ? flow.title() : flow.id(), 
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
}
