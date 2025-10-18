package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.graph.GraphDTO;
import com.alibaba.cloud.ai.studio.runtime.domain.graph.GraphDefinitionDTO;
import com.alibaba.cloud.ai.studio.runtime.domain.graph.GraphEdgeDTO;
import com.alibaba.cloud.ai.studio.runtime.domain.graph.GraphNodeDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Graph Management", description = "APIs for managing and interacting with graphs/workflows.")
@RequestMapping("/api/graphs")
public class GraphController {

    @GetMapping("/{graphId}")
    @Operation(summary = "Get Graph Initialization", description = "Initializes and retrieves the structure and metadata for a specified graph.")
    public Result<GraphDTO> getGraph(
        @Parameter(description = "The unique identifier of the graph.", required = true, example = "graph_001")
        @PathVariable String graphId) {

        // TODO: Replace with actual data fetching logic from a service layer.
        GraphDTO mockGraph = createMockGraph(graphId);

        return Result.success(mockGraph);
    }

    /**
     * Creates a mock graph DTO for demonstration purposes.
     * This method simulates fetching graph data from a persistent store.
     * @param graphId The ID of the graph to mock.
     * @return A fully populated GraphDTO.
     */
    private GraphDTO createMockGraph(String graphId) {
        // Mock Nodes
        GraphNodeDTO startNode = new GraphNodeDTO(
            "start",
            "开始",
            "START",
            Map.of()
        );

        GraphNodeDTO intentRecognitionNode = new GraphNodeDTO(
            "intent_recognition",
            "意图识别",
            "LLM_NODE",
            Map.of(
                "model", "qwen-turbo",
                "prompt", "分析用户意图...",
                "temperature", 0.1
            )
        );

        GraphNodeDTO knowledgeSearchNode = new GraphNodeDTO(
            "knowledge_search",
            "知识检索",
            "TOOL_NODE",
            Map.of(
                "toolName", "knowledge_base_search",
                "parameters", Map.of()
            )
        );

        List<GraphNodeDTO> nodes = List.of(startNode, intentRecognitionNode, knowledgeSearchNode);

        // Mock Edges
        GraphEdgeDTO edge1 = new GraphEdgeDTO(
            "edge_001",
            "start",
            "intent_recognition",
            null
        );

        GraphEdgeDTO edge2 = new GraphEdgeDTO(
            "edge_002",
            "intent_recognition",
            "knowledge_search",
            "intent != 'unknown'"
        );

        List<GraphEdgeDTO> edges = List.of(edge1, edge2);

        // Mock Definition
        GraphDefinitionDTO definition = new GraphDefinitionDTO(nodes, edges);

        // Mock Graph DTO
        return new GraphDTO(
            graphId,
            "智能客服主流程",
            "完整的智能客服处理流程",
            definition
        );
    }
}
