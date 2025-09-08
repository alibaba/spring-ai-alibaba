package com.alibaba.cloud.ai.studio.core.observability.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.service.CurrentGraphProxy;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.util.Map;

@RestController
@RequestMapping("/observability/v1/node")
@Tag(name = "Observability", description = "APIs for system observability, including streamNodeOutput")
public class StreamNodeController {

    private final CurrentGraphProxy currentGraphProxy;

    StreamNodeController(CurrentGraphProxy currentGraphProxy){
        this.currentGraphProxy=currentGraphProxy;
    }
    @GetMapping(path = "/stream_snapshots",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> writeStreamSnapshots(@RequestParam("text") String inputText) {
        return currentGraphProxy.writeStreamSnapshots(inputText);
    }
    @GetMapping(path = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NodeOutput> writeStream(@RequestParam("text") String inputText) {
        return currentGraphProxy.writeStream(inputText);
    }

}
