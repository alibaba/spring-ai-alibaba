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
import com.alibaba.cloud.ai.agent.agui.AGUIType;
import com.alibaba.cloud.ai.agent.dto.AgentRunRequest;
import com.alibaba.cloud.ai.agent.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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
	@PostMapping("/run")
	public NodeOutput agentRun(@RequestBody AgentRunRequest request) {
		if (request.appName == null || request.appName.trim().isEmpty()) {
			log.warn("appName cannot be null or empty in POST /run request.");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty");
		}
		if (request.sessionId == null || request.sessionId.trim().isEmpty()) {
			log.warn("sessionId cannot be null or empty in POST /run request.");
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "sessionId cannot be null or empty");
		}
		log.info("Request received for POST /run for session: {}", request.sessionId);

		try {
			BaseAgent agent = agentLoader.loadAgent(request.appName);
			RunnableConfig runnableConfig = RunnableConfig.builder().threadId(request.sessionId)
					.addMetadata("user_id", request.userId).build();
			// request.stateDelta 目前用不到
			Optional<NodeOutput> state = agent.invokeAndGetOutput(request.newMessage, runnableConfig);

			return state.orElseThrow(() -> {
				log.error("Agent run for session {} did not produce any output.", request.sessionId);
				return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run produced no output");
			});
		}
		catch (Exception e) {
			log.error("Error during agent run for session {}", request.sessionId, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e);
		}
	}

	@PostMapping(path = "/run_sse_copilotkit",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public Flux<ServerSentEvent<String>> copilotKit(@RequestBody String runAgentInputPayload) throws Exception {

		var input = mapper.readValue(runAgentInputPayload, AGUIType.RunAgentInput.class);

		BaseAgent agent = agentLoader.loadAgent(StringUtils.hasLength(input.appName()) ? input.appName() : "single_agent");

		return uiAgent.run(agent, input)
				.map(event -> {
					try {
						String eventType = event.type().name();
						String eventData = mapper.writeValueAsString(event);

						return ServerSentEvent.<String>builder()
								.event(eventType)
								.data(eventData)
								.build();
					}
					catch (Exception e) {
						log.error("Error serializing AGUIEvent", e);
						return ServerSentEvent.<String>builder()
								.event("error")
								.data("Error serializing event: " + e.getMessage())
								.build();
					}
				})
				.onErrorResume(e -> {
					log.error("Error in copilotKit stream", e);
					return Flux.just(ServerSentEvent.<String>builder()
							.event("error")
							.data("Error: " + e.getMessage())
							.build());
				});
	}

	/**
	 * Executes an agent run and streams the resulting events using Server-Sent Events (SSE).
	 *
	 * @param request The AgentRunRequest containing run details.
	 * @return A Flux that will stream events to the client.
	 */
	@PostMapping(value = "/run_sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<NodeOutput> agentRunSse(@RequestBody AgentRunRequest request) {
		if (request.appName == null || request.appName.trim().isEmpty()) {
			log.warn(
					"appName cannot be null or empty in SSE request for appName: {}, session: {}",
					request.appName,
					request.sessionId);
			return Flux.error(
					new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty"));
		}
		if (request.sessionId == null || request.sessionId.trim().isEmpty()) {
			log.warn(
					"sessionId cannot be null or empty in SSE request for appName: {}, session: {}",
					request.appName,
					request.sessionId);
			return Flux.error(
					new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId cannot be null or empty"));
		}

		try {
			BaseAgent agent = agentLoader.loadAgent(request.appName);
			RunnableConfig runnableConfig = RunnableConfig.builder()
					.threadId(request.sessionId)
					.addMetadata("user_id", request.userId)
					.build();
			// request.stateDelta 目前用不到
			Flux<NodeOutput> agentStream = agent.stream(request.newMessage, runnableConfig);

			// Create a heartbeat Flux to keep connection alive (emit every 15 seconds)
			Flux<NodeOutput> heartBeat = Flux.interval(java.time.Duration.ofSeconds(5))
					.map(tick -> NodeOutput.of("heartbeat", OverAllStateBuilder.builder().build()))
					.takeUntilOther(agentStream.last());

			// Merge the agent stream with heartbeat
			return Flux.merge(agentStream, heartBeat);
		}
		catch (Exception e) {
			log.error("Error during agent run for session {}", request.sessionId, e);
			return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
		}
	}
}
