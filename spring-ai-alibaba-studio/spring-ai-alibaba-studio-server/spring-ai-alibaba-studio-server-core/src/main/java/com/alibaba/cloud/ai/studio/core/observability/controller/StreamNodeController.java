package com.alibaba.cloud.ai.studio.core.observability.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.model.EnhancedNodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/observability/v1/node")
@Tag(name = "Observability", description = "APIs for system observability, including streamNodeOutput")
public class StreamNodeController {

    private final CurrentGraphService currentGraphProxy;

    StreamNodeController(CurrentGraphService currentGraphProxy){
        this.currentGraphProxy=currentGraphProxy;
    }
    
    @GetMapping(path = "/stream_snapshots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取节点流式快照", description = "获取每个节点完成后的状态快照，只包含业务数据")
    public Flux<Map<String, Object>> writeStreamSnapshots(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamSnapshots(inputText);
    }
    
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取基础流式输出", description = "获取每个节点的原始输出（NodeOutput格式）")
    public Flux<NodeOutput> writeStream(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStream(inputText);
    }
    
    @GetMapping(path = "/stream_enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "获取增强流式输出", description = "获取包含完整节点信息的流式输出，包括节点名称、ID、执行状态、时间戳等")
    public Flux<EnhancedNodeOutput> writeStreamEnhanced(
            @Parameter(description = "输入文本", example = "今天我去了西湖，天气特别好，感觉特别开心")
            @RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamEnhanced(inputText);
    }

}
