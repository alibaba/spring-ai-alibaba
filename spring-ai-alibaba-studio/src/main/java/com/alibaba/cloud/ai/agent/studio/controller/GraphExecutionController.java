/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.agent.studio.controller;

import com.alibaba.cloud.ai.agent.studio.dto.GraphRunRequest;
import com.alibaba.cloud.ai.agent.studio.dto.GraphRunResponse;
import com.alibaba.cloud.ai.agent.studio.dto.messages.MessageDTO;
import com.alibaba.cloud.ai.agent.studio.dto.messages.ToolRequestConfirmMessageDTO;
import com.alibaba.cloud.ai.agent.studio.loader.GraphLoader;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.UserMessage;

/**
 * Controller handling graph execution endpoints. Registered only when a {@link GraphLoader}
 * bean exists. Uses independent logic from Agent execution.
 */
@RestController
public class GraphExecutionController {

	private static final Logger log = LoggerFactory.getLogger(GraphExecutionController.class);

	private final ObjectMapper mapper = new ObjectMapper();

	private final GraphLoader graphLoader;

	@Autowired
	public GraphExecutionController(GraphLoader graphLoader) {
		this.graphLoader = graphLoader;
	}

	/**
	 * Executes a graph run and streams the resulting events using Server-Sent Events (SSE).
	 *
	 * @param request The GraphRunRequest containing run details.
	 * @return A Flux that streams events to the client in standard SSE format.
	 */
	@PostMapping(value = "/graph_run_sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> graphRunSse(@RequestBody GraphRunRequest request) {
		if (request.graphName == null || request.graphName.trim().isEmpty()) {
			return Flux.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"graphName cannot be null or empty"));
		}
		if (request.threadId == null || request.threadId.trim().isEmpty()) {
			return Flux.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"threadId cannot be null or empty"));
		}

		try {
			CompiledGraph graph = graphLoader.loadGraph(request.graphName);

			Map<String, Object> inputs;
			if (request.inputs != null && !request.inputs.isEmpty()) {
				inputs = new HashMap<>(request.inputs);
			}
			else {
				String content = (request.newMessage != null && request.newMessage.getContent() != null)
						? request.newMessage.getContent() : "";
				inputs = Map.of("input", content, "messages", List.of(new UserMessage(content)));
			}

			RunnableConfig runnableConfig = RunnableConfig.builder()
					.threadId(request.threadId)
					.addMetadata("user_id", request.userId != null ? request.userId : "user-001")
					.build();

			Flux<NodeOutput> graphStream = graph.stream(inputs, runnableConfig);

			return executeGraph(graphStream);
		}
		catch (Exception e) {
			log.error("Error during graph run for thread {}", request.threadId, e);
			return Flux.error(new ResponseStatusException(
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Graph run failed", e));
		}
	}

	private Flux<ServerSentEvent<String>> executeGraph(Flux<NodeOutput> graphStream) {
		return graphStream
				.filter(nodeOutput -> !(nodeOutput instanceof StreamingOutput<?> so
						&& so.getOutputType() == OutputType.AGENT_MODEL_FINISHED))
				.map(nodeOutput -> {
					String node = nodeOutput.node();
					String agentName = nodeOutput.agent();
					Usage tokenUsage = nodeOutput.tokenUsage();
					Map<String, Object> stateData = nodeOutput.state() != null
							? new LinkedHashMap<>(nodeOutput.state().data()) : null;
					GraphRunResponse graphResponse = null;

					if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
						Message message = streamingOutput.message();
						if (message == null) {
							graphResponse = new GraphRunResponse(node, agentName, (MessageDTO) null, tokenUsage, "",
									stateData);
						}
						else if (message instanceof AssistantMessage assistantMessage) {
							if (assistantMessage.hasToolCalls()) {
								graphResponse = new GraphRunResponse(node, agentName, assistantMessage, tokenUsage, "",
										stateData);
							}
							else {
								graphResponse = new GraphRunResponse(node, agentName, assistantMessage, tokenUsage,
										assistantMessage.getText(), stateData);
							}
						}
						else {
							graphResponse = new GraphRunResponse(node, agentName, message, tokenUsage, "", stateData);
						}
					}
					else if (nodeOutput instanceof InterruptionMetadata interruptionMetadata) {
						ToolRequestConfirmMessageDTO toolRequestMessage =
								MessageDTO.MessageDTOFactory.fromInterruptionMetadata(interruptionMetadata);
						graphResponse = new GraphRunResponse(node, agentName, toolRequestMessage, tokenUsage, "",
								stateData);
					}
					else {
						graphResponse = new GraphRunResponse(node, agentName, (MessageDTO) null, tokenUsage, "",
								stateData);
					}

					try {
						if (graphResponse != null) {
							String jsonData = mapper.writeValueAsString(graphResponse);
							return ServerSentEvent.<String>builder().data(jsonData).build();
						}
					}
					catch (Exception e) {
						log.error("Failed to serialize GraphRunResponse to JSON", e);
						return ServerSentEvent.<String>builder()
								.data("{\"error\":\"Failed to serialize response\"}")
								.build();
					}
					return ServerSentEvent.<String>builder().data("{}").build();
				})
				.onErrorResume(error -> {
					log.error("Error occurred during graph stream execution", error);
					String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error occurred";
					String errorType = error.getClass().getSimpleName();
					String errorJson = String.format(
							"{\"error\":true,\"errorType\":\"%s\",\"errorMessage\":\"%s\"}",
							errorType.replace("\"", "\\\""),
							errorMessage.replace("\"", "\\\"").replace("\n", "\\n"));
					return Flux.just(
							ServerSentEvent.<String>builder().event("error").data(errorJson).build());
				});
	}
}
