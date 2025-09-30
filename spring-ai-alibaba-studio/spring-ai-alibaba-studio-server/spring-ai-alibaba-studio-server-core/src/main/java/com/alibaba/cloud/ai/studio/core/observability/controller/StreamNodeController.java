/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * REST Controller for Node Streaming Operations
 * 
 * <p>This controller provides streaming endpoints for real-time node execution monitoring.
 * It offers different levels of detail in the streaming output, from basic raw node outputs
 * to enhanced outputs with comprehensive execution metadata.</p>
 * 
 * <p>All endpoints return Server-Sent Events (SSE) streams for real-time monitoring
 * capabilities. These streams provide live updates as nodes execute within the graph.</p>
 * @see CurrentGraphService
 * @see EnhancedNodeOutput
 */
@RestController
@RequestMapping("/observability/v1/node")
@Tag(name = "Node Streaming", description = "Real-time streaming APIs for node execution monitoring")
public class StreamNodeController {

	private final CurrentGraphService currentGraphProxy;

	public StreamNodeController(CurrentGraphService currentGraphProxy) {
		this.currentGraphProxy = currentGraphProxy;
	}

	@GetMapping(path = "/stream_snapshots", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Node State Snapshots",
			description = "Streams state snapshots after each node completes, containing only business data")
	public Flux<Map<String, Object>> writeStreamSnapshots(
			@Parameter(description = "Input text to process",
					example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStreamSnapshots(inputText);
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Basic Node Outputs", description = "Streams raw node outputs in NodeOutput format")
	public Flux<NodeOutput> writeStream(@Parameter(description = "Input text to process",
			example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStream(inputText);
	}

	@GetMapping(path = "/stream_enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream Enhanced Node Outputs",
			description = "Streams comprehensive node information including execution status, timing, and metadata")
	public Flux<EnhancedNodeOutput> writeStreamEnhanced(
			@Parameter(description = "Input text to process",
					example = "I went to the West Lake today, the weather was very good, and I felt very happy") @RequestParam("text") String inputText) {
		return currentGraphProxy.writeStreamEnhanced(inputText);
	}

}
