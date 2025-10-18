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
package com.alibaba.cloud.ai.studio.core.observability.dto;

import com.alibaba.cloud.ai.graph.StateGraph;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Data Transfer Object for Graph Flow Information
 * 
 * <p>This record represents comprehensive information about a graph flow,
 * including its metadata, execution graph, and visual representation.
 * It serves as the primary data structure for API responses when
 * retrieving graph flow details.</p>
 * 
 * <p>The DTO includes both technical details (StateGraph) and user-friendly
 * information (title, description, tags) to support various use cases
 * from execution to visualization.</p>
 * 
 * @param id The unique identifier of the graph flow
 * @param title The human-readable title of the graph flow
 * @param description A detailed description of the graph flow's purpose and functionality
 * @param stateGraph The actual state graph object containing the execution logic
 * @param tags A list of tags for categorization and search functionality
 * @param mermaidGraph The Mermaid diagram representation for visualization
 * 
 * @author Spring AI Alibaba Team
 * @since 1.0.0
 * @see com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow
 */
@Schema(description = "Graph flow information including metadata and visual representation")
public record SAAGraphFlowInfoDTO(
        
        @Schema(description = "Unique identifier of the graph flow", 
                example = "sentiment-analysis-flow",
                required = true)
        String id,
        
        @Schema(description = "Human-readable title of the graph flow", 
                example = "Sentiment Analysis Workflow",
                required = true)
        String title,
        
        @Schema(description = "Detailed description of the graph flow's purpose", 
                example = "A comprehensive sentiment analysis workflow that processes text input through multiple AI models")
        String description,
        
        @Schema(description = "The state graph object containing execution logic")
        StateGraph stateGraph,
        
        @Schema(description = "List of tags for categorization", 
                example = "[\"nlp\", \"sentiment\", \"analysis\"]")
        List<String> tags,
        
        @Schema(description = "Mermaid diagram representation for visualization", 
                example = "graph TD; A[Input] --> B[Preprocessing]; B --> C[Analysis]; C --> D[Output]")
        String mermaidGraph
) {}
