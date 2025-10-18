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
@Tag(name = "Observability",
		description = "APIs for managing graph execution and runtime operations, including real-time node output streaming.")
public class CurrentGraphController {

	private final CurrentGraphService currentGraphProxy;

	private final SAAGraphFlowRegistry graphFlowRegistry;

	public CurrentGraphController(CurrentGraphService currentGraphProxy, SAAGraphFlowRegistry saaGraphFlowRegistry) {
		this.currentGraphProxy = currentGraphProxy;
		this.graphFlowRegistry = saaGraphFlowRegistry;
	}

	@PostMapping("setCurrentGraph")
	@Operation(summary = "Set Current Active Graph",
			description = "Switches the system to use the specified graph for subsequent operations")
	public ResponseEntity<?> setCurrentGraph(
			@Parameter(description = "Unique identifier of the graph to activate", required = true,
					example = "sentiment-analysis-flow") @RequestParam String graphId) {
		return currentGraphProxy.switchTo(graphId);
	}

	@GetMapping("getCurrentGraph")
	@Operation(summary = "Get Current Active Graph",
			description = "Retrieves information about the currently active graph in the system")
	public SAAGraphFlow getCurrentGraph() {
		return currentGraphProxy.getCurrentGraph();
	}

	@GetMapping(path = "node/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Basic Node Outputs",
			description = "Streams raw output from each node in the current graph as it executes")
	public Flux<NodeOutput> writeStream(@Parameter(description = "Input text to process through the graph pipeline",
			example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStream(inputText);
	}

	@GetMapping(path = "node/stream_snapshots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Node State Snapshots",
			description = "Streams state snapshots after each node completes execution")
	public Flux<Map<String, Object>> writeStreamSnapshots(
			@Parameter(description = "Input text to process through the graph pipeline",
					example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStreamSnapshots(inputText);
	}

	@GetMapping(path = "node/stream_enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Enhanced Node Outputs",
			description = "Streams comprehensive node information including execution status, timing, and metadata")
	public Flux<EnhancedNodeOutput> writeStreamEnhanced(
			@Parameter(description = "Input text to process through the graph pipeline",
					example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStreamEnhanced(inputText);
	}

}
