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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.tools.task.AgentSpec;
import com.alibaba.cloud.ai.graph.agent.tools.task.AgentSpecReactAgentFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentToolObservationTest {

	@Test
	void shouldCreateSpringAiObservationForToolExecution() throws Exception {
		assertToolObservation(false);
	}

	@Test
	void shouldUseObservationRegistryFromCompileConfig() throws Exception {
		assertToolObservation(true);
	}

	@Test
	void shouldMarkHandledToolFailureOnObservation() throws Exception {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		List<ToolCallingObservationContext> stoppedToolObservations = new CopyOnWriteArrayList<>();
		AtomicInteger observedErrors = new AtomicInteger();
		observationRegistry.observationConfig()
			.observationHandler(new ToolObservationCollector(stoppedToolObservations, observedErrors));
		ToolCallback failingTool = FunctionToolCallback.builder("failing_tool", (ObservedRequest request) -> {
			throw new IllegalStateException("tool failed");
		})
			.description("Always fails")
			.inputType(ObservedRequest.class)
			.build();
		ReactAgent agent = ReactAgent.builder()
			.name("failing_tool_agent")
			.model(new ToolCallingChatModel("failing_tool"))
			.tools(failingTool)
			.observationRegistry(observationRegistry)
			.build();

		agent.call("invoke the failing tool");

		assertEquals(1, stoppedToolObservations.size());
		assertEquals(1, observedErrors.get());
	}

	@Test
	void shouldForwardObservationRegistryFromAgentSpecFactory() throws Exception {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		List<ToolCallingObservationContext> stoppedToolObservations = new CopyOnWriteArrayList<>();
		observationRegistry.observationConfig()
			.observationHandler(new ToolObservationCollector(stoppedToolObservations));
		ToolCallback observedTool = observedTool();
		ChatClient chatClient = ChatClient.builder(new ToolCallingChatModel(), observationRegistry, null, null).build();
		AgentSpecReactAgentFactory factory = AgentSpecReactAgentFactory.builder()
			.chatClient(chatClient)
			.defaultTools(observedTool)
			.observationRegistry(observationRegistry)
			.build();

		ReactAgent agent = factory.create(AgentSpec.of("spec_agent", "test agent", "use the tool"));
		agent.call("invoke the observed tool");

		assertEquals(1, stoppedToolObservations.size());
	}

	private static void assertToolObservation(boolean useCompileConfigRegistry) throws Exception {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		List<ToolCallingObservationContext> stoppedToolObservations = new CopyOnWriteArrayList<>();
		observationRegistry.observationConfig()
			.observationHandler(new ToolObservationCollector(stoppedToolObservations));
		ToolCallback observedTool = observedTool();
		Builder agentBuilder = ReactAgent.builder()
			.name("observed_agent")
			.model(new ToolCallingChatModel())
			.tools(observedTool);
		if (useCompileConfigRegistry) {
			agentBuilder.compileConfig(CompileConfig.builder().observationRegistry(observationRegistry).build());
		}
		else {
			agentBuilder.observationRegistry(observationRegistry);
		}
		ReactAgent agent = agentBuilder.build();

		agent.call("invoke the observed tool");

		assertEquals(1, stoppedToolObservations.size());
		ToolCallingObservationContext context = stoppedToolObservations.get(0);
		assertEquals("spring.ai.tool", context.getName());
		assertEquals("tool_call", context.getLowCardinalityKeyValue("spring.ai.kind").getValue());
		assertEquals("observed_tool", context.getToolDefinition().name());
		assertEquals("{\"value\":\"hello\"}", context.getToolCallArguments());
		assertEquals("\"observed:hello\"", context.getToolCallResult());
	}

	private static ToolCallback observedTool() {
		return FunctionToolCallback.builder("observed_tool",
				(ObservedRequest request) -> "observed:" + request.value)
			.description("Returns an observed value")
			.inputType(ObservedRequest.class)
			.build();
	}

	private static class ToolCallingChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		private final String toolName;

		private ToolCallingChatModel() {
			this("observed_tool");
		}

		private ToolCallingChatModel(String toolName) {
			this.toolName = toolName;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			AssistantMessage response = callCount.getAndIncrement() == 0
					? AssistantMessage.builder()
						.content("")
						.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", this.toolName,
								"{\"value\":\"hello\"}")))
						.build()
					: new AssistantMessage("done");
			return new ChatResponse(List.of(new Generation(response)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private record ToolObservationCollector(List<ToolCallingObservationContext> observations, AtomicInteger errors)
			implements ObservationHandler<ToolCallingObservationContext> {

		private ToolObservationCollector(List<ToolCallingObservationContext> observations) {
			this(observations, new AtomicInteger());
		}

		@Override
		public void onError(ToolCallingObservationContext context) {
			this.errors.incrementAndGet();
		}

		@Override
		public void onStop(ToolCallingObservationContext context) {
			this.observations.add(context);
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof ToolCallingObservationContext;
		}

	}

	private static class ObservedRequest {

		public String value;
	}

}
