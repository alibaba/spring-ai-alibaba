package com.alibaba.cloud.ai.studio.core.observability.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphProxy;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlowRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; // 引入日志
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@Data
@Slf4j // 添加 Slf4j 注解以便使用日志
public class CurrentGraphProxyImpl implements CurrentGraphProxy {

    private final SAAGraphFlowRegistry graphFlowRegistry;

    private volatile String currentGraphId;

    private volatile CompiledGraph compiledGraph;

    private static final String EMPTY_GRAPH_ID = "EMPTY_GRAPH_ID";

    private static final SAAGraphFlow DEFAULT_EMPTY_GRAPH_FLOW = createEmptyGraph();

    public CurrentGraphProxyImpl(SAAGraphFlowRegistry graphFlowRegistry) {
        this.graphFlowRegistry = graphFlowRegistry;
    }

    @Override
    public Boolean switchTo(String graphId) {
        if (graphId == null || graphId.isBlank()) {
            log.warn("Attempted to switch to a null or blank graphId.");
            this.currentGraphId = null;
            this.compiledGraph = null;
            return false;
        }

        SAAGraphFlow graphToSwitch = graphFlowRegistry.findById(graphId);
        if (graphToSwitch == null) {
            log.error("Failed to switch: Graph with ID '{}' not found in registry.", graphId);
            return false;
        }

        try {
            CompiledGraph newCompiledGraph = graphToSwitch.stateGraph().compile();
            log.info("Successfully compiled graph with ID: {}", graphId);

            this.currentGraphId = graphId;
            this.compiledGraph = newCompiledGraph;

            return true;
        } catch (Exception e) {
            log.error("Failed to compile graph with ID '{}'. Please check the graph definition.", graphId, e);
            return false;
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

    private static SAAGraphFlow createEmptyGraph() {
        StateGraph emptyStateGraph = new StateGraph();

        return SAAGraphFlow.builder()
                .id(EMPTY_GRAPH_ID)
                .title("Default Empty Graph")
                .stateGraph(emptyStateGraph)
                .build();
    }

    @Override
    public Boolean run() {
        if (compiledGraph == null) {
            log.warn("Cannot run: No graph is compiled.");
            return false;
        }
        // TODO: 实现运行逻辑，例如: compiledGraph.invoke(...)
        return true;
    }
}
