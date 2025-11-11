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

package com.alibaba.cloud.ai.agent.agui;

import com.alibaba.cloud.ai.agent.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/** Controller handling agent execution endpoints. */
@RestController
public class CopilotkitController {

	private static final Logger log = LoggerFactory.getLogger(CopilotkitController.class);
	final ObjectMapper mapper = new ObjectMapper();
	final AGUIAgent uiAgent = new AGUIAgent();
	private final AgentLoader agentLoader;

	@Autowired
	public CopilotkitController(AgentLoader agentLoader) {
		this.agentLoader = agentLoader;
	}

	@PostMapping(path = "/run_sse_copilotkit",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public Flux<ServerSentEvent<String>> copilotKit(@RequestBody String runAgentInputPayload) throws Exception {

		var input = mapper.readValue(runAgentInputPayload, AGUIType.RunAgentInput.class);

		BaseAgent agent = agentLoader.loadAgent(StringUtils.hasLength(input.appName()) ? input.appName() : "research_agent");

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

}
