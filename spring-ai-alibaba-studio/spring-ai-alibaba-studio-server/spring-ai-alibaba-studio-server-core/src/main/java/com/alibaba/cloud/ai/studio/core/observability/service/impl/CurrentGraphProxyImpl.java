package com.alibaba.cloud.ai.studio.core.observability.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.studio.core.observability.model.EnhancedNodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphService;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.config.SAAGraphFlowRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; // 引入日志
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Data
@Slf4j // 添加 Slf4j 注解以便使用日志
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
    public ResponseEntity<Boolean> switchTo(String graphId) {
        if (graphId == null || graphId.isBlank()) {
            log.warn("Attempted to switch to a null or blank graphId.");
            this.currentGraphId = null;
            this.compiledGraph = null;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }

        SAAGraphFlow graphToSwitch = graphFlowRegistry.findById(graphId);
        if (graphToSwitch == null) {
            log.error("Failed to switch: Graph with ID '{}' not found in registry.", graphId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }

        try {
            CompiledGraph newCompiledGraph = graphToSwitch.stateGraph().compile();
            log.info("Successfully compiled graph with ID: {}", graphId);

            this.currentGraphId = graphId;
            this.compiledGraph = newCompiledGraph;

            return ResponseEntity.status(HttpStatus.OK).body(true);
        } catch (Exception e) {
            log.error("Failed to compile graph with ID '{}'. Please check the graph definition.", graphId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
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

    /**
     * 【核心修改】为流处理方法增加保护
     */
    @Override
    public Flux<Map<String, Object>> writeStreamSnapshots(@RequestParam("text") String inputText) {
        if (compiledGraph == null) {
            log.warn("Cannot execute stream: No graph is currently compiled and active.");
            return Flux.error(new IllegalStateException("No graph is currently active. Please select a graph first."));
        }

        RunnableConfig cfg = RunnableConfig.builder()
                .streamMode(CompiledGraph.StreamMode.SNAPSHOTS)
                .build();

        return Flux.create(sink -> {
            try {
                compiledGraph.stream(Map.of("original_text", inputText), cfg)
                        .forEachAsync(node -> sink.next(node.state().data()))
                        .whenComplete((v, e) -> {
                            if (e != null) {
                                sink.error(e);
                            } else {
                                sink.complete();
                            }
                        });
            } catch (GraphRunnerException e) {
                sink.error(new RuntimeException("Error in snapshot stream execution: " + e.getMessage(), e));
            }
        });
    }


    @Override
    public Flux<NodeOutput> writeStream(String inputText) {
        if (compiledGraph == null) {
            log.warn("Cannot execute stream: No graph is currently compiled and active.");
            return Flux.error(new IllegalStateException("No graph is currently active. Please select a graph first."));
        }

        try {
            return Flux.fromStream(compiledGraph.stream(Map.of("original_text", inputText)).stream());
        } catch (GraphRunnerException e) {
            return Flux.error(new RuntimeException("Error in stream execution: " + e.getMessage(), e));
        }
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

        return Flux.create(sink -> {
            try {
                Map<String, LocalDateTime> nodeStartTimes = new ConcurrentHashMap<>();
                int[] executionOrder = {0}; // 使用数组来保证在lambda中可变

                compiledGraph.stream(Map.of("original_text", inputText), cfg)
                        .forEachAsync(node -> {
                            String nodeId = node.node();
                            Map<String, Object> nodeData = node.state().data();
                            
                            // 构建增强的节点输出
                            EnhancedNodeOutput enhancedOutput = EnhancedNodeOutput.builder()
                                    .nodeId(nodeId)
                                    .nodeName(extractNodeName(nodeId)) // 从节点ID提取名称，可根据实际情况调整
                                    .nodeType(extractNodeType(nodeData)) // 从数据中提取节点类型
                                    .executionStatus("SUCCESS")
                                    .startTime(nodeStartTimes.getOrDefault(nodeId, LocalDateTime.now()))
                                    .endTime(LocalDateTime.now())
                                    .durationMs(calculateDuration(nodeStartTimes.get(nodeId)))
                                    .data(nodeData)
                                    .executionOrder(++executionOrder[0])
                                    .isFinal(isLastNode(nodeId))
                                    .build();

                            sink.next(enhancedOutput);
                        })
                        .whenComplete((v, e) -> {
                            if (e != null) {
                                // execute Error
                                EnhancedNodeOutput errorOutput = EnhancedNodeOutput.builder()
                                        .nodeId("ERROR")
                                        .nodeName("执行错误")
                                        .nodeType("ERROR")
                                        .executionStatus("FAILED")
                                        .endTime(LocalDateTime.now())
                                        .errorMessage(e.getMessage())
                                        .build();
                                
                                sink.next(errorOutput);
                                sink.error(e);
                            } else {
                                sink.complete();
                            }
                        });
            } catch (GraphRunnerException e) {
                sink.error(new RuntimeException("Error in enhanced stream execution: " + e.getMessage(), e));
            }
        });
    }

    /**
     * 从节点ID提取节点名称
     * 可根据实际的节点命名规则进行调整
     */
    private String extractNodeName(String nodeId) {
        if (nodeId == null) return "未知节点";
        
        // 简单的名称映射，可以根据实际需求扩展
        switch (nodeId) {
            case "summarizer": return "文本摘要";
            case "rewriter": return "文本重写";
            case "titleGenerator": return "标题生成";
            case "extractDocs": return "文档提取";
            default: return nodeId; // 默认返回节点ID作为名称
        }
    }

    /**
     * 从节点数据中提取节点类型
     */
    private String extractNodeType(Map<String, Object> nodeData) {
        if (nodeData == null) return "UNKNOWN";
        
        // 根据数据内容判断节点类型
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
     * 计算执行耗时
     */
    private Long calculateDuration(LocalDateTime startTime) {
        if (startTime == null) return 0L;
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }

    /**
     * 判断是否为最后一个节点
     * 可根据实际的图结构进行判断
     */
    private Boolean isLastNode(String nodeId) {
        // 简单的判断逻辑，可以根据实际需求调整
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
        // TODO: 实现运行逻辑，例如: compiledGraph.invoke(...)
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
