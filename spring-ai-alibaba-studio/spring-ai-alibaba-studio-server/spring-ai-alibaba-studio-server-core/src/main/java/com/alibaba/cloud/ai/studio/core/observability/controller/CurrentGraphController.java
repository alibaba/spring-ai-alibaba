package com.alibaba.cloud.ai.studio.core.observability.controller;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphProxy;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlowRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/observability/v1/graph")
@Tag(name = "Observability", description = "APIs for managing graph execution and runtime operations, including real-time node output streaming.")
public class CurrentGraphController {
    private final CurrentGraphProxy currentGraphProxy;
    private final SAAGraphFlowRegistry graphFlowRegistry;

    CurrentGraphController(CurrentGraphProxy currentGraphProxy, SAAGraphFlowRegistry saaGraphFlowRegistry)
    {
        this.currentGraphProxy = currentGraphProxy;
        this.graphFlowRegistry = saaGraphFlowRegistry;
    }


    @PostMapping("setCurrentGraph")
    public Boolean setCurrentGraph(@RequestParam String graphId){
        return currentGraphProxy.switchTo(graphId);

    }

    @GetMapping("getCurrentGraph")
    public SAAGraphFlow getCurrentGraph(){
        return currentGraphProxy.getCurrentGraph();
    }

    @GetMapping("node/stream")
    public Flux<NodeOutput> writeStream(@RequestParam("text") String inputText) {
        return currentGraphProxy.writeStream(inputText);
    }

    @GetMapping("node/stream_snapshots")
    public Flux<Map<String, Object>> writeStreamSnapshots(@RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamSnapshots(inputText);
    }
}
