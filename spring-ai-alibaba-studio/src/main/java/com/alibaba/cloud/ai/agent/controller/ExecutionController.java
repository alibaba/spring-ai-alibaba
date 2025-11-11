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

package com.alibaba.cloud.ai.agent.controller;

import com.alibaba.cloud.ai.agent.agui.AGUIAgent;
import com.alibaba.cloud.ai.agent.dto.AgentRunRequest;
import com.alibaba.cloud.ai.agent.dto.messages.AgentRunResponse;
import com.alibaba.cloud.ai.agent.dto.messages.MessageDTO;
import com.alibaba.cloud.ai.agent.dto.messages.ToolRequestConfirmMessageDTO;
import com.alibaba.cloud.ai.agent.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/** Controller handling agent execution endpoints. */
@RestController
public class ExecutionController {

	private static final Logger log = LoggerFactory.getLogger(ExecutionController.class);
	final ObjectMapper mapper = new ObjectMapper();
	final AGUIAgent uiAgent = new AGUIAgent();
	private final AgentLoader agentLoader;

	@Autowired
	public ExecutionController(AgentLoader agentLoader) {
		this.agentLoader = agentLoader;
	}

	/**
	 * Executes a non-streaming agent run for a given session and message.
	 *
	 * @param request The AgentRunRequest containing run details.
	 * @return A list of events generated during the run.
	 * @throws ResponseStatusException if the session is not found or the run fails.
	 */
//	@PostMapping("/run")
//	public NodeOutput agentRun(@RequestBody AgentRunRequest request) {
//		if (request.appName == null || request.appName.trim().isEmpty()) {
//			log.warn("appName cannot be null or empty in POST /run request.");
//			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty");
//		}
//		if (request.threadId == null || request.threadId.trim().isEmpty()) {
//			log.warn("sessionId cannot be null or empty in POST /run request.");
//			throw new ResponseStatusException(
//					HttpStatus.BAD_REQUEST, "sessionId cannot be null or empty");
//		}
//		log.info("Request received for POST /run for session: {}", request.threadId);
//
//		try {
//			BaseAgent agent = agentLoader.loadAgent(request.appName);
//			RunnableConfig runnableConfig = RunnableConfig.builder().threadId(request.threadId)
//					.addMetadata("user_id", request.userId).build();
//			// request.stateDelta 目前用不到
//			Optional<NodeOutput> state = agent.invokeAndGetOutput(request.newMessage, runnableConfig);
//
//			return state.orElseThrow(() -> {
//				log.error("Agent run for session {} did not produce any output.", request.threadId);
//				return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run produced no output");
//			});
//		}
//		catch (Exception e) {
//			log.error("Error during agent run for session {}", request.threadId, e);
//			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e);
//		}
//	}

//	@PostMapping(path = "/run_sse_saa",
//			consumes = MediaType.APPLICATION_JSON_VALUE,
//			produces = MediaType.TEXT_EVENT_STREAM_VALUE
//	)
//	public Flux<ServerSentEvent<String>> agentRunSseSaa(@RequestBody AgentRunRequest request) throws Exception {
//		if (request.appName == null || request.appName.trim().isEmpty()) {
//			log.warn(
//					"appName cannot be null or empty in SSE request for appName: {}, session: {}",
//					request.appName,
//					request.threadId);
//			return Flux.error(
//					new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty"));
//		}
//		if (request.threadId == null || request.threadId.trim().isEmpty()) {
//			log.warn(
//					"sessionId cannot be null or empty in SSE request for appName: {}, session: {}",
//					request.appName,
//					request.threadId);
//			return Flux.error(
//					new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId cannot be null or empty"));
//		}
//
//		try {
//			BaseAgent agent = agentLoader.loadAgent(request.appName);
//			RunnableConfig runnableConfig = RunnableConfig.builder()
//					.threadId(request.threadId)
//					.addMetadata("user_id", request.userId)
//					.build();
//			// request.stateDelta 目前用不到
//			Flux<NodeOutput> agentStream = agent.stream(request.newMessage, runnableConfig);
//
//			// Create a heartbeat Flux to keep connection alive (emit every 15 seconds)
//			Flux<NodeOutput> heartBeat = Flux.interval(java.time.Duration.ofSeconds(5))
//					.map(tick -> NodeOutput.of("heartbeat", OverAllStateBuilder.builder().build()))
//					.takeUntilOther(agentStream.last());
//
//			// Merge the agent stream with heartbeat
//			return Flux.merge(agentStream, heartBeat);
//		}
//		catch (Exception e) {
//			log.error("Error during agent run for session {}", request.threadId, e);
//			return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
//		}
//	}

	/**
	 * Executes an agent run and streams the resulting events using Server-Sent Events (SSE).
	 *
	 * @param request The AgentRunRequest containing run details.
	 * @return A Flux that will stream events to the client in standard SSE format.
	 */
	@PostMapping(value = "/run_sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> agentRunSse(@RequestBody AgentRunRequest request) {
		if (request.appName == null || request.appName.trim().isEmpty()) {
			log.warn(
					"appName cannot be null or empty in SSE request for appName: {}, session: {}",
					request.appName,
					request.threadId);
			return Flux.error(
					new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty"));
		}
		if (request.threadId == null || request.threadId.trim().isEmpty()) {
			log.warn(
					"threadId cannot be null or empty in SSE request for appName: {}, session: {}",
					request.appName,
					request.threadId);
			return Flux.error(
					new ResponseStatusException(HttpStatus.BAD_REQUEST, "threadId cannot be null or empty"));
		}

		try {
			BaseAgent agent = agentLoader.loadAgent(request.appName);
			RunnableConfig runnableConfig = RunnableConfig.builder()
					.threadId(request.threadId)
					.addMetadata("user_id", request.userId)
					.build();
			// request.stateDelta 目前用不到
			Flux<NodeOutput> agentStream = agent.stream(request.newMessage.toUserMessage(), runnableConfig);

			// Convert Flux<NodeOutput> to Flux<ServerSentEvent<String>>
			return agentStream.map(nodeOutput -> {
				String node = nodeOutput.node();
				String agentName = nodeOutput.agent();
				Usage tokenUsage = nodeOutput.tokenUsage();


				// For streaming, we can use the message content as chunk
				StringBuilder chunkBuilder = new StringBuilder();
				AgentRunResponse agentResponse = null;
				if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
					Message message = streamingOutput.message();
					if (message == null) { // no update, typically output responses from nodes that does not produce messages
						return ServerSentEvent.<String>builder()
								.data("{}")
								.build();
					}
					if (message instanceof AssistantMessage assistantMessage) {
						if (assistantMessage.hasToolCalls()) {
							agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, "");
						} else {
							chunkBuilder.append(assistantMessage.getText());
							agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, assistantMessage.getText());
						}
					} else {
						agentResponse = new AgentRunResponse(node, agentName, message, tokenUsage, "");
					}
				} else if (nodeOutput instanceof InterruptionMetadata interruptionMetadata) {
					// Use the specialized method to convert InterruptionMetadata to ToolRequestMessageDTO
					ToolRequestConfirmMessageDTO toolRequestMessage = MessageDTO.MessageDTOFactory.fromInterruptionMetadata(interruptionMetadata);
					agentResponse = new AgentRunResponse(node, agentName, toolRequestMessage, tokenUsage, "");
				} else {
					// Handle other NodeOutput types if necessary
//					agentResponse = new AgentRunResponse(node, agentName, null, tokenUsage, "");
				}


				// Serialize to JSON string
				try {
					if (agentResponse != null) {
						String jsonData = mapper.writeValueAsString(agentResponse);
						return ServerSentEvent.<String>builder()
								.data(jsonData)
								.build();
					}
				} catch (Exception e) {
					log.error("Failed to serialize AgentRunResponse to JSON", e);
					return ServerSentEvent.<String>builder()
							.data("{\"error\":\"Failed to serialize response\"}")
							.build();
				}
				return ServerSentEvent.<String>builder()
						.data("{}")
						.build();
			});
		}
		catch (Exception e) {
			log.error("Error during agent run for session {}", request.threadId, e);
			return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
		}
	}
}
