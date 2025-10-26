package com.alibaba.cloud.ai.studio.core.observability.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.studio.core.observability.model.EnhancedNodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.model.ResponseBody;
import com.alibaba.cloud.ai.studio.core.observability.model.ResponseResultType;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphService;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.config.SAAGraphFlowRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CurrentGraphService} that manages the currently active graph.
 *
 */
@Service
@Data
@Slf4j
public class CurrentGraphProxyImpl implements CurrentGraphService {

    private final SAAGraphFlowRegistry graphFlowRegistry;

    private volatile String currentGraphId;

    private volatile CompiledGraph compiledGraph;

    private static final String EMPTY_GRAPH_ID = "EMPTY_GRAPH_ID";

    private static final SAAGraphFlow DEFAULT_EMPTY_GRAPH_FLOW = createEmptyGraph();

    public CurrentGraphProxyImpl(SAAGraphFlowRegistry graphFlowRegistry) {
        this.graphFlowRegistry = graphFlowRegistry;
    }

    @Override
    public ResponseEntity<ResponseBody> switchTo(String graphId) {
        if (graphId == null || graphId.isBlank()) {
            log.warn("Attempted to switch to a null or blank graphId.");
            this.currentGraphId = null;
            this.compiledGraph = null;
            return ResponseEntity.badRequest().body(new ResponseBody(ResponseResultType.SUCCESS,"Attempted to switch to a null or blank graphId",null));
        }

        SAAGraphFlow graphToSwitch = graphFlowRegistry.findById(graphId);
        if (graphToSwitch == null) {
            log.error("Failed to switch: Graph with ID '{}' not found in registry.", graphId);
            return ResponseEntity.badRequest().body(new ResponseBody(ResponseResultType.ERROR,"Attempted to switch to a null or blank graphId",null));
        }

        try {
            CompiledGraph newCompiledGraph = graphToSwitch.stateGraph().compile();
            log.info("Successfully compiled graph with ID: {}", graphId);

            this.currentGraphId = graphId;
            this.compiledGraph = newCompiledGraph;

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseBody(ResponseResultType.SUCCESS,"Successfully compiled graph with ID: " + graphId,null));
        } catch (Exception e) {
            log.error("Failed to compile graph with ID '{}'. Please check the graph definition.", graphId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseBody(ResponseResultType.ERROR,"Failed to compile graph with ID: {}", graphId));
        }
    }

    @Override
    public SAAGraphFlow getCurrentGraph() {
        if (currentGraphId != null) {
            SAAGraphFlow graph = graphFlowRegistry.findById(currentGraphId);
            return graph != null ? graph : DEFAULT_EMPTY_GRAPH_FLOW;
        }
        return DEFAULT_EMPTY_GRAPH_FLOW;
    }

    @Override
    public Flux<Map<String, Object>> writeStreamSnapshots(@RequestParam("text") String inputText) {
        if (compiledGraph == null) {
            log.warn("Cannot execute stream: No graph is currently compiled and active.");
            return Flux.error(new IllegalStateException("No graph is currently active. Please select a graph first."));
        }

        RunnableConfig cfg = RunnableConfig.builder()
                .streamMode(CompiledGraph.StreamMode.SNAPSHOTS)
                .build();

        return compiledGraph.stream(Map.of("original_text", inputText), cfg)
                .map(node -> node.state().data())
                .onErrorResume(e -> Flux.error(new RuntimeException("Error in snapshot stream execution: " + e.getMessage(), e)));
    }


    @Override
    public Flux<NodeOutput> writeStream(String inputText) {
        if (compiledGraph == null) {
            log.warn("Cannot execute stream: No graph is currently compiled and active.");
            return Flux.error(new IllegalStateException("No graph is currently active. Please select a graph first."));
        }

        return compiledGraph.stream(Map.of("original_text", inputText));
    }

    @Override
    public Flux<EnhancedNodeOutput> writeStreamEnhanced(String inputText) {
        if (compiledGraph == null) {
            log.warn("Cannot execute enhanced stream: No graph is currently compiled and active.");
            return Flux.error(new IllegalStateException("No graph is currently active. Please select a graph first."));
        }

        RunnableConfig cfg = RunnableConfig.builder()
                .streamMode(CompiledGraph.StreamMode.SNAPSHOTS)
                .build();

        Map<String, LocalDateTime> nodeStartTimes = new ConcurrentHashMap<>();
        int[] executionOrder = {0}; // Use an array to make it mutable in lambda
        StateGraph stateGraph = getCurrentGraph().stateGraph();

        return compiledGraph.stream(Map.of("original_text", inputText), cfg)
                .map(node -> {
                    String nodeId = node.node();
                    Map<String, Object> nodeData = node.state().data();

                    List<String> parentNodes = stateGraph.getEdges().edgesByTargetId(nodeId).stream()
                            .map(edge -> edge.sourceId())
                            .collect(Collectors.toList());
                    // Build the enhanced node output
                    return EnhancedNodeOutput.builder()
                            .nodeId(nodeId)
                            .executionStatus("SUCCESS")
                            .startTime(nodeStartTimes.getOrDefault(nodeId, LocalDateTime.now()))
                            .endTime(LocalDateTime.now())
                            .durationMs(calculateDuration(nodeStartTimes.get(nodeId)))
                            .inputData(node.state().data())
                            .data(nodeData)
                            .parentNodes(parentNodes)
                            .executionOrder(++executionOrder[0])
                            .isFinal(isLastNode(nodeId))
                            .build();
                })
                .onErrorResume(e -> {
                    // Execute Error
                    EnhancedNodeOutput errorOutput = EnhancedNodeOutput.builder()
                            .nodeId("ERROR")
                            .executionStatus("FAILED")
                            .endTime(LocalDateTime.now())
                            .errorMessage(e.getMessage())
                            .build();
                    return Flux.concat(Flux.just(errorOutput), Flux.error(e));
                });
    }

    /**
     * Extracts the node type from the node data.
     */
    private String extractNodeType(Map<String, Object> nodeData) {
        if (nodeData == null) return "UNKNOWN";

        // Determine node type based on data content
        if (nodeData.containsKey("summary")) {
            return "SUMMARIZER";
        } else if (nodeData.containsKey("reworded")) {
            return "REWRITER";
        } else if (nodeData.containsKey("title")) {
            return "TITLE_GENERATOR";
        } else if (nodeData.containsKey("docs")) {
            return "DOCUMENT_EXTRACTOR";
        } else {
            return "CUSTOM";
        }
    }

    /**
     * Calculates the execution duration.
     */
    private Long calculateDuration(LocalDateTime startTime) {
        if (startTime == null) return 0L;
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    /**
     * Checks if it is the last node.
     * This can be determined based on the actual graph structure.
     */
    private Boolean isLastNode(String nodeId) {
        // Simple logic, can be adjusted based on actual needs
        return "titleGenerator".equals(nodeId) || nodeId.contains("end") || nodeId.contains("final");
    }

    private static SAAGraphFlow createEmptyGraph() {
        StateGraph emptyStateGraph = new StateGraph();

        return SAAGraphFlow.builder()
                .id(EMPTY_GRAPH_ID)
                .title("Default Empty Graph")
                .stateGraph(emptyStateGraph)
                .build();
    }
    @Override
    public ResponseEntity<Void> run() {
        if (compiledGraph == null) {
            log.warn("Cannot run: No graph is compiled.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        // TODO: Implement the run logic, e.g., compiledGraph.invoke(...)
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
