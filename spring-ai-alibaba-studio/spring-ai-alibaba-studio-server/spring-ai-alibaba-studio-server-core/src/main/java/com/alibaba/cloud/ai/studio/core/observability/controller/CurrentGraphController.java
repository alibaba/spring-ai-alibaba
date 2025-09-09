package com.alibaba.cloud.ai.studio.core.observability.controller;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.model.EnhancedNodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphService;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.config.SAAGraphFlowRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/observability/v1/graph")
@Tag(name = "Observability", description = "APIs for managing graph execution and runtime operations, including real-time node output streaming.")
public class CurrentGraphController {
    private final CurrentGraphService currentGraphProxy;
    private final SAAGraphFlowRegistry graphFlowRegistry;

    CurrentGraphController(CurrentGraphService currentGraphProxy, SAAGraphFlowRegistry saaGraphFlowRegistry)
    {
        this.currentGraphProxy = currentGraphProxy;
        this.graphFlowRegistry = saaGraphFlowRegistry;
    }


    @PostMapping("setCurrentGraph")
    public ResponseEntity setCurrentGraph(@RequestParam String graphId){
        return currentGraphProxy.switchTo(graphId);

    }

    @GetMapping("getCurrentGraph")
    public SAAGraphFlow getCurrentGraph(){
        return currentGraphProxy.getCurrentGraph();
    }

    @GetMapping(path = "node/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取当前图的基础流式输出", description = "获取当前选中图的每个节点原始输出")
    public Flux<NodeOutput> writeStream(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStream(inputText);
    }

    @GetMapping(path = "node/stream_snapshots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取当前图的流式快照", description = "获取当前选中图的每个节点完成后的状态快照")
    public Flux<Map<String, Object>> writeStreamSnapshots(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamSnapshots(inputText);
    }

    @GetMapping(path = "node/stream_enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取当前图的增强流式输出", description = "获取当前选中图的包含完整节点信息的流式输出，包括节点名称、ID、执行状态、时间戳等")
    public Flux<EnhancedNodeOutput> writeStreamEnhanced(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamEnhanced(inputText);
    }
}
