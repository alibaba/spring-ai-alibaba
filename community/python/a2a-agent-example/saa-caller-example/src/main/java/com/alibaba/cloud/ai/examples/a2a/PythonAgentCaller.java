/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.a2a;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component for calling Python A2A agents via Nacos discovery.
 *
 * <p>This component demonstrates how to:
 * <ul>
 *   <li>Use AgentCardProvider to discover agents by name from Nacos</li>
 *   <li>Build A2aRemoteAgent with the discovered AgentCard</li>
 *   <li>Invoke the remote agent and process the response</li>
 * </ul>
 */
@Component
public class PythonAgentCaller {

	private static final Logger logger = LoggerFactory.getLogger(PythonAgentCaller.class);

	private final AgentCardProvider agentCardProvider;

	private final String pythonAgentName;

	public PythonAgentCaller(AgentCardProvider agentCardProvider,
			@Value("${python.agent.name:python-translator-agent}") String pythonAgentName) {
		this.agentCardProvider = agentCardProvider;
		this.pythonAgentName = pythonAgentName;
	}

	/**
	 * Call the Python translator agent.
	 * @param text Text to translate
	 * @return Translation result
	 */
	public String callTranslator(String text) {
		logger.info("Calling Python agent '{}' with text: {}", pythonAgentName, text);

		// 1. Discover agent from Nacos by name
		AgentCardWrapper agentCard = agentCardProvider.getAgentCard(pythonAgentName);
		logger.info("Discovered agent: name={}, url={}", agentCard.name(), agentCard.url());

		// 2. Build A2aRemoteAgent
		A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
			.name("python-translator-caller") // Local node name
			.description("Caller for Python translator agent")
			.agentCard(agentCard.getAgentCard()) // Use discovered AgentCard
			.instruction("{input}") // Pass input directly
			.streaming(false) // Use non-streaming for simplicity
			.build();

		// 3. Invoke the remote agent
		try {
			Optional<OverAllState> result = remoteAgent.invoke(text);

			if (result.isPresent()) {
				OverAllState state = result.get();
				String response = extractResponse(state);
				logger.info("Received response from Python agent: {}", response);
				return response;
			}
			else {
				logger.warn("No response from Python agent");
				return "No response received";
			}
		}
		catch (Exception e) {
			logger.error("Failed to call Python agent", e);
			throw new RuntimeException("Failed to call Python agent: " + e.getMessage(), e);
		}
	}

	/**
	 * Call the Python translator agent with streaming.
	 * @param text Text to translate
	 * @return Translation result
	 */
	public String callTranslatorStreaming(String text) {
		logger.info("Calling Python agent '{}' with streaming, text: {}", pythonAgentName, text);

		// 1. Discover agent from Nacos
		AgentCardWrapper agentCard = agentCardProvider.getAgentCard(pythonAgentName);

		// 2. Build A2aRemoteAgent with streaming enabled
		A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
			.name("python-translator-caller-streaming")
			.description("Streaming caller for Python translator agent")
			.agentCard(agentCard.getAgentCard())
			.instruction("{input}")
			.streaming(true) // Enable streaming
			.build();

		// 3. Invoke with streaming
		try {
			StringBuilder responseBuilder = new StringBuilder();
			AtomicReference<OverAllState> lastState = new AtomicReference<>();

			remoteAgent.stream(text).doOnNext(output -> {
				lastState.set(output.state());
				if (output instanceof StreamingOutput) {
					String chunk = ((StreamingOutput<?>) output).chunk();
					if (chunk != null && !chunk.isEmpty()) {
						responseBuilder.append(chunk);
					}
				}
			}).blockLast();

			String response = responseBuilder.toString();
			if (response.isEmpty() && lastState.get() != null) {
				response = extractResponse(lastState.get());
			}

			logger.info("Received streaming response from Python agent: {}", response);
			return response.isEmpty() ? "No response received" : response;
		}
		catch (Exception e) {
			logger.error("Failed to call Python agent with streaming", e);
			throw new RuntimeException("Failed to call Python agent: " + e.getMessage(), e);
		}
	}

	private String extractResponse(OverAllState state) {
		// Prefer agent output key (A2aRemoteAgent defaults to "output")
		Optional<Object> output = state.value("output");
		if (output.isPresent()) {
			return String.valueOf(output.get());
		}

		// Fallback: raw messages in state
		Optional<Object> messages = state.value("messages");
		if (messages.isPresent()) {
			return String.valueOf(messages.get());
		}

		return state.toString();
	}

}
